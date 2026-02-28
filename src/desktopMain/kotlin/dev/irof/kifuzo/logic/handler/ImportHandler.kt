package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.logic.service.KifuImportService
import java.nio.file.Path

/**
 * 棋譜インポートに関するロジックを ViewModel から切り出したクラス。
 */
class ImportHandler(
    private val importService: KifuImportService,
    private val onInfo: (String) -> Unit,
    private val onImported: () -> Unit,
) {
    fun performImport(sourceDir: Path, targetDir: Path) {
        val count = importService.importQuestFiles(sourceDir, targetDir)
        if (count > 0) {
            onInfo("${count}件の棋譜をインポートしました。")
            onImported()
        } else {
            onInfo("インポート対象のファイルが見つかりませんでした。")
        }
    }
}
