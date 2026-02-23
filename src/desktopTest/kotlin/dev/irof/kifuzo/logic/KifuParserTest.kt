package dev.irof.kifuzo.logic

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

    private fun parse(kifu: String): KifuSession {
        val state = ShogiBoardState()
        parseKifu(kifu.trimIndent().lines(), state)
        return state.session
    }

    private fun BoardSnapshot.at(file: Int, rank: Int): Pair<Piece, PieceColor>? {
        val s = Square(file, rank)
        return cells[s.yIndex][s.xIndex]
    }

    @Test
    fun 盤面図が含まれる棋譜を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_BOARD)
        assertEquals(Piece.OU to PieceColor.White, session.history[0].at(4, 1))
        assertEquals(Piece.OU to PieceColor.White, session.history[2].at(5, 2))
    }

    @Test
    fun 評価値コメントを抽出できること() {
        val session = parse(KifuTestData.KIFU_WITH_EVALUATION)
        assertEquals(Evaluation.Score(123), session.history[1].evaluation)
        assertEquals(Evaluation.Score(-456), session.history[2].evaluation)
        assertEquals(Evaluation.Score(2000), session.history[3].evaluation)
        assertEquals(Evaluation.Score(-500), session.history[4].evaluation)
    }

    @Test
    fun 詰みコメントから評価値を設定できること() {
        val session = parse(KifuTestData.KIFU_WITH_MATE)
        assertEquals(Evaluation.SenteWin, session.history[1].evaluation)
        assertEquals(Evaluation.SenteWin, session.history[2].evaluation)
        assertEquals(Evaluation.GoteWin, session.history[3].evaluation)
        assertEquals(Evaluation.GoteWin, session.history[4].evaluation)
    }

    @Test
    fun 評価値よりも詰み情報を優先すること() {
        val session = parse(KifuTestData.KIFU_WITH_PRIORITY_MATE)
        assertEquals(Evaluation.SenteWin, session.history[1].evaluation)
        assertEquals(Evaluation.GoteWin, session.history[2].evaluation)
    }

    @Test
    fun 評価値コメントがない棋譜ではすべての局面の評価値がUnknownであること() {
        val session = parse("1 ７六歩(77)\n2 ３四歩(33)")
        assertEquals(Evaluation.Unknown, session.history[0].evaluation)
        assertEquals(Evaluation.Unknown, session.history[1].evaluation)
        assertEquals(Evaluation.Unknown, session.history[2].evaluation)
    }

    @Test
    fun 終局行のみ評価値が設定されるケースを検証すること() {
        val session = parse("1 ７六歩(77)\n2 ３四歩(33)\n3 投了")
        assertEquals(Evaluation.Unknown, session.history[0].evaluation)
        assertEquals(Evaluation.Unknown, session.history[1].evaluation)
        assertEquals(Evaluation.Unknown, session.history[2].evaluation)
        assertEquals(Evaluation.GoteWin, session.history[3].evaluation)
    }

    @Test
    fun 対局者名や開始日時情報をスキャンできること() {
        val info = scanKifuInfo(KifuTestData.KIFU_WITH_PLAYER_INFO.lines())
        assertEquals("先手太郎", info.senteName)
        assertEquals("後手花子", info.goteName)
    }

    @Test
    fun 標準的な平手棋譜をパースできること() {
        val session = parse(KifuTestData.BASIC_KIFU)
        assertEquals(2, session.maxStep)
        assertEquals(Piece.FU to PieceColor.Black, session.history[1].at(7, 6))
        assertEquals(Piece.FU to PieceColor.White, session.history[2].at(3, 4))

        // 消費時間の検証
        assertEquals(1, session.history[1].consumptionSeconds)
        assertEquals(2, session.history[2].consumptionSeconds)
    }

    @Test
    fun 駒取りと駒打ちを正しく処理できること() {
        val session = parse(KifuTestData.KIFU_WITH_CAPTURE_AND_DROP)
        assertEquals(Piece.UM to PieceColor.Black, session.history[3].at(2, 2))
        assertEquals(listOf(Piece.KA), session.history[3].senteMochigoma)
        assertEquals(Piece.GI to PieceColor.White, session.history[4].at(2, 2))
        assertEquals(listOf(Piece.KA), session.history[4].goteMochigoma)
        assertEquals(Piece.KA to PieceColor.Black, session.history[5].at(4, 5))
    }

    @Test
    fun 結果行やコメント行が含まれていても手数としてカウントすること() {
        val session = parse(KifuTestData.KIFU_WITH_MIXED_COMMENTS)
        assertEquals(3, session.maxStep)
        assertEquals("3 投了", session.history[3].lastMoveText)
    }

    @Test
    fun 変化セクションの内容を無視すること() {
        val session = parse(KifuTestData.KIFU_WITH_VARIATION)
        assertEquals(2, session.maxStep)
        assertEquals("2 ３四歩(33)", session.history[2].lastMoveText)
    }

    @Test
    fun 成りや駒取りを含む詳細な手順をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_COMPLEX_MOVES)
        assertEquals(Piece.UM, session.history[3].at(2, 2)?.first)
        assertEquals(Piece.GI, session.history[4].at(4, 2)?.first)
        assertEquals(listOf(Piece.KA, Piece.KE), session.history[5].senteMochigoma)
    }

    @Test
    fun 初期持駒がある盤面図をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_INITIAL_MOCHI)
        val step0 = session.history[0]
        assertEquals(2, step0.senteMochigoma.count { it == Piece.HI })
        assertEquals(4, step0.goteMochigoma.count { it == Piece.KI })
    }

    @Test
    fun 同や打を含む特殊な表記をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_SPECIAL_NOTATION)
        assertEquals(Piece.FU, session.history[3].at(3, 4)?.first)
        assertEquals(Piece.KA, session.history[4].at(5, 5)?.first)
        assertEquals(Piece.GI, session.history[6].at(8, 8)?.first)
    }

    @Test
    fun 様々な終局結果を手数としてカウントできること() {
        val results = listOf("投了", "持将棋", "千日手", "切れ負け", "反則負け")
        results.forEach { result ->
            val session = parse("1 ７六歩(77)\n2 $result")
            assertEquals(2, session.maxStep)
            assertTrue(session.history[2].lastMoveText.contains(result))
        }
    }
}
