package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.Evaluation
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
                +8822KA+
                '*#詰み=先手勝ち
        """.trimIndent()

        val state = ShogiBoardState()
        parseCsa(csa.lines(), state)

        assertEquals(Evaluation.Score(123), state.session.history[1].evaluation)
        assertEquals(Evaluation.Score(-456), state.session.history[2].evaluation)
        assertEquals(Evaluation.SenteWin, state.session.history[3].evaluation)
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
                +8822KA+
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

        // 正しくパースされた指し手（2手）だけが反映されていること
        // 初期局面(1) + 指し手(2) = 3
        assertEquals(3, state.session.history.size)
    }
}
