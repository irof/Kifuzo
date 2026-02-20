package dev.irof.kfv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kfv.logic.*
import dev.irof.kfv.models.AppConfig
import dev.irof.kfv.models.AppSettings
import dev.irof.kfv.models.ShogiBoardState
import java.io.File

class KifuManagerViewModel {
    var currentDirectory by mutableStateOf(if (AppConfig.KIFU_ROOT.exists() && AppConfig.KIFU_ROOT.isDirectory) AppConfig.KIFU_ROOT else File(AppConfig.USER_HOME))
    var directoryContents by mutableStateOf(listOf<File>())
    var kifuInfos by mutableStateOf(mapOf<File, KifuInfo>())
    var selectedSenkei by mutableStateOf<String?>(null)
    
    var selectedFile by mutableStateOf<File?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var infoMessage by mutableStateOf<String?>(null)
    var showOverwriteConfirm by mutableStateOf<File?>(null)
    var viewingText by mutableStateOf<String?>(null)
    var isFlipped by mutableStateOf(false)
    
    // 設定関連
    var showSettings by mutableStateOf(false)
    var myNameRegex by mutableStateOf(AppSettings.myNameRegex)

    val boardState = ShogiBoardState()

    val filteredContents: List<File>
        get() = if (selectedSenkei == null) directoryContents
        else directoryContents.filter { file -> file.isDirectory || kifuInfos[file]?.senkei == selectedSenkei }

    val availableSenkei: List<String>
        get() = kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()

    fun refreshFiles() {
        val contents = currentDirectory.listFiles { file ->
            file.isDirectory || (file.isFile && file.extension.lowercase() in listOf("kif", "kifu", "kifz", "csa", "jkf", "txt"))
        }?.toList() ?: emptyList()
        directoryContents = contents.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        
        val newInfos = mutableMapOf<File, KifuInfo>()
        contents.filter { it.isFile && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }.forEach { file ->
            newInfos[file] = scanKifuInfo(file)
        }
        kifuInfos = newInfos
    }

    fun selectFile(file: File) {
        selectedFile = file
        val ext = file.extension.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                parseKifu(file, boardState)
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
        val count = importShogiQuestFiles()
        if (count > 0) {
            infoMessage = "${count}件の棋譜をインポートしました。"
            refreshFiles()
        } else {
            infoMessage = "Downloadsフォルダに該当する棋譜が見つかりませんでした。"
        }
    }

    fun convertCsa(file: File) {
        val targetFile = File(file.parent, file.nameWithoutExtension + ".kifu")
        val performConversion = {
            convertCsaToKifu(file)
            try {
                val tempState = ShogiBoardState()
                parseKifu(targetFile, tempState)
                val senkei = detectSenkei(tempState.history)
                if (senkei.isNotEmpty()) updateKifuSenkei(targetFile, senkei)
            } catch (e: Exception) { println("Auto-senkei detection failed: ${e.message}") }
            refreshFiles()
        }
        if (targetFile.exists()) {
            showOverwriteConfirm = file
        } else {
            performConversion()
        }
    }
    
    fun confirmOverwrite() {
        showOverwriteConfirm?.let {
            convertCsaToKifu(it)
            val targetFile = File(it.parent, it.nameWithoutExtension + ".kifu")
            try {
                val tempState = ShogiBoardState()
                parseKifu(targetFile, tempState)
                val senkei = detectSenkei(tempState.history)
                if (senkei.isNotEmpty()) updateKifuSenkei(targetFile, senkei)
            } catch (e: Exception) {}
            
            refreshFiles()
            showOverwriteConfirm = null
        }
    }

    fun detectAndWriteSenkei(file: File) {
        val senkei = detectSenkei(boardState.history)
        if (senkei.isNotEmpty()) {
            updateKifuSenkei(file, senkei)
            refreshFiles()
            infoMessage = "戦型を「$senkei」として追記しました。"
        }
    }
}
