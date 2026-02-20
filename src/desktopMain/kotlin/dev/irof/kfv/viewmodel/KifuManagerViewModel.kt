package dev.irof.kfv.viewmodel

import androidx.compose.runtime.*
import dev.irof.kfv.logic.*
import dev.irof.kfv.models.*
import kotlinx.coroutines.*
import java.nio.file.Path
import kotlin.io.path.*

class KifuManagerViewModel(
    private val repository: KifuRepository = KifuRepository()
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentRootDirectory by mutableStateOf(if (java.nio.file.Files.exists(AppConfig.KIFU_ROOT)) AppConfig.KIFU_ROOT else AppConfig.USER_HOME_PATH)
    
    // 表示用のフラット化されたツリーノードリスト
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

    // フィルタリング後のノードリスト
    val filteredNodes: List<FileTreeNode>
        get() = if (selectedSenkei == null) treeNodes
        else treeNodes.filter { node -> 
            node.isDirectory || kifuInfos[node.path]?.senkei == selectedSenkei 
        }

    val availableSenkei: List<String>
        get() = kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()

    /**
     * ルートディレクトリからツリーを初期化します
     */
    fun refreshFiles() {
        // 初期状態はルート直下のファイルを表示
        val rootFiles = repository.scanDirectory(currentRootDirectory)
        treeNodes = rootFiles.map { FileTreeNode(it, 0, it.isDirectory()) }
        
        // 全棋譜のスキャン（戦型フィルタ用）
        scope.launch {
            isScanning = true
            // サブディレクトリ内も含めて棋譜ファイルを探す
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(currentRootDirectory, 3) // 深さ3まで
                    .filter { it.isRegularFile() && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
                    .forEach { allKifuFiles.add(it) }
            }
            kifuInfos = withContext(Dispatchers.IO) {
                repository.getKifuInfos(allKifuFiles)
            }
            isScanning = false
        }
    }

    /**
     * フォルダの展開/折りたたみを切り替えます
     */
    fun toggleDirectory(node: FileTreeNode) {
        if (!node.isDirectory) return

        val index = treeNodes.indexOf(node)
        if (index == -1) return

        if (node.isExpanded) {
            // 閉じる: そのノードより下のレベルの要素を削除
            val newNodes = treeNodes.toMutableList()
            node.isExpanded = false
            newNodes[index] = node.copy(isExpanded = false)
            
            // 自分より深く、かつ連続している要素を削除
            var i = index + 1
            while (i < newNodes.size && newNodes[i].level > node.level) {
                newNodes.removeAt(i)
            }
            treeNodes = newNodes
        } else {
            // 展開する: 子要素をスキャンして挿入
            val children = repository.scanDirectory(node.path)
            val childNodes = children.map { FileTreeNode(it, node.level + 1, it.isDirectory(), parent = node) }
            
            val newNodes = treeNodes.toMutableList()
            node.isExpanded = true
            newNodes[index] = node.copy(isExpanded = true)
            newNodes.addAll(index + 1, childNodes)
            treeNodes = newNodes
        }
    }

    fun selectFile(path: Path) {
        selectedFile = path
        val ext = path.extension.lowercase()
        boardState.currentStep = 0 // 解析前に手数をリセット
        if (ext == "kifu" || ext == "kif") {
            try {
                repository.parse(path, boardState)
                updateAutoFlip()
            } catch (e: Exception) {
                errorMessage = "解析中断: ${path.name}\n\n${e.message}"
                boardState.history = emptyList()
            }
        } else {
            boardState.history = emptyList()
        }
    }

    fun updateAutoFlip() {
        if (myNameRegex.isEmpty()) return
        val regex = try { Regex(myNameRegex) } catch (e: Exception) { null } ?: return
        if (regex.containsMatchIn(boardState.goteName) && !regex.containsMatchIn(boardState.senteName)) {
            isFlipped = true
        } else if (regex.containsMatchIn(boardState.senteName)) {
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
        if (count > 0) {
            infoMessage = "${count}件の棋譜をインポートしました。"
            refreshFiles()
        } else {
            infoMessage = "指定されたフォルダに該当する棋譜が見つかりませんでした。"
        }
    }

    fun convertCsa(path: Path) {
        val targetFile = path.parent.resolve(path.nameWithoutExtension + ".kifu")
        if (java.nio.file.Files.exists(targetFile)) {
            showOverwriteConfirm = path
        } else {
            performCsaConversion(path)
        }
    }
    
    fun confirmOverwrite() {
        showOverwriteConfirm?.let {
            performCsaConversion(it)
            showOverwriteConfirm = null
        }
    }

    private fun performCsaConversion(path: Path) {
        val targetFile = repository.convertCsa(path)
        try {
            val tempState = ShogiBoardState()
            repository.parse(targetFile, tempState)
            val senkei = detectSenkei(tempState.history)
            if (senkei.isNotEmpty()) repository.updateSenkei(targetFile, senkei)
        } catch (e: Exception) {}
        refreshFiles()
    }

    fun detectAndWriteSenkei(path: Path) {
        val senkei = detectSenkei(boardState.history)
        if (senkei.isNotEmpty()) {
            repository.updateSenkei(path, senkei)
            refreshFiles()
            infoMessage = "戦型を「$senkei」として追記しました。"
        }
    }
}
