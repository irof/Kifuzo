package dev.irof.kifuzo.logic

import dev.irof.kifuzo.assertAt
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
        session.history[0].assertAt(4, 1, Piece.OU to PieceColor.White)
        session.history[2].assertAt(5, 2, Piece.OU to PieceColor.White)

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
        session.history[1].assertAt(7, 6, Piece.FU to PieceColor.Black)
        session.history[2].assertAt(3, 4, Piece.FU to PieceColor.White)

        // 消費時間の検証
        assertEquals(1, session.history[1].consumptionSeconds)
        assertEquals(2, session.history[2].consumptionSeconds)
    }

    @Test
    fun 駒の移動と持ち駒を正しく処理できること() {
        val session = parse(KifuTestData.KIFU_WITH_CAPTURE_AND_DROP)
        // 駒取り
        session.history[3].assertAt(2, 2, Piece.UM to PieceColor.Black)
        assertEquals(listOf(Piece.KA), session.history[3].senteMochigoma)
        // 相手による駒取り
        session.history[4].assertAt(2, 2, Piece.GI to PieceColor.White)
        assertEquals(listOf(Piece.KA), session.history[4].goteMochigoma)
        // 駒打ち
        session.history[5].assertAt(4, 5, Piece.KA to PieceColor.Black)
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
        assertEquals(Piece.UM, sessionComplex.history[3].at(2, 2)?.first)
        assertEquals(listOf(Piece.KA, Piece.KE), sessionComplex.history[5].senteMochigoma)

        // 初期持駒
        val sessionInitialMochi = parse(KifuTestData.KIFU_WITH_INITIAL_MOCHI)
        assertEquals(2, sessionInitialMochi.history[0].senteMochigoma.count { it == Piece.HI })
        assertEquals(4, sessionInitialMochi.history[0].goteMochigoma.count { it == Piece.KI })
    }

    @Test
    fun 同や打を含む特殊な表記をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_SPECIAL_NOTATION)
        assertEquals(Piece.FU, session.history[3].at(3, 4)?.first)
        assertEquals(Piece.KA, session.history[4].at(5, 5)?.first)
        assertEquals(Piece.GI, session.history[6].at(8, 8)?.first)
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

private fun BoardSnapshot.at(file: Int, rank: Int): Pair<Piece, PieceColor>? {
    val s = Square(file, rank)
    return cells[s.yIndex][s.xIndex]
}
