package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

/**
 * ファイル操作に関するロジックを ViewModel から切り出したクラス。
 */
class FileActionHandler(
    private val repository: KifuRepository,
    private val boardState: ShogiBoardState,
    private val onError: (String, String?) -> Unit,
    private val onInfo: (String) -> Unit,
    private val onFileRenamed: (Path) -> Unit,
    private val onFilesChanged: () -> Unit,
    private val onAutoFlip: () -> Unit,
) {
    fun selectFile(path: Path) {
        val ext = path.extension.lowercase()
        if (ext !in listOf("kifu", "kif", "csa")) {
            boardState.clear()
            return
        }
        executeParse(path) { repository.parse(it, boardState) }
    }

    fun forceParse(path: Path) {
        executeParse(path) { repository.parseManually(it, boardState) }
    }

    private fun executeParse(path: Path, parseAction: (Path) -> Unit) {
        try {
            parseAction(path)
            onAutoFlip()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            boardState.clear()
            val message = if (e is KifuParseException) "棋譜パースエラー" else "ファイルの読み込みに失敗しました"
            onError(message, e.message)
        }
    }

    fun performRename(path: Path, newName: String) {
        val newPath = repository.renameFileTo(path, newName)
        if (newPath != null) {
            onFileRenamed(newPath)
            onFilesChanged()
        } else {
            onError("リネームエラー", "ファイルの移動に失敗しました。")
        }
    }

    fun performCsaConversion(path: Path) {
        try {
            repository.convertCsa(path)
            onInfo("KIFU形式に変換しました。")
            onFilesChanged()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.error(e) { "Failed to convert CSA file: ${path.nameWithoutExtension}" }
            onError("変換エラー", e.message)
        }
    }

    fun writeGameResult(path: Path, result: String) {
        try {
            repository.updateResult(path, result)
            onFilesChanged()
            onInfo("終局結果「$result」を追記しました。")
        } catch (e: IOException) {
            onError("書込エラー", e.message)
        }
    }

    fun updateMetadata(path: Path, event: String, startTime: String) {
        try {
            repository.updateHeader(path, event, startTime)
            onFilesChanged()
            onInfo("棋譜情報を更新しました。")
        } catch (e: IOException) {
            onError("書込エラー", e.message)
        }
    }
}
