package dev.irof.kfv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kfv.logic.*
import dev.irof.kfv.models.*
import kotlinx.coroutines.*
import java.io.File

class KifuManagerViewModel(
    private val repository: KifuRepository = KifuRepository()
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var currentDirectory by mutableStateOf(if (AppConfig.KIFU_ROOT.exists() && AppConfig.KIFU_ROOT.isDirectory) AppConfig.KIFU_ROOT else File(AppConfig.USER_HOME))
    var directoryContents by mutableStateOf(listOf<File>())
    var kifuInfos by mutableStateOf(mapOf<File, KifuInfo>())
    var isScanning by mutableStateOf(false)
    var selectedSenkei by mutableStateOf<String?>(null)
    
    var selectedFile by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var infoMessage by mutableStateOf<String?>(null)
    var showOverwriteConfirm by mutableStateOf<File?>(null)
    var viewingText by mutableStateOf<String?>(null)
    var isFlipped by mutableStateOf(false)
    
    var showSettings by mutableStateOf(false)
    var myNameRegex by mutableStateOf(AppSettings.myNameRegex)

    val boardState = ShogiBoardState()

    val filteredContents: List<File>
        get() = if (selectedSenkei == null) directoryContents
        else directoryContents.filter { file -> file.isDirectory || kifuInfos[file]?.senkei == selectedSenkei }

    val availableSenkei: List<String>
        get() = kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()

    fun refreshFiles() {
        directoryContents = repository.scanDirectory(currentDirectory)
        
        scope.launch {
            isScanning = true
            kifuInfos = withContext(Dispatchers.IO) {
                repository.getKifuInfos(directoryContents)
            }
            isScanning = false
        }
    }

    fun selectFile(file: File) {
        selectedFile = file
        val ext = file.extension.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                repository.parse(file, boardState)
                updateAutoFlip()
            } catch (e: Exception) {
                errorMessage = "解析中断: ${file.name}\n\n${e.message}"
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

    fun importFiles() {
        val count = repository.importQuestFiles()
        if (count > 0) {
            infoMessage = "${count}件の棋譜をインポートしました。"
            refreshFiles()
        } else {
            infoMessage = "Downloadsフォルダに該当する棋譜が見つかりませんでした。"
        }
    }

    fun convertCsa(file: File) {
        val targetFile = File(file.parent, file.nameWithoutExtension + ".kifu")
        if (targetFile.exists()) {
            showOverwriteConfirm = file
        } else {
            performCsaConversion(file)
        }
    }
    
    fun confirmOverwrite() {
        showOverwriteConfirm?.let {
            performCsaConversion(it)
            showOverwriteConfirm = null
        }
    }

    private fun performCsaConversion(file: File) {
        val targetFile = repository.convertCsa(file)
        try {
            val tempState = ShogiBoardState()
            repository.parse(targetFile, tempState)
            val senkei = detectSenkei(tempState.history)
            if (senkei.isNotEmpty()) repository.updateSenkei(targetFile, senkei)
        } catch (e: Exception) {}
        refreshFiles()
    }

    fun detectAndWriteSenkei(file: File) {
        val senkei = detectSenkei(boardState.history)
        if (senkei.isNotEmpty()) {
            repository.updateSenkei(file, senkei)
            refreshFiles()
            infoMessage = "戦型を「$senkei」として追記しました。"
        }
    }
}
