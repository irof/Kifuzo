package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.service.KifuRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

/**
 * ファイル編集（リネーム、変換、メタデータ等）に関連するアクションハンドラー。
 */
class FileEditActionHandler(
    private val viewModel: KifuzoViewModel,
    private val repository: KifuRepository,
    private val fileActionHandler: FileActionHandler,
) {
    fun handle(action: KifuzoAction): Boolean = when (action) {
        is KifuzoAction.SelectFile -> {
            fileActionHandler.selectFile(action.path)
            viewModel.updateUiState { it.copy(selectedFile = action.path, pastedKifuText = null) }
            true
        }
        is KifuzoAction.SelectNextFile, is KifuzoAction.SelectPrevFile -> {
            handleSelectAdjacentFile(action is KifuzoAction.SelectNextFile)
            true
        }
        is KifuzoAction.ShowRenameDialog, is KifuzoAction.HideRenameDialog, is KifuzoAction.PerformRename -> {
            handleRenameEvent(action)
            true
        }
        is KifuzoAction.ConvertCsa, is KifuzoAction.ForceParseAsKifu, is KifuzoAction.ConfirmOverwrite, is KifuzoAction.HideOverwriteConfirm -> {
            handleParseEvent(action)
            true
        }
        is KifuzoAction.WriteGameResult, is KifuzoAction.UpdateMetadata, is KifuzoAction.OpenInExternalApp -> {
            handleMetadataAction(action)
            true
        }
        is KifuzoAction.ShowEditMetadataDialog -> {
            viewModel.updateUiState { it.copy(editMetadataTarget = action.path) }
            true
        }
        is KifuzoAction.HideEditMetadataDialog -> {
            viewModel.updateUiState { it.copy(editMetadataTarget = null) }
            true
        }
        else -> false
    }

    private fun handleRenameEvent(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.ShowRenameDialog -> handleShowRenameDialog(action.path)
            is KifuzoAction.HideRenameDialog -> viewModel.updateUiState { it.copy(renameTarget = null, proposedRenameName = null) }
            is KifuzoAction.PerformRename -> {
                fileActionHandler.performRename(action.path, action.newName)
                viewModel.updateUiState { it.copy(renameTarget = null, proposedRenameName = null) }
            }
            else -> {}
        }
    }

    private fun handleParseEvent(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.ConvertCsa -> handleConvertCsa(action.path)
            is KifuzoAction.ForceParseAsKifu -> fileActionHandler.forceParse(action.path)
            is KifuzoAction.ConfirmOverwrite -> viewModel.uiState.showOverwriteConfirm?.let {
                fileActionHandler.performCsaConversion(it)
                viewModel.updateUiState { it.copy(showOverwriteConfirm = null) }
            }
            is KifuzoAction.HideOverwriteConfirm -> viewModel.updateUiState { it.copy(showOverwriteConfirm = null) }
            else -> {}
        }
    }

    private fun handleMetadataAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.WriteGameResult -> fileActionHandler.writeGameResult(action.path, action.result)
            is KifuzoAction.UpdateMetadata -> fileActionHandler.updateMetadata(action.path, action.event, action.startTime)
            is KifuzoAction.OpenInExternalApp -> fileActionHandler.openInExternalApp(action.path)
            else -> {}
        }
    }

    private fun handleSelectAdjacentFile(forward: Boolean) {
        val nodes = viewModel.uiState.treeNodes
        val currentIndex = viewModel.uiState.selectedFile?.let { selected -> nodes.indexOfFirst { it.path == selected } } ?: -1
        if (currentIndex != -1) {
            val targetIndices = if (forward) (currentIndex + 1 until nodes.size) else (currentIndex - 1 downTo 0)
            targetIndices.asSequence().map { nodes[it] }.firstOrNull()?.let { node ->
                fileActionHandler.selectFile(node.path)
                viewModel.updateUiState { it.copy(selectedFile = node.path) }
            }
        }
    }

    private fun handleShowRenameDialog(path: Path) {
        val info = viewModel.uiState.kifuInfos[path] ?: try {
            repository.scanKifuInfo(java.nio.file.Files.readAllLines(path))
        } catch (e: java.io.IOException) {
            logger.debug(e) { "Failed to read lines for rename info: $path" }
            dev.irof.kifuzo.models.KifuInfo(path)
        }
        val proposedName = repository.generateProposedName(path, info, viewModel.uiState.filenameTemplate) ?: path.fileName.toString()
        viewModel.updateUiState { it.copy(renameTarget = path, proposedRenameName = proposedName) }
    }

    private fun handleConvertCsa(path: Path) {
        val targetFile = (path.parent ?: path).resolve(path.fileName.nameWithoutExtension + ".kifu")
        if (java.nio.file.Files.exists(targetFile)) {
            viewModel.updateUiState { it.copy(showOverwriteConfirm = path) }
        } else {
            fileActionHandler.performCsaConversion(path)
        }
    }
}
