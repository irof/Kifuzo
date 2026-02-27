package dev.irof.kifuzo.logic.handler
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsHandlerTest {
    private lateinit var boardState: ShogiBoardState
    private lateinit var handler: SettingsHandler
    private var isFlipped: Boolean? = null

    @BeforeTest
    fun setup() {
        boardState = ShogiBoardState()
        handler = SettingsHandler(
            repository = StubKifuRepository(),
            boardState = boardState,
            onFilesChanged = {},
            onAutoFlip = { isFlipped = it },
        )
        isFlipped = null
    }

    @Test
    fun 後手名が正規表現にマッチした時に反転すること() {
        boardState.updateSession(KifuSession(senteName = "Sente", goteName = "MyName"))

        handler.updateAutoFlip("MyName")

        assertEquals(true, isFlipped)
    }

    @Test
    fun 先手名が正規表現にマッチした時に反転しないこと() {
        boardState.updateSession(KifuSession(senteName = "MyName", goteName = "Gote"))

        handler.updateAutoFlip("MyName")

        assertEquals(false, isFlipped)
    }

    @Test
    fun 不正な正規表現が渡された時にログ出力され処理が中断されること() {
        handler.updateAutoFlip("[") // 不正な正規表現
        assertEquals(null, isFlipped)
    }
}
