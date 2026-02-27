package dev.irof.kifuzo.logic

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
}
