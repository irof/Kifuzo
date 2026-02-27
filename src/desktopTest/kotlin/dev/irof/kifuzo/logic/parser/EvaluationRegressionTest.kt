package dev.irof.kifuzo.logic.parser
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.parseCsa
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.parser.parseKifu
import dev.irof.kifuzo.logic.parser.scanKifuInfo
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluationRegressionTest {

    private fun parse(kifu: String): dev.irof.kifuzo.models.KifuSession {
        val state = ShogiBoardState()
        parseKifu(kifu.trimIndent().lines(), state)
        return state.session
    }

    @Test
    fun 詰みの後に数値評価値が来たら数値に戻ること() {
        val kifu = """
            1 ７六歩(77)
            *#詰み=先手勝ち
            2 ３四歩(33)
            * 500
        """.trimIndent()

        val session = parse(kifu)
        assertEquals(Evaluation.SenteWin, session.moves[0].evaluation, "1手目は先手勝ち")
        assertEquals(Evaluation.Score(500), session.moves[1].evaluation, "2手目は500点に戻るべき")
    }

    @Test
    fun 詰みが継続する場合は詰みのままであること() {
        val kifu = """
            1 ７六歩(77)
            *#詰み=先手勝ち
            2 ３四歩(33)
            *#詰み=先手勝ち
        """.trimIndent()

        val session = parse(kifu)
        assertEquals(Evaluation.SenteWin, session.moves[0].evaluation)
        assertEquals(Evaluation.SenteWin, session.moves[1].evaluation)
    }
}
