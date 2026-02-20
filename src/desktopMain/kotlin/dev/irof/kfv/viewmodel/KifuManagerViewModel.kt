package dev.irof.kfv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kfv.logic.*
import dev.irof.kfv.models.*
import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

class KifuManagerViewModel(
    private val repository: KifuRepository = KifuRepositoryImpl(),
) {
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
            is KifuManagerAction.ShowImportDialog -> updateState { it.copy(showImportDialog = action.show) }
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

    /**
     * 現在の展開状態を維持したまま、ファイル一覧を更新します。
     */
    fun refreshFiles() {
        val root = currentRootDirectory ?: return
        val newNodes = fileTreeManager.buildTree(root, uiState.treeNodes)
        updateState { it.copy(treeNodes = newNodes) }

        // 戦型情報のスキャン（サブディレクトリも含めて最新にする）
        scope.launch {
            updateState { it.copy(isScanning = true) }
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(root, 3)
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
        updateState { it.copy(selectedSenkei = null, treeNodes = emptyList()) } // ルート変更時は展開状態をクリア
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

    fun saveSettings(newRegex: String) {
        AppSettings.myNameRegex = newRegex
        updateState { it.copy(myNameRegex = newRegex, showSettings = false) }
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

    private fun convertCsa(path: Path) {
        val targetFile = path.parent.resolve(path.nameWithoutExtension + ".kifu")
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
}
