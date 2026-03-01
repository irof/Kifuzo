package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.models.AppSettings
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * UI 設定、表示、貼り付けに関連するアクションハンドラー。
 */
class UiActionHandler(
    private val viewModel: KifuzoViewModel,
    private val repository: KifuRepository,
    private val settings: AppSettings,
    private val fileActionHandler: FileActionHandler,
    private val importHandler: ImportHandler,
    private val settingsHandler: SettingsHandler,
) {
    fun handle(action: KifuzoAction): Boolean = when (action) {
        is KifuzoAction.SaveSettings -> handleSaveSettings(action)
        is KifuzoAction.ShowSettings -> {
            viewModel.updateUiState { it.copy(showSettings = action.show) }
            true
        }
        is KifuzoAction.ShowImportDialog -> {
            viewModel.updateUiState { it.copy(showImportDialog = action.show) }
            true
        }
        is KifuzoAction.ImportFiles -> {
            viewModel.currentRootDirectory?.let { importHandler.performImport(action.sourceDir, it) }
            true
        }
        is KifuzoAction.SetViewingText, is KifuzoAction.ToggleFlipped, is KifuzoAction.ClearErrorAndInfo -> {
            handleDisplayAction(action)
            true
        }
        is KifuzoAction.UpdateSidebarWidth, is KifuzoAction.ToggleSidebar, is KifuzoAction.ToggleMoveList -> {
            handleSidebarAction(action)
            true
        }
        is KifuzoAction.PasteKifu, is KifuzoAction.ShowSavePastedKifuDialog, is KifuzoAction.HideSavePastedKifuDialog, is KifuzoAction.SavePastedKifu -> {
            handlePasteAction(action)
            true
        }
        else -> false
    }

    private fun handleSaveSettings(action: KifuzoAction.SaveSettings): Boolean {
        settings.myNameRegex = action.regex
        settings.filenameTemplate = action.template
        settingsHandler.updateAutoFlip(action.regex)
        viewModel.updateUiState { it.copy(myNameRegex = action.regex, filenameTemplate = action.template, showSettings = false) }
        return true
    }

    private fun handleDisplayAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.SetViewingText -> viewModel.updateUiState { it.copy(viewingText = action.text) }
            is KifuzoAction.ToggleFlipped -> viewModel.updateUiState { it.copy(isFlipped = !it.isFlipped) }
            is KifuzoAction.ClearErrorAndInfo -> viewModel.updateUiState { it.copy(errorMessage = null, errorDetail = null, infoMessage = null) }
            else -> {}
        }
    }

    private fun handleSidebarAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.UpdateSidebarWidth -> handleUpdateSidebarWidth(action.delta)
            is KifuzoAction.ToggleSidebar -> viewModel.updateUiState { it.copy(isSidebarVisible = !it.isSidebarVisible) }
            is KifuzoAction.ToggleMoveList -> viewModel.updateUiState { it.copy(isMoveListVisible = !it.isMoveListVisible) }
            else -> {}
        }
    }

    private fun handleUpdateSidebarWidth(delta: Float) {
        val newWidth = (viewModel.uiState.sidebarWidth + delta).coerceIn(
            dev.irof.kifuzo.models.AppConfig.MIN_SIDEBAR_WIDTH,
            dev.irof.kifuzo.models.AppConfig.MAX_SIDEBAR_WIDTH,
        )
        settings.sidebarWidth = newWidth
        viewModel.updateUiState { it.copy(sidebarWidth = newWidth) }
    }

    private fun handlePasteAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.PasteKifu -> viewModel.performPasteKifu()
            is KifuzoAction.ShowSavePastedKifuDialog -> handleShowSavePastedDialog()
            is KifuzoAction.HideSavePastedKifuDialog -> viewModel.updateUiState { it.copy(pastedKifuProposedName = null) }
            is KifuzoAction.SavePastedKifu -> handleSavePastedKifuEvent(action)
            else -> {}
        }
    }

    private fun handleSavePastedKifuEvent(action: KifuzoAction.SavePastedKifu) {
        viewModel.currentRootDirectory?.let { fileActionHandler.savePastedKifu(it, action.filename, action.text) }
            ?: run { viewModel.updateUiState { it.copy(errorMessage = "保存先ディレクトリが選択されていません。") } }
        viewModel.updateUiState { it.copy(pastedKifuText = null, pastedKifuProposedName = null) }
    }

    internal fun handleShowSavePastedDialog() {
        viewModel.uiState.pastedKifuText?.let { text ->
            val info = repository.scanKifuInfo(text.lines())
            val proposedName = repository.generateProposedNameForPasted(info, viewModel.uiState.filenameTemplate)
            viewModel.updateUiState { it.copy(pastedKifuProposedName = proposedName ?: "pasted_kifu.kifu") }
        }
    }
}
