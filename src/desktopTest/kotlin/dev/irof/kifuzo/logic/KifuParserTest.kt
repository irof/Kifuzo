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
        // 初期局面: 4一に後手玉 (v玉)
        session.initialSnapshot.assertAt(4, 1, BoardPiece(Piece.OU, PieceColor.White))
        // 2手目: 5二玉(41) により、5二に後手玉が来る
        session.getSnapshotAt(2).assertAt(5, 2, BoardPiece(Piece.OU, PieceColor.White))

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
        assertEquals(Evaluation.Score(123), session.moves[0].evaluation)
        assertEquals(Evaluation.Score(-456), session.moves[1].evaluation)
        assertEquals(Evaluation.Score(2000), session.moves[2].evaluation)
        assertEquals(Evaluation.Score(-500), session.moves[3].evaluation)

        // 詰み情報
        val sessionMate = parse(KifuTestData.KIFU_WITH_MATE)
        assertEquals(Evaluation.SenteWin, sessionMate.moves[0].evaluation)
        assertEquals(Evaluation.SenteWin, sessionMate.moves[1].evaluation)
        assertEquals(Evaluation.GoteWin, sessionMate.moves[2].evaluation)
        assertEquals(Evaluation.GoteWin, sessionMate.moves[3].evaluation)

        // 投了などの終局行から評価値を設定できること
        val sessionResult = parse("1 ７六歩(77)\n2 ３四歩(33)\n3 投了")
        assertEquals(Evaluation.GoteWin, sessionResult.moves[2].evaluation)
    }

    @Test
    fun 標準的な平手棋譜をパースできること() {
        val session = parse(KifuTestData.BASIC_KIFU)
        assertEquals(2, session.maxStep)
        // 1手目: 7六歩(77)
        session.getSnapshotAt(1).assertAt(7, 6, BoardPiece(Piece.FU, PieceColor.Black))
        // 2手目: 3四歩(33)
        session.getSnapshotAt(2).assertAt(3, 4, BoardPiece(Piece.FU, PieceColor.White))

        // 消費時間の検証
        assertEquals(1, session.moves[0].consumptionSeconds)
        assertEquals(2, session.moves[1].consumptionSeconds)
    }

    @Test
    fun 駒の移動と持ち駒を正しく処理できること() {
        val session = parse(KifuTestData.KIFU_WITH_CAPTURE_AND_DROP)
        // 3手目: 2二角成(88) -> 2二に馬(先手)
        session.getSnapshotAt(3).assertAt(2, 2, BoardPiece(Piece.UM, PieceColor.Black))
        assertEquals(listOf(Piece.KA), session.getSnapshotAt(3).senteMochigoma)
        // 4手目: 同 銀(31) -> 2二に銀(後手)
        session.getSnapshotAt(4).assertAt(2, 2, BoardPiece(Piece.GI, PieceColor.White))
        assertEquals(listOf(Piece.KA), session.getSnapshotAt(4).goteMochigoma)
        // 5手目: 4五角打 -> 4五に角(先手)
        session.getSnapshotAt(5).assertAt(4, 5, BoardPiece(Piece.KA, PieceColor.Black))
    }

    @Test
    fun 特殊なセクションやコメントを適切に処理すること() {
        // 結果行やコメント行が含まれていても手数としてカウントすること
        val sessionMixed = parse(KifuTestData.KIFU_WITH_MIXED_COMMENTS)
        assertEquals(3, sessionMixed.maxStep)
        assertEquals("3 投了", sessionMixed.moves[2].moveText)

        // 変化セクションの内容を無視すること
        val sessionVariation = parse(KifuTestData.KIFU_WITH_VARIATION)
        assertEquals(2, sessionVariation.maxStep)
        assertEquals("2 ３四歩(33)", sessionVariation.moves[1].moveText)
    }

    @Test
    fun 成りや初期持駒を含む複雑な棋譜をパースできること() {
        // 成り: 3手目 2二角成(88)
        val sessionComplex = parse(KifuTestData.KIFU_WITH_COMPLEX_MOVES)
        sessionComplex.getSnapshotAt(3).assertAt(2, 2, BoardPiece(Piece.UM, PieceColor.Black))
        assertEquals(listOf(Piece.KA, Piece.KE), sessionComplex.getSnapshotAt(5).senteMochigoma)

        // 初期持駒
        val sessionInitialMochi = parse(KifuTestData.KIFU_WITH_INITIAL_MOCHI)
        assertEquals(2, sessionInitialMochi.initialSnapshot.senteMochigoma.count { it == Piece.HI })
        assertEquals(4, sessionInitialMochi.initialSnapshot.goteMochigoma.count { it == Piece.KI })
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
        val initial = session.initialSnapshot
        assertEquals(listOf(Piece.KA, Piece.KI, Piece.KI), initial.senteMochigoma.sortedBy { it.mochigomaOrder })
        assertEquals(listOf(Piece.GI), initial.goteMochigoma)
    }

    @Test
    fun 同や打を含む特殊な表記をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_SPECIAL_NOTATION)
        // 3手目: 3四同歩(76)
        session.getSnapshotAt(3).assertAt(3, 4, BoardPiece(Piece.FU, PieceColor.Black))
        // 4手目: 5五角打
        session.getSnapshotAt(4).assertAt(5, 5, BoardPiece(Piece.KA, PieceColor.White))
        // 6手目: 8八同銀(79)
        session.getSnapshotAt(6).assertAt(8, 8, BoardPiece(Piece.GI, PieceColor.White))
    }

    @Test
    fun 変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_VARIATION)
        // 本譜の確認
        assertEquals(2, session.maxStep)
        assertEquals("2 ３四歩(33)", session.moves[1].moveText)

        // 変化の確認（1手目 = ７六歩 の指し手からの分岐）
        val variations = session.moves[0].variations
        assertEquals(1, variations.size)
        val variation = variations[0]
        // 手順リストは変化後の指し手のみとなる
        assertEquals(2, variation.size)
        assertEquals("2 ８四歩(83)", variation[0].moveText)
        assertEquals("3 ２六歩(27)", variation[1].moveText)
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
        val move3 = session.moves[2]
        assertEquals(listOf(Piece.KA), move3.resultSnapshot.senteMochigoma)

        // 変化 4手目
        val variation = move3.variations[0]
        assertEquals(listOf(Piece.KA), variation[0].resultSnapshot.senteMochigoma)
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

        // 本譜 1手目終了局面からの分岐
        val var2 = session.moves[0].variations[0]
        assertEquals("2 ８四歩(83)", var2[0].moveText)

        // 変化2手目ラインの 2手目 (84歩) 終了局面（3手目開始前）からの分岐
        val var3AtVar2 = var2[0].variations
        assertEquals(1, var3AtVar2.size, "変化2手目ラインの3手目からの分岐があるはず")
        assertEquals("3 ７八金(69)", var3AtVar2[0][0].moveText)
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
        val variations = session.moves[0].variations
        assertEquals(2, variations.size)
        assertEquals("2 ８四歩(83)", variations[0][0].moveText)
        assertEquals("2 ４四歩(43)", variations[1][0].moveText)
    }

    @Test
    fun 複雑な変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_COMPLEX_VARIATION)

        // 本譜 4手目終了後の局面
        val var5 = session.moves[3].variations[0]
        assertEquals("5 ７七角(66)", var5[0].moveText)
        // 7筋7段目: cells[6][2]
        assertEquals(BoardPiece(Piece.KA, PieceColor.Black), var5[0].resultSnapshot.cells[6][2])

        // 6手目
        assertEquals("6 ４四歩(43)", var5[1].moveText)
        // 4筋4段目: cells[3][5]
        assertEquals(BoardPiece(Piece.FU, PieceColor.White), var5[1].resultSnapshot.cells[3][5])
    }

    @Test
    fun 同で始まる変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_VARIATION_WITH_DOU)
        val variation = session.moves[2].variations[0]
        assertEquals("4 同　銀(31)", variation[0].moveText)
        // 2筋2段目: cells[1][7]
        assertEquals(BoardPiece(Piece.GI, PieceColor.White), variation[0].resultSnapshot.cells[1][7])
    }

    @Test
    fun 様々な終局結果をパースできること() {
        val results = listOf("投了", "持将棋", "千日手", "切れ負け", "反則負け")
        results.forEach { result ->
            val session = parse("1 ７六歩(77)\n2 $result")
            assertEquals(2, session.maxStep)
            assertTrue(session.moves[1].moveText.contains(result))
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
