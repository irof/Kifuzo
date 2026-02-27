package dev.irof.kifuzo.logic.service
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
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KifuParseServiceTest {
    private val service = KifuParseServiceImpl()
    private val state = ShogiBoardState()

    @Test
    fun CSA形式の内容を自動判別してパースできること() {
        val lines = """
            V2.2
            N+Sente
            N-Gote
            P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
            P2 * -HI *  *  *  *  * -KA *
            P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
            P4 *  *  *  *  *  *  *  *  *
            P5 *  *  *  *  *  *  *  *  *
            P6 *  *  *  *  *  *  *  *  *
            P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
            P8 * +KA *  *  *  *  * +HI *
            P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
            +
            +7776FU
            T1
        """.trimIndent().lines()

        service.parseManually(lines, state)
        assertEquals(1, state.session.maxStep)
        assertEquals("Sente", state.session.senteName)
    }

    @Test
    fun KIF形式の内容を自動判別してパースできること() {
        val lines = """
            先手：Sente
            後手：Gote
            指し手
            1 ７六歩(77) ( 0:01/00:00:01)
            2 ３四歩(33) ( 0:02/00:00:02)
        """.trimIndent().lines()

        service.parseManually(lines, state)
        assertEquals(2, state.session.maxStep)
        assertEquals("Sente", state.session.senteName)
    }

    @Test
    fun どちらでもない場合はエラーになること() {
        val lines = """
            これは棋譜ではありません。
            ただのテキストファイルです。
        """.trimIndent().lines()

        assertFailsWith<KifuParseException> {
            service.parseManually(lines, state)
        }
    }

    @Test
    fun 空のファイルはエラーになること() {
        assertFailsWith<KifuParseException> {
            service.parseManually(emptyList(), state)
        }
    }
}
