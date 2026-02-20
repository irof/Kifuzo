package logic

import java.io.File

/**
 * Downloadsフォルダから将棋クエストの棋譜（.txt）を検出し、
 * ~/Kifu/quest/csa フォルダへコピー・整理します。
 */
fun importShogiQuestFiles(): Int {
    val userHome = System.getProperty("user.home")
    val downloadsDir = File(userHome, "Downloads")
    val targetDir = File(userHome, "Kifu/quest/csa")
    
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }
    
    // 将棋クエストの棋譜ファイル（通常は .txt で CSA形式の内容を持つもの）を検索
    // ここではファイル名に "sq" が含まれるか、内容が V2.2 で始まるものを対象とします
    val files = downloadsDir.listFiles { file ->
        file.isFile && file.extension.lowercase() == "txt" && 
        (file.name.contains("sq", ignoreCase = true) || file.readText().trim().startsWith("V2.2"))
    } ?: return 0
    
    var count = 0
    files.forEach { file ->
        try {
            // CSAとして保存するため拡張子を変更
            val newName = file.nameWithoutExtension + ".csa"
            val targetFile = File(targetDir, newName)
            file.copyTo(targetFile, overwrite = true)
            // 元ファイルを削除（インポート完了）
            file.delete()
            count++
        } catch (e: Exception) {
            println("Failed to import ${file.name}: ${e.message}")
        }
    }
    return count
}
