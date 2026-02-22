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
    fun testReadTextWithUtf8() {
        val text = "あいうえお"
        tempFile.writeBytes(text.toByteArray(Charsets.UTF_8))

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }

    @Test
    fun testReadTextWithShiftJis() {
        val text = "あいうえお"
        val sjis = Charset.forName("Shift_JIS")
        tempFile.writeBytes(text.toByteArray(sjis))

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }

    @Test
    fun testReadTextWithAscii() {
        val text = "abcde12345"
        tempFile.writeBytes(text.toByteArray(Charsets.US_ASCII))

        val readText = readTextWithEncoding(tempFile)
        assertEquals(text, readText)
    }
}
