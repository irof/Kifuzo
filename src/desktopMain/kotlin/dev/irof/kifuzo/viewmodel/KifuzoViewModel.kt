package dev.irof.kifuzo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kifuzo.logic.FileActionHandler
import dev.irof.kifuzo.logic.FileTreeManager
import dev.irof.kifuzo.logic.ImportHandler
import dev.irof.kifuzo.logic.KifuRepository
import dev.irof.kifuzo.logic.KifuRepositoryImpl
import dev.irof.kifuzo.logic.SettingsHandler
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

class KifuzoViewModel(
    private val repository: KifuRepository = KifuRepositoryImpl(),
) {
    companion object {
        private const val SCAN_DEPTH = 3
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val fileTreeManager = FileTreeManager(repository)
    val boardState = ShogiBoardState()

    private val fileActionHandler = FileActionHandler(
        repository = repository,
        boardState = boardState,
        onError = { msg, detail -> updateState { it.copy(errorMessage = msg, errorDetail = detail) } },
        onInfo = { msg -> updateState { it.copy(infoMessage = msg) } },
        onFileRenamed = { path -> updateState { it.copy(selectedFile = path) } },
        onFilesChanged = { refreshFiles() },
        onAutoFlip = { settingsHandler.updateAutoFlip(uiState.myNameRegex) },
    )

    private val importHandler = ImportHandler(
        repository = repository,
        onInfo = { msg -> updateState { it.copy(infoMessage = msg) } },
        onImported = {
            updateState { it.copy(showImportDialog = false) }
            refreshFiles()
        },
    )

    private val settingsHandler = SettingsHandler(
        boardState = boardState,
        onAutoFlip = { flipped -> updateState { it.copy(isFlipped = flipped) } },
    )

    var currentRootDirectory by mutableStateOf<Path?>(
        AppSettings.lastRootDir.let {
            if (it.isNotEmpty()) {
                val path = java.nio.file.Paths.get(it)
                if (java.nio.file.Files.exists(path)) path else null
            } else {
                null
            }
        },
    )
        private set

    var uiState by mutableStateOf(
        KifuzoUiState(
            myNameRegex = AppSettings.myNameRegex,
            filenameTemplate = AppSettings.filenameTemplate,
            sidebarWidth = AppSettings.sidebarWidth,
        ),
    )
        private set

    fun dispatch(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.SetRootDirectory,
            is KifuzoAction.ToggleDirectory,
            is KifuzoAction.SetViewMode,
            is KifuzoAction.SetFileSortOption,
            is KifuzoAction.ToggleFileFilter,
            is KifuzoAction.RefreshFiles,
            -> handleFileTreeAction(action)

            is KifuzoAction.SelectFile,
            is KifuzoAction.SelectNextFile,
            is KifuzoAction.SelectPrevFile,
            is KifuzoAction.ShowRenameDialog,
            is KifuzoAction.HideRenameDialog,
            is KifuzoAction.PerformRename,
            is KifuzoAction.ConvertCsa,
            is KifuzoAction.ConfirmOverwrite,
            is KifuzoAction.HideOverwriteConfirm,
            is KifuzoAction.WriteGameResult,
            -> handleFileAction(action)

            is KifuzoAction.SaveSettings,
            is KifuzoAction.SetViewingText,
            is KifuzoAction.ToggleFlipped,
            is KifuzoAction.ShowSettings,
            is KifuzoAction.ShowImportDialog,
            is KifuzoAction.ClearErrorAndInfo,
            is KifuzoAction.ImportFiles,
            is KifuzoAction.UpdateSidebarWidth,
            is KifuzoAction.ToggleSidebar,
            -> handleUiAction(action)

            is KifuzoAction.ChangeStep,
            is KifuzoAction.NextStep,
            is KifuzoAction.PrevStep,
            -> handleStepAction(action)
        }
    }

    private fun handleFileTreeAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.SetRootDirectory -> {
                currentRootDirectory = action.path
                AppSettings.lastRootDir = action.path.toString()
                updateState { it.copy(treeNodes = emptyList()) }
                refreshFiles()
            }
            is KifuzoAction.ToggleDirectory -> {
                val newNodes = fileTreeManager.toggleNode(action.node, uiState.treeNodes, uiState.fileSortOption)
                updateState { it.copy(treeNodes = newNodes) }
            }
            is KifuzoAction.SetViewMode -> {
                updateState { it.copy(viewMode = action.mode) }
                refreshFiles()
            }
            is KifuzoAction.SetFileSortOption -> {
                AppSettings.fileSortOption = action.option
                updateState { it.copy(fileSortOption = action.option) }
                refreshFiles()
            }
            is KifuzoAction.ToggleFileFilter -> {
                updateState {
                    val newFilters = if (it.fileFilters.contains(action.filter)) it.fileFilters - action.filter else it.fileFilters + action.filter
                    it.copy(fileFilters = newFilters)
                }
                refreshFiles()
            }
            is KifuzoAction.RefreshFiles -> refreshFiles()
            else -> {}
        }
    }

    private fun handleFileAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.SelectFile -> fileActionHandler.selectFile(action.path).also { updateState { it.copy(selectedFile = action.path) } }
            is KifuzoAction.SelectNextFile -> selectAdjacentFile(forward = true)
            is KifuzoAction.SelectPrevFile -> selectAdjacentFile(forward = false)
            is KifuzoAction.ShowRenameDialog -> {
                val proposedName = repository.generateProposedName(action.path, uiState.filenameTemplate) ?: action.path.fileName.toString()
                updateState { it.copy(renameTarget = action.path, proposedRenameName = proposedName) }
            }
            is KifuzoAction.HideRenameDialog -> updateState { it.copy(renameTarget = null, proposedRenameName = null) }
            is KifuzoAction.PerformRename -> {
                fileActionHandler.performRename(action.path, action.newName)
                updateState { it.copy(renameTarget = null, proposedRenameName = null) }
            }
            is KifuzoAction.ConvertCsa -> {
                val targetFile = (action.path.parent ?: action.path).resolve(action.path.nameWithoutExtension + ".kifu")
                if (java.nio.file.Files.exists(targetFile)) {
                    updateState { it.copy(showOverwriteConfirm = action.path) }
                } else {
                    fileActionHandler.performCsaConversion(action.path)
                }
            }
            is KifuzoAction.ConfirmOverwrite -> {
                uiState.showOverwriteConfirm?.let {
                    fileActionHandler.performCsaConversion(it)
                    updateState { it.copy(showOverwriteConfirm = null) }
                }
            }
            is KifuzoAction.HideOverwriteConfirm -> updateState { it.copy(showOverwriteConfirm = null) }
            is KifuzoAction.WriteGameResult -> fileActionHandler.writeGameResult(action.path, action.result)
            else -> {}
        }
    }

    private fun handleUiAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.SaveSettings -> {
                settingsHandler.saveSettings(action.regex, action.template)
                updateState { it.copy(myNameRegex = action.regex, filenameTemplate = action.template, showSettings = false) }
            }
            is KifuzoAction.SetViewingText -> updateState { it.copy(viewingText = action.text) }
            is KifuzoAction.ToggleFlipped -> updateState { it.copy(isFlipped = !it.isFlipped) }
            is KifuzoAction.ShowSettings -> updateState { it.copy(showSettings = action.show) }
            is KifuzoAction.ShowImportDialog -> updateState { it.copy(showImportDialog = action.show) }
            is KifuzoAction.ClearErrorAndInfo -> updateState { it.copy(errorMessage = null, errorDetail = null, infoMessage = null) }
            is KifuzoAction.ImportFiles -> importHandler.importFiles(action.sourceDir, currentRootDirectory)
            is KifuzoAction.UpdateSidebarWidth -> updateSidebarWidth(action.delta)
            is KifuzoAction.ToggleSidebar -> updateState { it.copy(isSidebarVisible = !it.isSidebarVisible) }
            else -> {}
        }
    }

    private fun handleStepAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.ChangeStep -> boardState.currentStep = action.step
            is KifuzoAction.NextStep -> boardState.currentStep++
            is KifuzoAction.PrevStep -> boardState.currentStep--
            else -> {}
        }
    }

    private fun updateSidebarWidth(delta: Float) {
        val newWidth = (uiState.sidebarWidth + delta).coerceIn(
            dev.irof.kifuzo.models.AppConfig.MIN_SIDEBAR_WIDTH,
            dev.irof.kifuzo.models.AppConfig.MAX_SIDEBAR_WIDTH,
        )
        AppSettings.sidebarWidth = newWidth
        updateState { it.copy(sidebarWidth = newWidth) }
    }

    private fun selectAdjacentFile(forward: Boolean) {
        val selected = uiState.selectedFile ?: return
        val nodes = uiState.treeNodes
        val currentIndex = nodes.indexOfFirst { it.path == selected }
        if (currentIndex == -1) return

        val step = if (forward) 1 else -1
        var nextIndex = currentIndex + step
        while (nextIndex in nodes.indices) {
            val node = nodes[nextIndex]
            if (!node.isDirectory) {
                fileActionHandler.selectFile(node.path)
                updateState { it.copy(selectedFile = node.path) }
                return
            }
            nextIndex += step
        }
    }

    private fun updateState(update: (KifuzoUiState) -> KifuzoUiState) {
        uiState = update(uiState)
    }

    fun refreshFiles() {
        val root = currentRootDirectory ?: return
        val mode = uiState.viewMode
        val filters = uiState.fileFilters
        val sortOption = uiState.fileSortOption

        if (mode == FileViewMode.HIERARCHY) {
            val newNodes = fileTreeManager.buildTree(root, uiState.treeNodes, filters, sortOption)
            updateState { it.copy(treeNodes = newNodes) }
        } else {
            scope.launch {
                updateState { it.copy(isScanning = true) }
                val newNodes = withContext(Dispatchers.IO) {
                    fileTreeManager.buildFlatList(root, filters, sortOption)
                }
                updateState { it.copy(treeNodes = newNodes, isScanning = false) }
            }
        }

        // 棋譜情報のスキャン（対局者名、開始日時など）
        scope.launch {
            updateState { it.copy(isScanning = true) }
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(root, SCAN_DEPTH)
                    .filter { it.isRegularFile() && (it.extension.lowercase() in listOf("kifu", "kif", "csa")) }
                    .forEach { allKifuFiles.add(it) }
            }
            val infos = withContext(Dispatchers.IO) { repository.getKifuInfos(allKifuFiles) }
            updateState { it.copy(kifuInfos = infos, isScanning = false) }
        }
    }
}
