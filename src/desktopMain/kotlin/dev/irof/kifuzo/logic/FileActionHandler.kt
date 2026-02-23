package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

/**
 * 棋譜ファイルに対する各種操作をハンドルするクラス。
 */
class FileActionHandler(
    private val repository: KifuRepository,
    private val boardState: ShogiBoardState,
    private val onError: (String) -> Unit,
    private val onInfo: (String) -> Unit,
    private val onFileRenamed: (Path) -> Unit,
    private val onFilesChanged: () -> Unit,
    private val onAutoFlip: () -> Unit,
) {
    fun selectFile(path: Path) {
        val ext = path.extension.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                repository.parse(path, boardState)
                onAutoFlip()
            } catch (e: KifuParseException) {
                onError("棋譜パースエラー: ${path.name}\n\n${e.message}")
                boardState.clear()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                onError("予期せぬエラー: ${path.name}\n\n${e.message}")
                boardState.clear()
            }
        } else {
            boardState.clear()
        }
    }

    fun renameFile(path: Path, template: String) {
        val newPath = repository.renameKifuFile(path, template)
        if (newPath != null) {
            onFilesChanged()
            onFileRenamed(newPath)
        } else {
            onError("ファイルのリネームに失敗しました。棋譜内に必要な情報が不足しているか、同名のファイルが既に存在する可能性があります。")
        }
    }

    fun performCsaConversion(path: Path) {
        val targetFile = repository.convertCsa(path)
        try {
            val tempState = ShogiBoardState()
            repository.parse(targetFile, tempState)
            val senkei = detectSenkei(tempState.session.history)
            if (senkei.isNotEmpty()) repository.updateSenkei(targetFile, senkei)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.error(e) { "Failed to parse converted CSA file or update senkei: $targetFile" }
        }
        onFilesChanged()
    }

    fun detectAndWriteSenkei(path: Path) {
        val senkei = detectSenkei(boardState.session.history)
        if (senkei.isNotEmpty()) {
            repository.updateSenkei(path, senkei)
            onFilesChanged()
            onInfo("戦型を「$senkei」として追記しました。")
        }
    }

    fun writeGameResult(path: Path, result: String) {
        repository.updateResult(path, result)
        onFilesChanged()
        selectFile(path) // 再読み込み
        onInfo("終局結果を「$result」として追記しました。")
    }
}
