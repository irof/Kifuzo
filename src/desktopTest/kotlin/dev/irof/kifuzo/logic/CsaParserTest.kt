package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals

class CsaParserTest {

    @Test
    fun CSAの評価値コメントを抽出できること() {
        val csa = """
                N+Sente
                N-Gote
                +7776FU
                '*#評価値=123
                -3334FU
                '*#評価値=-456
                +8822UM
                '*#詰み=先手勝ち
        """.trimIndent()

        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        // 1手目
        assertEquals(Evaluation.Score(123), state.session.moves[0].evaluation)
        assertEquals("1 ７六歩", state.session.moves[0].moveText)
        // 2手目
        assertEquals(Evaluation.Score(-456), state.session.moves[1].evaluation)
        assertEquals("2 ３四歩", state.session.moves[1].moveText)
        // 3手目
        assertEquals(Evaluation.SenteWin, state.session.moves[2].evaluation)
        assertEquals("3 ２二角成", state.session.moves[2].moveText)
    }

    @Test
    fun 同の表記を生成できること() {
        val csa = """
                +7776FU
                -3334FU
                +2222KA
        """.trimIndent()
        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        // 3手目が 22 なので、2手目の 34 とは異なるため通常の座標が表示される
        assertEquals("3 ２二角", state.session.moves[2].moveText)

        val csaSame = """
                +7776FU
                -3334FU
                +8822UM
                -3122GI
        """.trimIndent()
        val stateSame = ShogiBoardState()
        parseCsa(csaSame.lines(), stateSame)

        // 4手目(-3122GI)が3手目(+8822KA+)と同じ座標(22)に移動するので「同」になる
        assertEquals("4 同　銀", stateSame.session.moves[3].moveText)
    }

    @Test
    fun 駒が取られた時にfirstContactStepが設定されること() {
        val csa = """
                N+Sente
                N-Gote
                +7776FU
                T1
                -3334FU
                T1
                +8822UM
                T1
        """.trimIndent()

        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        // 3手目(+8822KA+)で角が取られるので、firstContactStepは3になるはず
        assertEquals(3, state.session.firstContactStep)
        // initialStep も衝突手数を優先する
        assertEquals(3, state.session.initialStep)
    }

    @Test
    fun CSA形式での駒の成りが正しく処理されること() {
        val csa = """
                ${'$'}EVENT:第1期蔵王戦
                ${'$'}START_TIME:2026/02/24 10:00:00
                N+Sente
                N-Gote
                P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
                P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
                P4 *  *  *  *  *  *  *  *  *
                P5 *  *  *  *  *  *  *  *  *
                P6 *  *  *  *  *  *  *  *  *
                P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
                P8 * +KA *  *  *  *  * +HI *
                P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
                +
                +7776TO
        """.trimIndent()

        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        // 1手目(+7776TO)で歩が「と」になるので、「成」が付くはず
        assertEquals("1 ７六歩成", state.session.moves[0].moveText)
        assertEquals(Piece.TO, state.session.moves[0].resultSnapshot.cells[5][2]?.piece)
        assertEquals("第1期蔵王戦", state.session.event)
        assertEquals("2026/02/24 10:00:00", state.session.startTime)
    }

    @Test
    fun 不完全な指し手行が含まれていてもエラーにならないこと() {
        val csa = """
                N+Sente
                N-Gote
                +7776FU
                +
                -
                -3334FU
        """.trimIndent()

        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        // 初期局面(initialSnapshot) 以外の指し手(moves)が2手
        assertEquals(2, state.session.moves.size)
    }

    @Test
    fun CSA形式の初期持駒が正しく処理されること() {
        val csa = """
            N+Sente
            N-Gote
            P+00HI00KA
            P-00KI00GI
            +7776FU
        """.trimIndent()
        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        val initialSnapshot = state.session.initialSnapshot
        assertEquals(listOf(Piece.HI, Piece.KA), initialSnapshot.senteMochigoma)
        assertEquals(listOf(Piece.KI, Piece.GI), initialSnapshot.goteMochigoma)
    }

    @Test
    fun 途中局面開始のCSAでは初期手数が0になること() {
        val csa = """
            P+00HI
            P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
            P2 *  *  *  *  *  *  * -KA *
            P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
            P4 *  *  *  *  *  *  *  *  *
            P5 *  *  *  *  *  *  *  *  *
            P6 *  *  *  *  *  *  *  *  *
            P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
            P8 * +KA *  *  *  *  * +HI *
            P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
            +7776FU
            -3334FU
            +8822UM
        """.trimIndent()
        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        // 3手目 (+8822UM) で 22の飛車 を取るので衝突が発生する
        assertEquals(3, state.session.firstContactStep)
        // 途中局面開始なので、初期手数は 0 であるべき
        assertEquals(0, state.session.initialStep, "途中局面開始（isStandardStart=false）の場合は開始局面(0)を優先すべき")
    }
}
