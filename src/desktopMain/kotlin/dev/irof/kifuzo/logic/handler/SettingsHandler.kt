package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * 設定保存に関するロジックを ViewModel から切り出したクラス。
 */
class SettingsHandler(
    private val repository: KifuRepository,
    private val settings: AppSettings,
    private val boardState: ShogiBoardState,
    private val onFilesChanged: () -> Unit,
    private val onAutoFlip: (Boolean) -> Unit,
) {
    fun renameWithTemplate(path: Path, template: String) {
        val info = try {
            repository.scanKifuInfo(java.nio.file.Files.readAllLines(path))
        } catch (e: java.io.IOException) {
            logger.error(e) { "Failed to read lines for renaming: $path" }
            return
        }
        val proposedName = repository.generateProposedName(path, info, template) ?: return
        val newPath = repository.renameFileTo(path, proposedName)
        if (newPath != null) {
            onFilesChanged()
        }
    }

    /**
     * 自分の名前設定に基づいて、現在の盤面を自動反転するか判定し、必要なら反転させます。
     */
    fun updateAutoFlip(myNameRegex: String) {
        if (myNameRegex.isBlank()) return

        val session = boardState.session
        val sente = session.senteName
        val gote = session.goteName

        try {
            val regex = myNameRegex.toRegex()
            if (regex.containsMatchIn(gote) && !regex.containsMatchIn(sente)) {
                onAutoFlip(true)
            } else if (regex.containsMatchIn(sente) && !regex.containsMatchIn(gote)) {
                onAutoFlip(false)
            }
        } catch (cause: Exception) {
            // 不正な正規表現の場合は単に無視する（ログは ViewModel 等で出してもよい）
        }
    }
}
