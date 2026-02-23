package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals

class CsaParserTest {

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
