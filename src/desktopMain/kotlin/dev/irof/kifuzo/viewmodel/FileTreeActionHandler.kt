package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileTreeNode
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * ファイルツリー操作に関連するアクションハンドラー。
 */
class FileTreeActionHandler(
    private val viewModel: KifuzoViewModel,
    private val settings: AppSettings,
    private val fileTreeManager: FileTreeManager,
) {
    fun handle(action: KifuzoAction): Boolean = when (action) {
        is KifuzoAction.SetRootDirectory -> {
            viewModel.updateRootDirectory(action.path)
            true
        }
        is KifuzoAction.ToggleDirectory -> {
            handleToggleDirectory(action.node)
            true
        }
        is KifuzoAction.SetViewMode -> {
            viewModel.updateUiState { it.copy(viewMode = action.mode) }
            viewModel.refreshFiles()
            true
        }
        is KifuzoAction.SetFileSortOption -> {
            settings.fileSortOption = action.option
            viewModel.updateUiState { it.copy(fileSortOption = action.option) }
            viewModel.refreshFiles()
            true
        }
        is KifuzoAction.ToggleFileFilter -> {
            handleToggleFileFilter(action.filter)
            true
        }
        is KifuzoAction.RefreshFiles -> {
            viewModel.refreshFiles()
            true
        }
        else -> false
    }

    private fun handleToggleDirectory(node: FileTreeNode) {
        try {
            val newNodes = fileTreeManager.toggleNode(node, viewModel.uiState.treeNodes, viewModel.uiState.fileSortOption)
            viewModel.updateUiState { it.copy(treeNodes = newNodes) }
        } catch (e: java.io.IOException) {
            logger.error(e) { "Failed to toggle directory: ${node.path}" }
            viewModel.updateUiState { it.copy(errorMessage = "フォルダの展開に失敗しました。", errorDetail = e.message) }
        }
    }

    private fun handleToggleFileFilter(filter: FileFilter) {
        viewModel.updateUiState {
            val newFilters = if (it.fileFilters.contains(filter)) it.fileFilters - filter else it.fileFilters + filter
            it.copy(fileFilters = newFilters)
        }
        viewModel.refreshFiles()
    }
}
