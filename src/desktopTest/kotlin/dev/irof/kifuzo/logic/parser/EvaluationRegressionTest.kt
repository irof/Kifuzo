package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.logic.parser.kif.KifParser
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluationRegressionTest {

    private fun parse(kifu: String): KifuSession {
        val state = ShogiBoardState()
        KifParser().parse(kifu.lines(), state)
        return state.session
    }

    @Test
    fun 詰みの後に数値評価値が来たら数値に戻ること() {
        val session = parse(KifuTestData.EVALUATION_REGRESSION_1)
        assertEquals(Evaluation.SenteWin, session.moves[0].evaluation, "1手目は先手勝ち")
        assertEquals(Evaluation.Score(500), session.moves[1].evaluation, "2手目は500点に戻るべき")
    }

    @Test
    fun 詰みが継続する場合は詰みのままであること() {
        val session = parse(KifuTestData.EVALUATION_REGRESSION_2)
        assertEquals(Evaluation.SenteWin, session.moves[0].evaluation)
        assertEquals(Evaluation.SenteWin, session.moves[1].evaluation)
    }
}
