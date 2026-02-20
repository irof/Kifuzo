package logic

import models.AppConfig
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Downloadsフォルダから特定のテキストファイルを検出し、
 * 棋譜の内容に基づいて適切なフォルダへリネームして移動します。
 */
fun importShogiQuestFiles(): Int {
    val downloadsDir = File(AppConfig.USER_HOME, "Downloads")
    
    val txtFiles = downloadsDir.listFiles { file ->
        file.isFile && file.extension.lowercase() == "txt"
    } ?: return 0
    
    var count = 0
    // java.time.format.DateTimeFormatter を使用
    val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        .withZone(ZoneId.systemDefault())
    
    // 名前から丸括弧とその中身を削除するための正規表現
    val nameCleanupRegex = Regex("""\(.*?\)""")

    txtFiles.forEach { file ->
        try {
            val lines = readLinesWithEncoding(file)
            if (lines.isEmpty()) return@forEach
            
            // CSA判定: 先頭10行以内に N+ または N- があるか
            val headerLines = lines.take(10)
            val hasCsaMarker = headerLines.any { it.startsWith("N+") || it.startsWith("N-") }
            if (!hasCsaMarker) return@forEach
            
            // 情報抽出
            val isQuest = headerLines.firstOrNull()?.trim() == "'Shogi Quest"
            var sente = "unknown"
            var gote = "unknown"
            
            // ファイル全体から名前を探す
            lines.forEach { line ->
                if (line.startsWith("N+")) {
                    sente = line.substring(2).replace(nameCleanupRegex, "").trim()
                } else if (line.startsWith("N-")) {
                    gote = line.substring(2).replace(nameCleanupRegex, "").trim()
                }
            }
            
            // java.time を使ってタイムスタンプから日付文字列を取得
            val dateStr = dateFormatter.format(Instant.ofEpochMilli(file.lastModified()))
            
            // 保存先決定
            val targetDir = if (isQuest) AppConfig.QUEST_CSA_DIR else AppConfig.KIFU_ROOT
            if (!targetDir.exists()) targetDir.mkdirs()
            
            // 新しいファイル名: csa-{YYYYMMDD}-{先手}-{後手}.csa
            val newFileName = "csa-$dateStr-$sente-$gote.csa"
            val targetFile = File(targetDir, newFileName)
            
            // 移動（コピーして削除）
            file.copyTo(targetFile, overwrite = true)
            file.delete()
            count++
        } catch (e: Exception) {
            println("Failed to import ${file.name}: ${e.message}")
        }
    }
    return count
}
