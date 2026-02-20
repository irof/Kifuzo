package dev.irof.kfv.logic

import dev.irof.kfv.models.AppConfig
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.*

/**
 * Downloadsフォルダから特定のテキストファイルを検出し、
 * 棋譜の内容に基づいて適切なフォルダへリネームして移動します。
 */
fun importShogiQuestFiles(): Int {
    val downloadsDir = AppConfig.USER_HOME_PATH / "Downloads"
    if (!downloadsDir.exists()) return 0

    val txtFiles = downloadsDir.listDirectoryEntries("*.txt").filter { it.isRegularFile() }

    var count = 0
    val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneId.systemDefault())

    val nameCleanupRegex = Regex("""\(.*?\)""")

    txtFiles.forEach { file ->
        try {
            val lines = readLinesWithEncoding(file)
            if (lines.isEmpty()) return@forEach

            val headerLines = lines.take(10)
            val hasCsaMarker = headerLines.any { it.startsWith("N+") || it.startsWith("N-") }
            if (!hasCsaMarker) return@forEach

            val isQuest = headerLines.firstOrNull()?.trim() == "'Shogi Quest"
            var sente = "unknown"
            var gote = "unknown"

            lines.forEach { line ->
                if (line.startsWith("N+")) {
                    sente = line.substring(2).replace(nameCleanupRegex, "").trim()
                } else if (line.startsWith("N-")) {
                    gote = line.substring(2).replace(nameCleanupRegex, "").trim()
                }
            }

            val dateStr = dateFormatter.format(file.getLastModifiedTime().toInstant())
            val targetDir = if (isQuest) AppConfig.QUEST_CSA_DIR else AppConfig.KIFU_ROOT
            if (!targetDir.exists()) targetDir.createDirectories()

            val newFileName = "csa-$dateStr-$sente-$gote.csa"
            val targetFile = targetDir / newFileName

            file.copyTo(targetFile, overwrite = true)
            file.deleteExisting()
            count++
        } catch (e: Exception) {
            println("Failed to import ${file.name}: ${e.message}")
        }
    }
    return count
}
