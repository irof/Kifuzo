package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
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
        if (ext in listOf("kifu", "kif", "csa")) {
            executeParse(path) { repository.parse(it, boardState) }
        } else {
            boardState.clear()
        }
    }

    fun forceParse(path: Path) {
        executeParse(path) { repository.parseManually(it, boardState) }
    }

    @Suppress("PrintStackTrace")
    private fun executeParse(path: Path, parseAction: (Path) -> Unit) {
        try {
            parseAction(path)
            onAutoFlip()
        } catch (cause: Exception) {
            boardState.clear()
            val message = if (cause is KifuParseException) "棋譜パースエラー" else "ファイルの読み込みに失敗しました"
            val detail = if (cause is KifuParseException) {
                cause.message
            } else {
                val sw = java.io.StringWriter()
                cause.printStackTrace(java.io.PrintWriter(sw))
                "${cause.message}\n\n$sw"
            }
            onError(message, detail)
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
        } catch (cause: Exception) {
            logger.error(cause) { "Failed to convert CSA file: ${path.nameWithoutExtension}" }
            onError("変換エラー", cause.message)
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

    fun openInExternalApp(path: Path) {
        try {
            val absolutePath = path.toAbsolutePath().toString()
            val command = arrayOf("open", "-n", "/Applications/ShogiHome.app", absolutePath)
            Runtime.getRuntime().exec(command)
            onInfo("ShogiHomeで開いています...")
        } catch (e: java.io.IOException) {
            logger.error(e) { "Failed to execute open command" }
            onError("起動エラー", "ShogiHomeの起動に失敗しました。/Applications/ShogiHome.app が存在するか確認してください。\n\n${e.message}")
        } catch (e: SecurityException) {
            logger.error(e) { "Security restriction prevented opening ShogiHome" }
            onError("起動エラー", "セキュリティ制限によりShogiHomeを起動できませんでした。\n\n${e.message}")
        }
    }

    fun savePastedKifu(root: Path, filename: String, text: String) {
        try {
            val targetPath = root.resolve(filename)
            dev.irof.kifuzo.logic.io.writeTextToFile(targetPath, text)
            onInfo("棋譜を保存しました: $filename")
            onFileRenamed(targetPath)
            onFilesChanged()
        } catch (e: java.io.IOException) {
            logger.error(e) { "Failed to save pasted kifu" }
            onError("保存エラー", e.message)
        }
    }
}
