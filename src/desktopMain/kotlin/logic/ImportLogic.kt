package logic

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Downloadsフォルダから特定のテキストファイルを検出し、
 * 棋譜の内容に基づいて適切なフォルダへリネームして移動します。
 */
fun importShogiQuestFiles(): Int {
    val userHome = System.getProperty("user.home")
    val downloadsDir = File(userHome, "Downloads")
    val kifuRoot = File(userHome, "Kifu")
    val questDir = File(kifuRoot, "quest/csa")
    
    val txtFiles = downloadsDir.listFiles { file ->
        file.isFile && file.extension.lowercase() == "txt"
    } ?: return 0
    
    var count = 0
    val dateFormat = SimpleDateFormat("yyyyMMdd")
    // 名前から丸括弧とその中身を削除するための正規表現
    val nameCleanupRegex = Regex("""\(.*?\)""")

    txtFiles.forEach { file ->
        try {
            val lines = file.useLines { it.take(10).toList() }
            if (lines.isEmpty()) return@forEach
            
            // CSA判定: 先頭10行以内に N+ または N- があるか
            val hasCsaMarker = lines.any { it.startsWith("N+") || it.startsWith("N-") }
            if (!hasCsaMarker) return@forEach
            
            // 情報抽出
            val isQuest = lines.firstOrNull()?.trim() == "'Shogi Quest"
            var sente = "unknown"
            var gote = "unknown"
            
            // ファイル全体から名前を探す（念のため）
            file.forEachLine { line ->
                if (line.startsWith("N+")) {
                    sente = line.substring(2).replace(nameCleanupRegex, "").trim()
                } else if (line.startsWith("N-")) {
                    gote = line.substring(2).replace(nameCleanupRegex, "").trim()
                }
            }
            
            // タイムスタンプから日付取得
            val dateStr = dateFormat.format(Date(file.lastModified()))
            
            // 保存先決定
            val targetDir = if (isQuest) questDir else kifuRoot
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
