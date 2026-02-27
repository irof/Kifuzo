package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.logic.service.KifuRepository
import java.nio.file.Path

/**
 * 棋譜インポートに関するロジックを ViewModel から切り出したクラス。
 */
class ImportHandler(
    private val repository: KifuRepository,
    private val onInfo: (String) -> Unit,
    private val onImported: () -> Unit,
) {
    fun performImport(sourceDir: Path, targetDir: Path) {
        val count = repository.importQuestFiles(sourceDir, targetDir)
        if (count > 0) {
            onInfo("${count}件の棋譜をインポートしました。")
            onImported()
        } else {
            onInfo("インポート対象のファイルが見つかりませんでした。")
        }
    }
}
