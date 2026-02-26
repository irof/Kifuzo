package dev.irof.kifuzo.logic

import dev.irof.kifuzo.assertAt
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KifuParserTest {

    @Test
    fun 盤面図やメタデータが含まれる棋譜を正しくパースできること() {
        // 盤面図
        val session = parse(KifuTestData.KIFU_WITH_BOARD)
        session.history[0].assertAt(4, 1, BoardPiece(Piece.OU, PieceColor.White))
        session.history[2].assertAt(5, 2, BoardPiece(Piece.OU, PieceColor.White))

        // メタデータ
        val info = scanKifuInfo(KifuTestData.KIFU_WITH_PLAYER_INFO.lines())
        assertEquals("先手太郎", info.senteName)
        assertEquals("後手花子", info.goteName)
        assertEquals("2026/02/24 10:00:00", info.startTime)
        assertEquals("第1期蔵王戦", info.event)
    }

    @Test
    fun 評価値コメントや終局結果から評価値を抽出できること() {
        val session = parse(KifuTestData.KIFU_WITH_EVALUATION)
        assertEquals(Evaluation.Score(123), session.history[1].evaluation)
        assertEquals(Evaluation.Score(-456), session.history[2].evaluation)
        assertEquals(Evaluation.Score(2000), session.history[3].evaluation)
        assertEquals(Evaluation.Score(-500), session.history[4].evaluation)

        // 詰み情報
        val sessionMate = parse(KifuTestData.KIFU_WITH_MATE)
        assertEquals(Evaluation.SenteWin, sessionMate.history[1].evaluation)
        assertEquals(Evaluation.GoteWin, sessionMate.history[3].evaluation)

        // 優先順位（評価値よりも詰み情報を優先）
        val sessionPriority = parse(KifuTestData.KIFU_WITH_PRIORITY_MATE)
        assertEquals(Evaluation.SenteWin, sessionPriority.history[1].evaluation)
        assertEquals(Evaluation.GoteWin, sessionPriority.history[2].evaluation)

        // 評価値がない場合はUnknownになること
        val sessionNoEval = parse("1 ７六歩(77)\n2 ３四歩(33)")
        assertEquals(Evaluation.Unknown, sessionNoEval.history[0].evaluation)
        assertEquals(Evaluation.Unknown, sessionNoEval.history[1].evaluation)

        // 投了などの終局行から評価値を設定できること
        val sessionResult = parse("1 ７六歩(77)\n2 ３四歩(33)\n3 投了")
        assertEquals(Evaluation.GoteWin, sessionResult.history[3].evaluation)
    }

    @Test
    fun 標準的な平手棋譜をパースできること() {
        val session = parse(KifuTestData.BASIC_KIFU)
        assertEquals(2, session.maxStep)
        session.history[1].assertAt(7, 6, BoardPiece(Piece.FU, PieceColor.Black))
        session.history[2].assertAt(3, 4, BoardPiece(Piece.FU, PieceColor.White))

        // 消費時間の検証
        assertEquals(1, session.history[1].consumptionSeconds)
        assertEquals(2, session.history[2].consumptionSeconds)
    }

    @Test
    fun 駒の移動と持ち駒を正しく処理できること() {
        val session = parse(KifuTestData.KIFU_WITH_CAPTURE_AND_DROP)
        // 駒取り
        session.history[3].assertAt(2, 2, BoardPiece(Piece.UM, PieceColor.Black))
        assertEquals(listOf(Piece.KA), session.history[3].senteMochigoma)
        // 相手による駒取り
        session.history[4].assertAt(2, 2, BoardPiece(Piece.GI, PieceColor.White))
        assertEquals(listOf(Piece.KA), session.history[4].goteMochigoma)
        // 駒打ち
        session.history[5].assertAt(4, 5, BoardPiece(Piece.KA, PieceColor.Black))
    }

    @Test
    fun 特殊なセクションやコメントを適切に処理すること() {
        // 結果行やコメント行が含まれていても手数としてカウントすること
        val sessionMixed = parse(KifuTestData.KIFU_WITH_MIXED_COMMENTS)
        assertEquals(3, sessionMixed.maxStep)
        assertEquals("3 投了", sessionMixed.history[3].lastMoveText)

        // 変化セクションの内容を無視すること
        val sessionVariation = parse(KifuTestData.KIFU_WITH_VARIATION)
        assertEquals(2, sessionVariation.maxStep)
        assertEquals("2 ３四歩(33)", sessionVariation.history[2].lastMoveText)
    }

    @Test
    fun 成りや初期持駒を含む複雑な棋譜をパースできること() {
        // 成り
        val sessionComplex = parse(KifuTestData.KIFU_WITH_COMPLEX_MOVES)
        assertEquals(Piece.UM, sessionComplex.history[3].at(2, 2)?.piece)
        assertEquals(listOf(Piece.KA, Piece.KE), sessionComplex.history[5].senteMochigoma)

        // 初期持駒
        val sessionInitialMochi = parse(KifuTestData.KIFU_WITH_INITIAL_MOCHI)
        assertEquals(2, sessionInitialMochi.history[0].senteMochigoma.count { it == Piece.HI })
        assertEquals(4, sessionInitialMochi.history[0].goteMochigoma.count { it == Piece.KI })
    }

    @Test
    fun 途中局面から開始の場合に持ち駒が反映されること() {
        val kifu = """
            先手の持駒：角　金二
            後手の持駒：銀
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|一
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|二
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|三
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|四
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|五
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|六
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|七
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|八
            | ・ ・ ・ ・ 王 ・ ・ ・ ・|九
            1 ５八玉(59)
        """.trimIndent()
        val session = parse(kifu)
        val initial = session.history[0]
        assertEquals(listOf(Piece.KA, Piece.KI, Piece.KI), initial.senteMochigoma.sortedBy { it.mochigomaOrder })
        assertEquals(listOf(Piece.GI), initial.goteMochigoma)
    }

    @Test
    fun 途中局面から開始の場合に持ち駒が反映されること_のなし() {
        val kifu = """
            先手持駒：角　金二
            後手持駒：銀
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|一
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|二
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|三
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|四
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|五
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|六
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|七
            | ・ ・ ・ ・ ・ ・ ・ ・ ・|八
            | ・ ・ ・ ・ 王 ・ ・ ・ ・|九
            1 ５八玉(59)
        """.trimIndent()
        val session = parse(kifu)
        val initial = session.history[0]
        assertEquals(listOf(Piece.KA, Piece.KI, Piece.KI), initial.senteMochigoma.sortedBy { it.mochigomaOrder })
        assertEquals(listOf(Piece.GI), initial.goteMochigoma)
    }

    @Test
    fun 同や打を含む特殊な表記をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_SPECIAL_NOTATION)
        assertEquals(Piece.FU, session.history[3].at(3, 4)?.piece)
        assertEquals(Piece.KA, session.history[4].at(5, 5)?.piece)
        assertEquals(Piece.GI, session.history[6].at(8, 8)?.piece)
    }

    @Test
    fun 変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_VARIATION)
        // 本譜の確認
        assertEquals(2, session.maxStep)
        assertEquals("2 ３四歩(33)", session.history[2].lastMoveText)

        // 変化の確認（1手目 = ７六歩 の局面からの分岐）
        val variations = session.history[1].variations
        assertEquals(1, variations.size)
        val variation = variations[0]
        // 手順リストは [分岐元局面, 変化1手目, 変化2手目, ...] となる
        assertEquals(3, variation.size)
        assertEquals("2 ８四歩(83)", variation[1].lastMoveText)
        assertEquals("3 ２六歩(27)", variation[2].lastMoveText)
    }

    @Test
    fun 変化手順の開始局面でも持ち駒が引き継がれること() {
        val kifu = """
            1 ７六歩(77)
            2 ３四歩(33)
            3 ２二角成(88)
            4 同　銀(31)
            変化：4手
            4 同　玉(41)
        """.trimIndent()
        val session = parse(kifu)

        // 本譜 3手目終了時 (22角成) の持ち駒: 角
        val snapshot3 = session.history[3]
        assertEquals(listOf(Piece.KA), snapshot3.senteMochigoma)

        // 変化 4手目開始時（3手目終了時と同じ局面）
        val variation = snapshot3.variations[0]
        assertEquals(listOf(Piece.KA), variation[0].senteMochigoma, "変化の開始局面(0手目相当)に持ち駒があるべき")
    }

    @Test
    fun ネストされた変化手順を正しくパースできること() {
        val kifu = """
            1 ７六歩(77)
            2 ３四歩(33)
            3 ２六歩(27)
            4 ８四歩(83)
            変化：2手
            2 ８四歩(83)
            3 ２六歩(27)
            4 ８五歩(84)
            変化：3手
            3 ７八金(69)
            4 ３二金(41)
        """.trimIndent()
        val session = parse(kifu)

        // 本譜 2手目 (34歩) からの分岐: 84歩
        val var2 = session.history[1].variations[0]
        assertEquals("2 ８四歩(83)", var2[1].lastMoveText)

        // 変化2手目ラインの 3手目 (26歩) からの分岐: 78金
        val var3AtHonpu = session.history[2].variations
        assertTrue(var3AtHonpu.isEmpty(), "本譜3手目からの分岐はないはず")

        val var3AtVar2 = var2[1].variations
        assertEquals(1, var3AtVar2.size, "変化2手目ラインの3手目からの分岐があるはず")
        assertEquals("3 ７八金(69)", var3AtVar2[0][1].lastMoveText)
    }

    @Test
    fun 同一箇所からの複数の変化手順を正しくパースできること() {
        val kifu = """
            1 ７六歩(77)
            2 ３四歩(33)
            変化：2手
            2 ８四歩(83)
            変化：2手
            2 ４四歩(43)
        """.trimIndent()
        val session = parse(kifu)
        val variations = session.history[1].variations
        assertEquals(2, variations.size)
        assertEquals("2 ８四歩(83)", variations[0][1].lastMoveText)
        assertEquals("2 ４四歩(43)", variations[1][1].lastMoveText)
    }

    @Test
    fun 複雑な変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_COMPLEX_VARIATION)

        // 変化：5手目（4手目の局面から分岐）の検証
        // 本譜 4手目は ４二銀(31) なので、その局面での 66 の駒(角)を動かす
        val var5 = session.history[4].variations[0]
        assertEquals("5 ７七角(66)", var5[1].lastMoveText)
        assertEquals(BoardPiece(Piece.KA, PieceColor.Black), var5[1].cells[6][2]) // 7筋7段目

        // 6手目の移動
        assertEquals("6 ４四歩(43)", var5[2].lastMoveText)
        assertEquals(BoardPiece(Piece.FU, PieceColor.White), var5[2].cells[3][5]) // 4筋4段目
    }

    @Test
    fun 同で始まる変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_VARIATION_WITH_DOU)
        val variation = session.history[3].variations[0]
        assertEquals("4 同　銀(31)", variation[1].lastMoveText)
        assertEquals(BoardPiece(Piece.GI, PieceColor.White), variation[1].cells[1][7]) // 2筋2段目
    }

    @Test
    fun 様々な終局結果をパースできること() {
        val results = listOf("投了", "持将棋", "千日手", "切れ負け", "反則負け")
        results.forEach { result ->
            val session = parse("1 ７六歩(77)\n2 $result")
            assertEquals(2, session.maxStep)
            assertTrue(session.history[2].lastMoveText.contains(result))
        }
    }
}

private fun parse(kifu: String): KifuSession {
    val state = ShogiBoardState()
    parseKifu(kifu.trimIndent().lines(), state)
    return state.session
}

private fun BoardSnapshot.at(file: Int, rank: Int): BoardPiece? {
    val s = Square(file, rank)
    return cells[s.yIndex][s.xIndex]
}
