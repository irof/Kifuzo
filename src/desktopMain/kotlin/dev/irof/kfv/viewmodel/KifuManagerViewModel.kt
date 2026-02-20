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
    var treeNodes by mutableStateOf(listOf<FileTreeNode>())
    var kifuInfos by mutableStateOf(mapOf<Path, KifuInfo>())
    var isScanning by mutableStateOf(false)
    var selectedSenkei by mutableStateOf<String?>(null)
    
    var selectedFile by mutableStateOf<Path?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var infoMessage by mutableStateOf<String?>(null)
    var showOverwriteConfirm by mutableStateOf<Path?>(null)
    var viewingText by mutableStateOf<String?>(null)
    var isFlipped by mutableStateOf(false)
    
    var showSettings by mutableStateOf(false)
    var myNameRegex by mutableStateOf(AppSettings.myNameRegex)

    val boardState = ShogiBoardState()

    val filteredNodes: List<FileTreeNode>
        get() = if (selectedSenkei == null) treeNodes
        else treeNodes.filter { node -> node.isDirectory || kifuInfos[node.path]?.senkei == selectedSenkei }

    val availableSenkei: List<String>
        get() = kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()

    fun refreshFiles() {
        val rootFiles = repository.scanDirectory(currentRootDirectory)
        treeNodes = rootFiles.map { FileTreeNode(it, 0, it.isDirectory()) }
        
        scope.launch {
            isScanning = true
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(currentRootDirectory, 3)
                    .filter { it.isRegularFile() && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
                    .forEach { allKifuFiles.add(it) }
            }
            kifuInfos = withContext(Dispatchers.IO) {
                repository.getKifuInfos(allKifuFiles)
            }
            isScanning = false
        }
    }

    fun toggleDirectory(node: FileTreeNode) {
        if (!node.isDirectory) return
        val index = treeNodes.indexOf(node)
        if (index == -1) return
        if (node.isExpanded) {
            val newNodes = treeNodes.toMutableList()
            newNodes[index] = node.copy(isExpanded = false)
            var i = index + 1
            while (i < newNodes.size && newNodes[i].level > node.level) { newNodes.removeAt(i) }
            treeNodes = newNodes
        } else {
            val children = repository.scanDirectory(node.path)
            val childNodes = children.map { FileTreeNode(it, node.level + 1, it.isDirectory(), parent = node) }
            val newNodes = treeNodes.toMutableList()
            newNodes[index] = node.copy(isExpanded = true)
            newNodes.addAll(index + 1, childNodes)
            treeNodes = newNodes
        }
    }

    fun selectFile(path: Path) {
        selectedFile = path
        val ext = path.extension.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                repository.parse(path, boardState)
                updateAutoFlip()
            } catch (e: Exception) {
                errorMessage = "解析中断: ${path.name}\n\n${e.message}"
                boardState.clear()
            }
        } else {
            boardState.clear()
        }
    }

    fun updateAutoFlip() {
        if (myNameRegex.isEmpty()) return
        val regex = try { Regex(myNameRegex) } catch (e: Exception) { null } ?: return
        if (regex.containsMatchIn(boardState.session.goteName) && !regex.containsMatchIn(boardState.session.senteName)) {
            isFlipped = true
        } else if (regex.containsMatchIn(boardState.session.senteName)) {
            isFlipped = false
        }
    }

    fun saveSettings(newRegex: String) {
        myNameRegex = newRegex
        AppSettings.myNameRegex = newRegex
        showSettings = false
        updateAutoFlip()
    }

    fun importFiles(sourceDir: Path) {
        val count = repository.importQuestFiles(sourceDir)
        AppSettings.importSourceDir = sourceDir.toString()
        if (count > 0) { infoMessage = "${count}件の棋譜をインポートしました。"; refreshFiles() }
        else { infoMessage = "指定されたフォルダに該当する棋譜が見つかりませんでした。" }
    }

    fun convertCsa(path: Path) {
        val targetFile = path.parent.resolve(path.nameWithoutExtension + ".kifu")
        if (java.nio.file.Files.exists(targetFile)) { showOverwriteConfirm = path } 
        else { performCsaConversion(path) }
    }
    
    fun confirmOverwrite() {
        showOverwriteConfirm?.let { performCsaConversion(it); showOverwriteConfirm = null }
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
            infoMessage = "戦型を「$senkei」として追記しました。"
        }
    }
}
