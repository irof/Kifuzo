package dev.irof.kifuzo.logic

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.Charsets

class KifuUpdateTest {

    @Test
    fun 終局行を追記する際に空行が挿入されないこと() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            // 末尾に改行があるファイルを用意
            val content = """
                |   1 ７六歩(77)
                |   2 ３四歩(33)
                |
            """.trimMargin()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuResult(tempFile, "投了")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            val expected = """
                |   1 ７六歩(77)
                |   2 ３四歩(33)
                |   3 投了
            """.trimMargin()
            assertEquals(expected, resultText)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun すでに終局行がある場合は上書きすること() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            val content = """
                |   1 ７六歩(77)
                |   2 投了
            """.trimMargin()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuResult(tempFile, "中断")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            val expected = """
                |   1 ７六歩(77)
                |   2 中断
            """.trimMargin()
            assertEquals(expected, resultText)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}
