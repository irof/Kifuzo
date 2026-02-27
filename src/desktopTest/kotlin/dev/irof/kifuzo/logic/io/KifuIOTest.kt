package dev.irof.kifuzo.logic.io
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
import java.nio.charset.Charset
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeBytes
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class KifuIOTest {

    private var tempFile = createTempFile()

    @BeforeTest
    fun setUp() {
        tempFile = createTempFile()
    }

    @AfterTest
    fun tearDown() {
        tempFile.deleteIfExists()
    }

    @Test
    fun UTF8エンコーディングのテキストを読み込めること() {
        val text = "あいうえお"
        tempFile.writeBytes(text.toByteArray(Charsets.UTF_8))

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }

    @Test
    fun ShiftJISエンコーディングのテキストを読み込めること() {
        val text = "あいうえお"
        val sjis = Charset.forName("Shift_JIS")
        tempFile.writeBytes(text.toByteArray(sjis))

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }

    @Test
    fun ASCIIエンコーディングのテキストを読み込めること() {
        val text = "abcde12345"
        tempFile.writeBytes(text.toByteArray(Charsets.US_ASCII))

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }

    @Test
    fun 不正なUTF8バイト列を含む場合にShiftJISとして読み込めること() {
        // Shift_JIS で "あ" (0x82 0xA0) は、UTF-8 としては不正
        val text = "あ"
        val sjis = Charset.forName("Shift_JIS")
        val bytes = text.toByteArray(sjis)
        tempFile.writeBytes(bytes)

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }

    @Test
    fun 空のファイルを読み込めること() {
        tempFile.writeBytes(ByteArray(0))
        val lines = readLinesWithEncoding(tempFile)
        assertEquals(0, lines.size)
    }

    @Test
    fun 末尾に改行がある場合に空行が除去されること() {
        val text = "line1\nline2\n"
        tempFile.writeBytes(text.toByteArray(Charsets.UTF_8))
        val lines = readLinesWithEncoding(tempFile)
        assertEquals(2, lines.size)
        assertEquals("line1", lines[0])
        assertEquals("line2", lines[1])
    }

    @Test
    fun 末尾に改行がない場合に全行が読み込まれること() {
        val text = "line1\nline2"
        tempFile.writeBytes(text.toByteArray(Charsets.UTF_8))
        val lines = readLinesWithEncoding(tempFile)
        assertEquals(2, lines.size)
        assertEquals("line1", lines[0])
        assertEquals("line2", lines[1])
    }
}
