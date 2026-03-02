package dev.irof.kifuzo.logic.service

import java.nio.file.Files
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertTrue

class KifuUpdaterTest {

    @Test
    fun updateKifuHeaderがKIFファイルのヘッダーを更新できること() {
        val dir = createTempDirectory("kifuzo-updater-test")
        try {
            val file = dir.resolve("test.kifu").createFile()
            file.writeText(
                """
                # KIF形式
                棋戦：旧棋戦
                開始日時：2026/01/01
                指し手
                """.trimIndent(),
            )

            updateKifuHeader(file, "新棋戦", "2026/03/02 12:00:00")

            val lines = file.readLines()
            assertTrue(lines.any { it.contains("棋戦：新棋戦") })
            assertTrue(lines.any { it.contains("開始日時：2026/03/02 12:00:00") })
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun updateKifuResultがCSAファイルに結果を書き込めること() {
        val dir = createTempDirectory("kifuzo-updater-csa")
        try {
            val file = dir.resolve("test.csa").createFile()
            file.writeText(
                """
                V2.2
                N+Sente
                N-Gote
                +7776FU
                """.trimIndent(),
            )

            updateKifuResult(file, "TORYO")

            val content = Files.readString(file)
            assertTrue(content.contains("%TORYO"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
