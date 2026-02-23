package dev.irof.kifuzo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kifuzo.logic.FileActionHandler
import dev.irof.kifuzo.logic.FileTreeManager
import dev.irof.kifuzo.logic.ImportHandler
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
        onError = { msg -> updateState { it.copy(errorMessage = msg) } },
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
            is KifuzoAction.SetRootDirectory -> setRootDirectory(action.path)
            is KifuzoAction.ToggleDirectory -> toggleDirectory(action.node)
            is KifuzoAction.SelectFile -> fileActionHandler.selectFile(action.path).also { updateState { it.copy(selectedFile = action.path) } }
            is KifuzoAction.SaveSettings -> {
                settingsHandler.saveSettings(action.regex, action.template)
                updateState { it.copy(myNameRegex = action.regex, filenameTemplate = action.template, showSettings = false) }
            }
            is KifuzoAction.SetViewingText -> updateState { it.copy(viewingText = action.text) }
            is KifuzoAction.ToggleFlipped -> updateState { it.copy(isFlipped = !it.isFlipped) }
            is KifuzoAction.ShowSettings -> updateState { it.copy(showSettings = action.show) }
            is KifuzoAction.ShowImportDialog -> updateState { it.copy(showImportDialog = action.show) }
            is KifuzoAction.ClearErrorAndInfo -> updateState { it.copy(errorMessage = null, infoMessage = null) }
            is KifuzoAction.ImportFiles -> importHandler.importFiles(action.sourceDir, currentRootDirectory)
            is KifuzoAction.RenameFile -> fileActionHandler.renameFile(action.path, uiState.filenameTemplate)
            is KifuzoAction.ConvertCsa -> convertCsa(action.path)
            is KifuzoAction.ConfirmOverwrite -> confirmOverwrite()
            is KifuzoAction.HideOverwriteConfirm -> updateState { it.copy(showOverwriteConfirm = null) }
            is KifuzoAction.DetectAndWriteSenkei -> fileActionHandler.detectAndWriteSenkei(action.path)
            is KifuzoAction.WriteGameResult -> fileActionHandler.writeGameResult(action.path, action.result)
            is KifuzoAction.ToggleSidebar -> updateState { it.copy(isSidebarVisible = !it.isSidebarVisible) }
            is KifuzoAction.SetViewMode -> {
                updateState { it.copy(viewMode = action.mode) }
                refreshFiles()
            }
            is KifuzoAction.ToggleFileFilter -> {
                updateState {
                    val newFilters = if (it.fileFilters.contains(action.filter)) it.fileFilters - action.filter else it.fileFilters + action.filter
                    it.copy(fileFilters = newFilters)
                }
                refreshFiles()
            }
            is KifuzoAction.UpdateSidebarWidth -> updateSidebarWidth(action.delta)
            is KifuzoAction.RefreshFiles -> refreshFiles()
            is KifuzoAction.SelectNextFile -> selectAdjacentFile(forward = true)
            is KifuzoAction.SelectPrevFile -> selectAdjacentFile(forward = false)
            is KifuzoAction.ChangeStep -> boardState.currentStep = action.step
            is KifuzoAction.NextStep -> boardState.currentStep++
            is KifuzoAction.PrevStep -> boardState.currentStep--
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

        if (mode == FileViewMode.HIERARCHY) {
            val newNodes = fileTreeManager.buildTree(root, uiState.treeNodes, filters)
            updateState { it.copy(treeNodes = newNodes) }
        } else {
            scope.launch {
                updateState { it.copy(isScanning = true) }
                val newNodes = withContext(Dispatchers.IO) {
                    fileTreeManager.buildFlatList(root, filters)
                }
                updateState { it.copy(treeNodes = newNodes, isScanning = false) }
            }
        }

        // 戦型情報のスキャン
        scope.launch {
            updateState { it.copy(isScanning = true) }
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(root, SCAN_DEPTH)
                    .filter { it.isRegularFile() && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
                    .forEach { allKifuFiles.add(it) }
            }
            val infos = withContext(Dispatchers.IO) { repository.getKifuInfos(allKifuFiles) }
            updateState { it.copy(kifuInfos = infos, isScanning = false) }
        }
    }

    fun setRootDirectory(path: Path) {
        currentRootDirectory = path
        AppSettings.lastRootDir = path.toString()
        updateState { it.copy(treeNodes = emptyList()) } // ルート変更時は展開状態をクリア
        refreshFiles()
    }

    private fun toggleDirectory(node: FileTreeNode) {
        val newNodes = fileTreeManager.toggleNode(node, uiState.treeNodes)
        updateState { it.copy(treeNodes = newNodes) }
    }

    private fun convertCsa(path: Path) {
        val targetFile = (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
        if (java.nio.file.Files.exists(targetFile)) {
            updateState { it.copy(showOverwriteConfirm = path) }
        } else {
            fileActionHandler.performCsaConversion(path)
        }
    }

    fun confirmOverwrite() {
        uiState.showOverwriteConfirm?.let {
            fileActionHandler.performCsaConversion(it)
            updateState { it.copy(showOverwriteConfirm = null) }
        }
    }
}
