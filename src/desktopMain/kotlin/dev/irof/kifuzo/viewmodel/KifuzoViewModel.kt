package dev.irof.kifuzo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kifuzo.logic.FileTreeManager
import dev.irof.kifuzo.logic.KifuParseException
import dev.irof.kifuzo.logic.KifuRepository
import dev.irof.kifuzo.logic.KifuRepositoryImpl
import dev.irof.kifuzo.logic.detectSenkei
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.models.ShogiBoardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class KifuzoViewModel(
    private val repository: KifuRepository = KifuRepositoryImpl(),
) {
    companion object {
        private const val SCAN_DEPTH = 3
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val fileTreeManager = FileTreeManager(repository)

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

    val boardState = ShogiBoardState()

    fun dispatch(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.SetRootDirectory -> setRootDirectory(action.path)
            is KifuzoAction.ToggleDirectory -> toggleDirectory(action.node)
            is KifuzoAction.SelectFile -> selectFile(action.path)
            is KifuzoAction.SaveSettings -> saveSettings(action.regex, action.template)
            is KifuzoAction.SetViewingText -> updateState { it.copy(viewingText = action.text) }
            is KifuzoAction.ToggleFlipped -> updateState { it.copy(isFlipped = !it.isFlipped) }
            is KifuzoAction.ShowSettings -> updateState { it.copy(showSettings = action.show) }
            is KifuzoAction.ShowImportDialog -> updateState { it.copy(showImportDialog = action.show) }
            is KifuzoAction.ClearErrorAndInfo -> updateState { it.copy(errorMessage = null, infoMessage = null) }
            is KifuzoAction.ImportFiles -> importFiles(action.sourceDir)
            is KifuzoAction.RenameFile -> renameFile(action.path)
            is KifuzoAction.ConvertCsa -> convertCsa(action.path)
            is KifuzoAction.ConfirmOverwrite -> confirmOverwrite()
            is KifuzoAction.HideOverwriteConfirm -> updateState { it.copy(showOverwriteConfirm = null) }
            is KifuzoAction.DetectAndWriteSenkei -> detectAndWriteSenkei(action.path)
            is KifuzoAction.WriteGameResult -> writeGameResult(action.path, action.result)
            is KifuzoAction.ToggleSidebar -> updateState { it.copy(isSidebarVisible = !it.isSidebarVisible) }
            is KifuzoAction.SetViewMode -> {
                updateState { it.copy(viewMode = action.mode) }
                refreshFiles()
            }
            is KifuzoAction.ToggleFileFilter -> {
                updateState {
                    val newFilters = if (it.fileFilters.contains(action.filter)) {
                        it.fileFilters - action.filter
                    } else {
                        it.fileFilters + action.filter
                    }
                    it.copy(fileFilters = newFilters)
                }
                refreshFiles()
            }
            is KifuzoAction.UpdateSidebarWidth -> {
                updateState {
                    val newWidth = (it.sidebarWidth + action.delta).coerceIn(
                        dev.irof.kifuzo.models.AppConfig.MIN_SIDEBAR_WIDTH,
                        dev.irof.kifuzo.models.AppConfig.MAX_SIDEBAR_WIDTH,
                    )
                    AppSettings.sidebarWidth = newWidth
                    it.copy(sidebarWidth = newWidth)
                }
            }
            is KifuzoAction.RefreshFiles -> refreshFiles()
            is KifuzoAction.SelectNextFile -> selectAdjacentFile(forward = true)
            is KifuzoAction.SelectPrevFile -> selectAdjacentFile(forward = false)
            is KifuzoAction.ChangeStep -> boardState.currentStep = action.step
            is KifuzoAction.NextStep -> boardState.currentStep++
            is KifuzoAction.PrevStep -> boardState.currentStep--
        }
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
                selectFile(node.path)
                return
            }
            nextIndex += step
        }
    }

    private fun updateState(update: (KifuzoUiState) -> KifuzoUiState) {
        uiState = update(uiState)
    }

    /**
     * 現在の展開状態を維持したまま、ファイル一覧を更新します。
     */
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

    private fun selectFile(path: Path) {
        updateState { it.copy(selectedFile = path) }
        val ext = path.extension.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                repository.parse(path, boardState)
                updateAutoFlip()
            } catch (e: KifuParseException) {
                updateState { it.copy(errorMessage = "棋譜パースエラー: ${path.name}\n\n${e.message}") }
                boardState.clear()
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "予期せぬエラー: ${path.name}\n\n${e.message}") }
                boardState.clear()
            }
        } else {
            boardState.clear()
        }
    }

    private fun updateAutoFlip() {
        val myRegexStr = uiState.myNameRegex
        if (myRegexStr.isEmpty()) return
        val regex = try {
            Regex(myRegexStr)
        } catch (e: Exception) {
            null
        } ?: return
        if (regex.containsMatchIn(boardState.session.goteName) && !regex.containsMatchIn(boardState.session.senteName)) {
            updateState { it.copy(isFlipped = true) }
        } else if (regex.containsMatchIn(boardState.session.senteName)) {
            updateState { it.copy(isFlipped = false) }
        }
    }

    fun saveSettings(newRegex: String, newTemplate: String) {
        AppSettings.myNameRegex = newRegex
        AppSettings.filenameTemplate = newTemplate
        updateState { it.copy(myNameRegex = newRegex, filenameTemplate = newTemplate, showSettings = false) }
        updateAutoFlip()
    }

    private fun importFiles(sourceDir: Path) {
        val root = currentRootDirectory ?: return
        val count = repository.importQuestFiles(sourceDir, root)
        AppSettings.importSourceDir = sourceDir.toString()
        updateState { it.copy(showImportDialog = false) }
        if (count > 0) {
            updateState { it.copy(infoMessage = "${count}件の棋譜をインポートしました。") }
            refreshFiles()
        } else {
            updateState { it.copy(infoMessage = "指定されたフォルダに該当する棋譜が見つかりませんでした。") }
        }
    }

    private fun renameFile(path: Path) {
        val newPath = repository.renameKifuFile(path, uiState.filenameTemplate)
        if (newPath != null) {
            refreshFiles()
            selectFile(newPath)
        } else {
            updateState { it.copy(errorMessage = "ファイルのリネームに失敗しました。棋譜内に必要な情報が不足しているか、同名のファイルが既に存在する可能性があります。") }
        }
    }

    private fun convertCsa(path: Path) {
        val targetFile = (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
        if (java.nio.file.Files.exists(targetFile)) {
            updateState { it.copy(showOverwriteConfirm = path) }
        } else {
            performCsaConversion(path)
        }
    }

    fun confirmOverwrite() {
        uiState.showOverwriteConfirm?.let {
            performCsaConversion(it)
            updateState { it.copy(showOverwriteConfirm = null) }
        }
    }

    private fun performCsaConversion(path: Path) {
        val targetFile = repository.convertCsa(path)
        try {
            val tempState = ShogiBoardState()
            repository.parse(targetFile, tempState)
            val senkei = detectSenkei(tempState.session.history)
            if (senkei.isNotEmpty()) repository.updateSenkei(targetFile, senkei)
        } catch (e: Exception) {}
        refreshFiles()
    }

    fun detectAndWriteSenkei(path: Path) {
        val senkei = detectSenkei(boardState.session.history)
        if (senkei.isNotEmpty()) {
            repository.updateSenkei(path, senkei)
            refreshFiles()
            updateState { it.copy(infoMessage = "戦型を「$senkei」として追記しました。") }
        }
    }

    fun writeGameResult(path: Path, result: String) {
        repository.updateResult(path, result)
        refreshFiles()
        selectFile(path) // 再読み込み
        updateState { it.copy(infoMessage = "終局結果を「$result」として追記しました。") }
    }
}
