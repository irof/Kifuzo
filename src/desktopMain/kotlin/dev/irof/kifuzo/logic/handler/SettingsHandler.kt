package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path

/**
 * 設定保存に関するロジックを ViewModel から切り出したクラス。
 */
class SettingsHandler(
    private val repository: KifuRepository,
    private val boardState: ShogiBoardState,
    private val onFilesChanged: () -> Unit,
    private val onAutoFlip: (Boolean) -> Unit,
) {
    fun renameWithTemplate(path: Path, template: String) {
        val proposedName = repository.generateProposedName(path, template) ?: return
        val newPath = repository.renameFileTo(path, proposedName)
        if (newPath != null) {
            onFilesChanged()
        }
    }

    fun updateAutoFlip(myNameRegex: String) {
        if (myNameRegex.isEmpty()) return
        try {
            val regex = Regex(myNameRegex)
            val sente = boardState.session.senteName
            val gote = boardState.session.goteName

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
