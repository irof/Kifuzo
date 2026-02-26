package dev.irof.kifuzo.logic

import dev.irof.kifuzo.assertAt
import dev.irof.kifuzo.assertMaxStep
import dev.irof.kifuzo.assertMochigomaCount
import dev.irof.kifuzo.assertMove
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KifuParserTest {

    @Test
    fun 盤面図やメタデータが含まれる棋譜を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_BOARD)
        session.initialSnapshot.assertAt(4, 1, Piece.OU, PieceColor.White)
        session.getSnapshotAt(2).assertAt(5, 2, Piece.OU, PieceColor.White)

        val info = scanKifuInfo(KifuTestData.KIFU_WITH_PLAYER_INFO.lines())
        assertEquals("先手太郎", info.senteName)
        assertEquals("後手花子", info.goteName)
        assertEquals("2026/02/24 10:00:00", info.startTime)
        assertEquals("第1期蔵王戦", info.event)
    }

    @Test
    fun 評価値コメントや終局結果から評価値を抽出できること() {
        val session = parse(KifuTestData.KIFU_WITH_EVALUATION)
        session.moves[0].assertMove("1 ７六歩(77)", Evaluation.Score(123))
        session.moves[1].assertMove("2 ３四歩(33)", Evaluation.Score(-456))
        assertTrue(session.moves[2].moveText.contains("２二角成"), "3手目に角成が含まれること")
        assertEquals(Evaluation.Score(2000), session.moves[2].evaluation)

        val sessionMate = parse(KifuTestData.KIFU_WITH_MATE)
        assertEquals(Evaluation.SenteWin, sessionMate.moves[0].evaluation)
        assertEquals(Evaluation.SenteWin, sessionMate.moves[1].evaluation)
        assertEquals(Evaluation.GoteWin, sessionMate.moves[2].evaluation)
        assertEquals(Evaluation.GoteWin, sessionMate.moves[3].evaluation)

        val sessionResult = parse("1 ７六歩(77)\n2 ３四歩(33)\n3 投了")
        assertEquals(Evaluation.GoteWin, sessionResult.moves[2].evaluation)
    }

    @Test
    fun 標準的な平手棋譜をパースできること() {
        val session = parse(KifuTestData.BASIC_KIFU)
        session.assertMaxStep(2)
        session.getSnapshotAt(1).assertAt(7, 6, Piece.FU, PieceColor.Black)
        session.getSnapshotAt(2).assertAt(3, 4, Piece.FU, PieceColor.White)
    }

    @Test
    fun 駒の移動と持ち駒を正しく処理できること() {
        val session = parse(KifuTestData.KIFU_WITH_CAPTURE_AND_DROP)
        session.getSnapshotAt(3).assertAt(2, 2, Piece.UM, PieceColor.Black)
        session.getSnapshotAt(3).assertMochigomaCount(PieceColor.Black, Piece.KA, 1)
        session.getSnapshotAt(4).assertAt(2, 2, Piece.GI, PieceColor.White)
        session.getSnapshotAt(4).assertMochigomaCount(PieceColor.White, Piece.KA, 1)
        session.getSnapshotAt(5).assertAt(4, 5, Piece.KA, PieceColor.Black)
    }

    @Test
    fun 特殊なセクションやコメントを適切に処理すること() {
        val sessionMixed = parse(KifuTestData.KIFU_WITH_MIXED_COMMENTS)
        sessionMixed.assertMaxStep(3)
        assertTrue(sessionMixed.moves[2].moveText.contains("投了"))

        val sessionVariation = parse(KifuTestData.KIFU_WITH_VARIATION)
        sessionVariation.assertMaxStep(2)
        assertTrue(sessionVariation.moves[1].moveText.contains("３四歩"))
    }

    @Test
    fun 成りや初期持駒を含む複雑な棋譜をパースできること() {
        val sessionComplex = parse(KifuTestData.KIFU_WITH_COMPLEX_MOVES)
        sessionComplex.getSnapshotAt(3).assertAt(2, 2, Piece.UM, PieceColor.Black)
        sessionComplex.getSnapshotAt(5).assertMochigomaCount(PieceColor.Black, Piece.KA, 1)
        sessionComplex.getSnapshotAt(5).assertMochigomaCount(PieceColor.Black, Piece.KE, 1)

        val sessionInitialMochi = parse(KifuTestData.KIFU_WITH_INITIAL_MOCHI)
        sessionInitialMochi.initialSnapshot.assertMochigomaCount(PieceColor.Black, Piece.HI, 2)
        sessionInitialMochi.initialSnapshot.assertMochigomaCount(PieceColor.White, Piece.KI, 4)
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
        session.initialSnapshot.assertMochigomaCount(PieceColor.Black, Piece.KA, 1)
        session.initialSnapshot.assertMochigomaCount(PieceColor.Black, Piece.KI, 2)
        session.initialSnapshot.assertMochigomaCount(PieceColor.White, Piece.GI, 1)
    }

    @Test
    fun 同や打を含む特殊な表記をパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_SPECIAL_NOTATION)
        session.getSnapshotAt(3).assertAt(3, 4, Piece.FU, PieceColor.Black)
        session.getSnapshotAt(4).assertAt(5, 5, Piece.KA, PieceColor.White)
        session.getSnapshotAt(6).assertAt(8, 8, Piece.GI, PieceColor.White)
    }

    @Test
    fun 変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_WITH_VARIATION)
        session.assertMaxStep(2)

        val variations = session.moves[0].variations
        assertEquals(1, variations.size, "1手目からの変化が1つあること")
        assertTrue(variations[0][0].moveText.contains("８四歩"))
        assertTrue(variations[0][1].moveText.contains("２六歩"))
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
        session.moves[2].resultSnapshot.assertMochigomaCount(PieceColor.Black, Piece.KA, 1)

        val variations = session.moves[2].variations
        assertEquals(1, variations.size)
        variations[0][0].resultSnapshot.assertMochigomaCount(PieceColor.Black, Piece.KA, 1)
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

        val var2 = session.moves[0].variations
        assertEquals(1, var2.size)
        assertTrue(var2[0][0].moveText.contains("８四歩"))

        // 変化ラインの2手目(84歩)終了後 = moves[0] の variations
        val var3 = var2[0][0].variations
        assertEquals(1, var3.size)
        assertTrue(var3[0][0].moveText.contains("７八金"))
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
        assertTrue(variations[0][0].moveText.contains("８四歩"))
        assertTrue(variations[1][0].moveText.contains("４四歩"))
    }

    @Test
    fun 複雑な変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_COMPLEX_VARIATION)
        val var5 = session.moves[3].variations
        assertEquals(1, var5.size)
        assertTrue(var5[0][0].moveText.contains("７七角"))
        var5[0][0].resultSnapshot.assertAt(7, 7, Piece.KA, PieceColor.Black)
    }

    @Test
    fun 同で始まる変化手順を正しくパースできること() {
        val session = parse(KifuTestData.KIFU_VARIATION_WITH_DOU)
        val variations = session.moves[2].variations
        assertEquals(1, variations.size)
        assertTrue(variations[0][0].moveText.contains("同　銀"))
        variations[0][0].resultSnapshot.assertAt(2, 2, Piece.GI, PieceColor.White)
    }

    @Test
    fun 様々な終局結果をパースできること() {
        val results = listOf("投了", "持将棋", "千日手", "切れ負け", "反則負け")
        results.forEach { result ->
            val session = parse("1 ７六歩(77)\n2 $result")
            session.assertMaxStep(2)
            assertTrue(session.moves[1].moveText.contains(result))
        }
    }
}

private fun parse(kifu: String): KifuSession {
    val state = ShogiBoardState()
    parseKifu(kifu.trimIndent().lines(), state)
    return state.session
}
