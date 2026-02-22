package dev.irof.kifuzo.logic

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.text.Charsets

class KifuUpdateTest {

    @Test
    fun 終局行を追記する際に空行が挿入されないこと() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            // 末尾に改行があるファイルを用意
            val content = """
                1 ７六歩(77)
                2 ３四歩(33)

            """.trimIndent()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuResult(tempFile, "投了")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            // 空行が含まれていないことを確認
            assertFalse(resultText.contains("\n\n"), "空行が含まれていないこと:\n$resultText")
            assertTrue(resultText.contains("   3 投了"), "終局行が含まれていること:\n$resultText")
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun すでに終局行がある場合は上書きすること() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            val content = """
                1 ７六歩(77)
                2 投了
            """.trimIndent()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuResult(tempFile, "中断")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            assertFalse(resultText.contains("投了"), "古い結果が残っていないこと")
            assertTrue(resultText.contains("   2 中断"), "新しい結果が書き込まれていること")
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun 戦型を追記する際に空行が挿入されないこと() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            val content = """
                先手：先手
                後手：後手

                1 ７六歩(77)
                2 ３四歩(33)

            """.trimIndent()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuSenkei(tempFile, "矢倉")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            assertFalse(resultText.contains("\n\n\n"), "空行が連続して増えていないこと")
            assertTrue(resultText.contains("戦型：矢倉"), "戦型が追記されていること")
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}
