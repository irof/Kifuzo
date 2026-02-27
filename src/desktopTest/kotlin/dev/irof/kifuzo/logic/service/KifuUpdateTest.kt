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
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
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

    @Test
    fun 棋戦情報を追加する際に手数行より上に挿入されること() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            val content = """
                先手：先手
                後手：後手
                手数----指手---------消費時間--
                   1 ７六歩(77)
            """.trimIndent()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuHeader(tempFile, "第1期蔵王戦", "2026/02/24")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            // 期待値: 棋戦と開始日時が「手数」行より前に来ること
            val expected = """
                先手：先手
                後手：後手
                棋戦：第1期蔵王戦
                開始日時：2026/02/24
                手数----指手---------消費時間--
                   1 ７六歩(77)
            """.trimIndent()
            assertEquals(expected, resultText)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun 既存の情報を更新できること() {
        val tempFile = Files.createTempFile("kifuzo_test", ".kifu")
        try {
            val content = """
                棋戦：旧棋戦
                開始日時：2000/01/01
                手数----指手---------消費時間--
                   1 ７六歩(77)
            """.trimIndent()
            Files.write(tempFile, content.toByteArray(Charsets.UTF_8))

            updateKifuHeader(tempFile, "新棋戦", "2026/02/24")

            val resultText = Files.readString(tempFile, Charsets.UTF_8)
            val expected = """
                棋戦：新棋戦
                開始日時：2026/02/24
                手数----指手---------消費時間--
                   1 ７六歩(77)
            """.trimIndent()
            assertEquals(expected, resultText)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}
