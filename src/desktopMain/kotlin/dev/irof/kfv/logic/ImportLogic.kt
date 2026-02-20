package dev.irof.kfv.logic

import dev.irof.kfv.models.AppConfig
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.*

/**
 * 指定されたフォルダから特定のテキストファイルを検出し、
 * 棋譜の内容に基づいてリネームしてカレントディレクトリへ移動します。
 */
fun importShogiQuestFiles(sourceDir: Path, targetDir: Path): Int {
    if (!sourceDir.exists() || !targetDir.exists()) return 0
    
    val txtFiles = sourceDir.listDirectoryEntries("*.txt").filter { it.isRegularFile() }
    
    var count = 0
    val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneId.systemDefault())
    
    val nameCleanupRegex = Regex("""\(.*?\)""")

    txtFiles.forEach { file ->
        try {
            val lines = readLinesWithEncoding(file)
            if (lines.isEmpty()) return@forEach
            
            // CSA判定: 先頭10行以内に N+ または N- があるか
            val headerLines = lines.take(10)
            val hasCsaMarker = headerLines.any { it.startsWith("N+") || it.startsWith("N-") }
            if (!hasCsaMarker) return@forEach
            
            var sente = "unknown"
            var gote = "unknown"
            
            // 名前抽出
            lines.forEach { line ->
                if (line.startsWith("N+")) {
                    sente = line.substring(2).replace(nameCleanupRegex, "").trim()
                } else if (line.startsWith("N-")) {
                    gote = line.substring(2).replace(nameCleanupRegex, "").trim()
                }
            }
            
            // タイムスタンプから日付取得
            val dateStr = dateFormatter.format(file.getLastModifiedTime().toInstant())
            
            // 新しいファイル名: {YYYYMMDD}-{先手}-{後手}.csa
            val newFileName = "$dateStr-$sente-$gote.csa"
            val targetFile = targetDir / newFileName
            
            // 移動（コピーして削除）
            file.copyTo(targetFile, overwrite = true)
            file.deleteExisting()
            count++
        } catch (e: Exception) {
            println("Failed to import ${file.name}: ${e.message}")
        }
    }
    return count
}
