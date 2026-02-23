package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.AppSettings
import java.nio.file.Path

/**
 * 棋譜のインポート処理をハンドルするクラス。
 */
class ImportHandler(
    private val repository: KifuRepository,
    private val onInfo: (String) -> Unit,
    private val onImported: () -> Unit,
) {
    fun importFiles(sourceDir: Path, root: Path?) {
        if (root == null) return
        val count = repository.importQuestFiles(sourceDir, root)
        AppSettings.importSourceDir = sourceDir.toString()
        if (count > 0) {
            onInfo("${count}件の棋譜をインポートしました。")
            onImported()
        } else {
            onInfo("指定されたフォルダに該当する棋譜が見つかりませんでした。")
        }
    }
}
