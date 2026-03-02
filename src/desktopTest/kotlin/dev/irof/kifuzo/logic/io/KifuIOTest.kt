package dev.irof.kifuzo.logic.io

import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class KifuIOTest {

    @Test
    fun readTextWithEncodingがUTF8を優先して読み込めること() {
        val dir = createTempDirectory("kifuzo-io-test")
        try {
            val file = dir.resolve("utf8.kifu")
            file.writeText("テスト", Charsets.UTF_8)
            assertEquals("テスト", readTextWithEncoding(file))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun readTextWithEncodingがShiftJISを判別して読み込めること() {
        val dir = createTempDirectory("kifuzo-io-sjis")
        try {
            val file = dir.resolve("sjis.kifu")
            val sjis = Charset.forName("Shift_JIS")
            file.writeBytes("テスト".toByteArray(sjis))
            assertEquals("テスト", readTextWithEncoding(file))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun readLinesWithEncodingが末尾の空行を適切に処理すること() {
        val dir = createTempDirectory("kifuzo-io-lines")
        try {
            val file = dir.resolve("lines.kifu")
            file.writeText("A\nB\n", Charsets.UTF_8)
            val lines = readLinesWithEncoding(file)
            assertEquals(2, lines.size)
            assertEquals("A", lines[0])
            assertEquals("B", lines[1])

            // 空ファイル
            val emptyFile = dir.resolve("empty.kifu")
            emptyFile.writeText("", Charsets.UTF_8)
            assertEquals(0, readLinesWithEncoding(emptyFile).size)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun writeTextToFileがUTF8で書き込むこと() {
        val dir = createTempDirectory("kifuzo-io-write")
        try {
            val file = dir.resolve("out.kifu")
            writeTextToFile(file, "出力テスト")
            assertEquals("出力テスト", java.nio.file.Files.readString(file, Charsets.UTF_8))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
