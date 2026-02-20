package dev.irof.kfv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kfv.logic.*
import dev.irof.kfv.models.*
import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class KifuManagerViewModel(
    private val repository: KifuRepository = KifuRepository()
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentRootDirectory by mutableStateOf(if (java.nio.file.Files.exists(AppConfig.KIFU_ROOT)) AppConfig.KIFU_ROOT else AppConfig.USER_HOME_PATH)
        private set

    var uiState by mutableStateOf(KifuManagerUiState(myNameRegex = AppSettings.myNameRegex))
        private set

    val boardState = ShogiBoardState()

    fun dispatch(action: KifuManagerAction) {
        when (action) {
            is KifuManagerAction.SetRootDirectory -> setRootDirectory(action.path)
            is KifuManagerAction.ToggleDirectory -> toggleDirectory(action.node)
            is KifuManagerAction.SelectFile -> selectFile(action.path)
            is KifuManagerAction.SetSelectedSenkei -> updateState { it.copy(selectedSenkei = action.senkei) }
            is KifuManagerAction.SaveSettings -> saveSettings(action.regex)
            is KifuManagerAction.SetViewingText -> updateState { it.copy(viewingText = action.text) }
            is KifuManagerAction.ToggleFlipped -> updateState { it.copy(isFlipped = !it.isFlipped) }
            is KifuManagerAction.ShowSettings -> updateState { it.copy(showSettings = action.show) }
            is KifuManagerAction.ClearErrorAndInfo -> updateState { it.copy(errorMessage = null, infoMessage = null) }
            is KifuManagerAction.ImportFiles -> importFiles(action.sourceDir)
            is KifuManagerAction.ConvertCsa -> convertCsa(action.path)
            is KifuManagerAction.ConfirmOverwrite -> confirmOverwrite()
            is KifuManagerAction.HideOverwriteConfirm -> updateState { it.copy(showOverwriteConfirm = null) }
            is KifuManagerAction.DetectAndWriteSenkei -> detectAndWriteSenkei(action.path)
            is KifuManagerAction.ChangeStep -> boardState.currentStep = action.step
            is KifuManagerAction.NextStep -> boardState.currentStep++
            is KifuManagerAction.PrevStep -> boardState.currentStep--
        }
    }

    private fun updateState(update: (KifuManagerUiState) -> KifuManagerUiState) {
        uiState = update(uiState)
    }

    fun refreshFiles() {
        val rootFiles = repository.scanDirectory(currentRootDirectory)
        updateState { it.copy(treeNodes = rootFiles.map { f -> FileTreeNode(f, 0, f.isDirectory()) }) }
        
        scope.launch {
            updateState { it.copy(isScanning = true) }
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(currentRootDirectory, 3)
                    .filter { it.isRegularFile() && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
                    .forEach { allKifuFiles.add(it) }
            }
            val infos = withContext(Dispatchers.IO) { repository.getKifuInfos(allKifuFiles) }
            updateState { it.copy(kifuInfos = infos, isScanning = false) }
        }
    }

    fun setRootDirectory(path: Path) {
        currentRootDirectory = path
        updateState { it.copy(selectedSenkei = null) }
        refreshFiles()
    }

    private fun toggleDirectory(node: FileTreeNode) {
        if (!node.isDirectory) return
        val nodes = uiState.treeNodes
        val index = nodes.indexOf(node)
        if (index == -1) return

        if (node.isExpanded) {
            val newNodes = nodes.toMutableList()
            newNodes[index] = node.copy(isExpanded = false)
            var i = index + 1
            while (i < newNodes.size && newNodes[i].level > node.level) { newNodes.removeAt(i) }
            updateState { it.copy(treeNodes = newNodes) }
        } else {
            val children = repository.scanDirectory(node.path)
            val childNodes = children.map { FileTreeNode(it, node.level + 1, it.isDirectory(), parent = node) }
            val newNodes = nodes.toMutableList()
            newNodes[index] = node.copy(isExpanded = true)
            newNodes.addAll(index + 1, childNodes)
            updateState { it.copy(treeNodes = newNodes) }
        }
    }

    private fun selectFile(path: Path) {
        updateState { it.copy(selectedFile = path) }
        val ext = path.extension.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                repository.parse(path, boardState)
                updateAutoFlip()
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = "解析中断: ${path.name}\n\n${e.message}") }
                boardState.clear()
            }
        } else {
            boardState.clear()
        }
    }

    private fun updateAutoFlip() {
        val myRegexStr = uiState.myNameRegex
        if (myRegexStr.isEmpty()) return
        val regex = try { Regex(myRegexStr) } catch (e: Exception) { null } ?: return
        if (regex.containsMatchIn(boardState.session.goteName) && !regex.containsMatchIn(boardState.session.senteName)) {
            updateState { it.copy(isFlipped = true) }
        } else if (regex.containsMatchIn(boardState.session.senteName)) {
            updateState { it.copy(isFlipped = false) }
        }
    }

    private fun saveSettings(newRegex: String) {
        AppSettings.myNameRegex = newRegex
        updateState { it.copy(myNameRegex = newRegex, showSettings = false) }
        updateAutoFlip()
    }

    private fun importFiles(sourceDir: Path) {
        val count = repository.importQuestFiles(sourceDir, currentRootDirectory)
        AppSettings.importSourceDir = sourceDir.toString()
        if (count > 0) {
            updateState { it.copy(infoMessage = "${count}件の棋譜をインポートしました。") }
            refreshFiles()
        } else {
            updateState { it.copy(infoMessage = "指定されたフォルダに該当する棋譜が見つかりませんでした。") }
        }
    }

    private fun convertCsa(path: Path) {
        val targetFile = path.parent.resolve(path.nameWithoutExtension + ".kifu")
        if (java.nio.file.Files.exists(targetFile)) {
            updateState { it.copy(showOverwriteConfirm = path) }
        } else {
            performCsaConversion(path)
        }
    }
    
    private fun confirmOverwrite() {
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

    private fun detectAndWriteSenkei(path: Path) {
        val senkei = detectSenkei(boardState.session.history)
        if (senkei.isNotEmpty()) {
            repository.updateSenkei(path, senkei)
            refreshFiles()
            updateState { it.copy(infoMessage = "戦型を「$senkei」として追記しました。") }
        }
    }
}
