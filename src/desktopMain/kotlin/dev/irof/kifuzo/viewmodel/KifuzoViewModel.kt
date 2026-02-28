package dev.irof.kifuzo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
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

@Suppress("TooManyFunctions")
class KifuzoViewModel(
    private val repository: KifuRepository = KifuRepositoryImpl(),
    private val fileTreeManager: FileTreeManager = FileTreeManager(repository),
) {
    companion object {
        private const val SCAN_DEPTH = 3
        private const val ERROR_DETAIL_PREVIEW_LENGTH = 100
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val boardState = ShogiBoardState()

    private val fileActionHandler = FileActionHandler(
        repository = repository,
        boardState = boardState,
        onError = { msg, detail -> uiState = uiState.copy(errorMessage = msg, errorDetail = detail) },
        onInfo = { msg -> uiState = uiState.copy(infoMessage = msg) },
        onFileRenamed = { path -> uiState = uiState.copy(selectedFile = path) },
        onFilesChanged = { refreshFiles() },
        onAutoFlip = { settingsHandler.updateAutoFlip(uiState.myNameRegex) },
    )

    private val importHandler = ImportHandler(
        repository = repository,
        onInfo = { msg -> uiState = uiState.copy(infoMessage = msg) },
        onImported = {
            uiState = uiState.copy(showImportDialog = false)
            refreshFiles()
        },
    )

    private val settingsHandler = SettingsHandler(
        repository = repository,
        boardState = boardState,
        onFilesChanged = { refreshFiles() },
        onAutoFlip = { flipped -> uiState = uiState.copy(isFlipped = flipped) },
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
        val handled = handleFileTreeAction(action) ||
            handleFileAction(action) ||
            handleUiAction(action)
        if (!handled) {
            when (action) {
                is KifuzoAction.ChangeStep -> boardState.currentStep = action.step
                is KifuzoAction.NextStep -> boardState.currentStep++
                is KifuzoAction.PrevStep -> boardState.currentStep--
                is KifuzoAction.SelectVariation -> boardState.switchHistory(action.moves)
                is KifuzoAction.ResetToMainHistory -> boardState.resetToMainHistory()
                else -> {}
            }
        }
    }

    private fun handleFileTreeAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.SetRootDirectory -> {
                currentRootDirectory = action.path
                AppSettings.lastRootDir = action.path.toString()
                uiState = uiState.copy(treeNodes = emptyList())
                refreshFiles()
            }
            is KifuzoAction.ToggleDirectory -> {
                val newNodes = fileTreeManager.toggleNode(action.node, uiState.treeNodes, uiState.fileSortOption)
                uiState = uiState.copy(treeNodes = newNodes)
            }
            is KifuzoAction.SetViewMode -> {
                uiState = uiState.copy(viewMode = action.mode)
                refreshFiles()
            }
            is KifuzoAction.SetFileSortOption -> {
                AppSettings.fileSortOption = action.option
                uiState = uiState.copy(fileSortOption = action.option)
                refreshFiles()
            }
            is KifuzoAction.ToggleFileFilter -> {
                uiState = uiState.let {
                    val newFilters = if (it.fileFilters.contains(action.filter)) it.fileFilters - action.filter else it.fileFilters + action.filter
                    it.copy(fileFilters = newFilters)
                }
                refreshFiles()
            }
            is KifuzoAction.RefreshFiles -> refreshFiles()
            else -> return false
        }
        return true
    }

    private fun handleFileAction(action: KifuzoAction): Boolean = handleFileSelectAction(action) || handleFileEditAction(action)

    private fun handleFileSelectAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.SelectFile -> {
                fileActionHandler.selectFile(action.path)
                uiState = uiState.copy(selectedFile = action.path)
            }
            is KifuzoAction.SelectNextFile, is KifuzoAction.SelectPrevFile -> {
                val forward = action is KifuzoAction.SelectNextFile
                val nodes = uiState.treeNodes
                val currentIndex = uiState.selectedFile?.let { selected -> nodes.indexOfFirst { it.path == selected } } ?: -1
                if (currentIndex != -1) {
                    val targetIndices = if (forward) (currentIndex + 1 until nodes.size) else (currentIndex - 1 downTo 0)
                    targetIndices.asSequence().map { nodes[it] }.firstOrNull { !it.isDirectory }?.let { node ->
                        fileActionHandler.selectFile(node.path)
                        uiState = uiState.copy(selectedFile = node.path)
                    }
                }
            }
            else -> return false
        }
        return true
    }

    private fun handleFileEditAction(action: KifuzoAction): Boolean = handleRenameAction(action) || handleConversionAction(action) || handleMetadataAction(action)

    private fun handleRenameAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.ShowRenameDialog -> {
                val proposedName = repository.generateProposedName(action.path, uiState.filenameTemplate) ?: action.path.fileName.toString()
                uiState = uiState.copy(renameTarget = action.path, proposedRenameName = proposedName)
            }
            is KifuzoAction.HideRenameDialog -> uiState = uiState.copy(renameTarget = null, proposedRenameName = null)
            is KifuzoAction.PerformRename -> {
                fileActionHandler.performRename(action.path, action.newName)
                uiState = uiState.copy(renameTarget = null, proposedRenameName = null)
            }
            else -> return false
        }
        return true
    }

    private fun handleConversionAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.ConvertCsa -> {
                val targetFile = (action.path.parent ?: action.path).resolve(action.path.nameWithoutExtension + ".kifu")
                if (java.nio.file.Files.exists(targetFile)) {
                    uiState = uiState.copy(showOverwriteConfirm = action.path)
                } else {
                    fileActionHandler.performCsaConversion(action.path)
                }
            }
            is KifuzoAction.ForceParseAsKifu -> fileActionHandler.forceParse(action.path)
            is KifuzoAction.ConfirmOverwrite -> {
                uiState.showOverwriteConfirm?.let {
                    fileActionHandler.performCsaConversion(it)
                    uiState = uiState.copy(showOverwriteConfirm = null)
                }
            }
            is KifuzoAction.HideOverwriteConfirm -> uiState = uiState.copy(showOverwriteConfirm = null)
            else -> return false
        }
        return true
    }

    private fun handleMetadataAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.WriteGameResult -> fileActionHandler.writeGameResult(action.path, action.result)
            is KifuzoAction.UpdateMetadata -> fileActionHandler.updateMetadata(action.path, action.event, action.startTime)
            is KifuzoAction.ShowEditMetadataDialog -> uiState = uiState.copy(editMetadataTarget = action.path)
            is KifuzoAction.HideEditMetadataDialog -> uiState = uiState.copy(editMetadataTarget = null)
            else -> return false
        }
        return true
    }

    private fun handleUiAction(action: KifuzoAction): Boolean = handleConfigAction(action) || handleDisplayAction(action) || handlePasteAction(action)

    private fun handleConfigAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.SaveSettings -> {
                AppSettings.myNameRegex = action.regex
                AppSettings.filenameTemplate = action.template
                settingsHandler.updateAutoFlip(action.regex)
                uiState = uiState.copy(myNameRegex = action.regex, filenameTemplate = action.template, showSettings = false)
            }
            is KifuzoAction.ShowSettings -> uiState = uiState.copy(showSettings = action.show)
            is KifuzoAction.ShowImportDialog -> uiState = uiState.copy(showImportDialog = action.show)
            is KifuzoAction.ImportFiles -> currentRootDirectory?.let { importHandler.performImport(action.sourceDir, it) }
            else -> return false
        }
        return true
    }

    private fun handleDisplayAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.SetViewingText -> uiState = uiState.copy(viewingText = action.text)
            is KifuzoAction.ToggleFlipped -> uiState = uiState.copy(isFlipped = !uiState.isFlipped)
            is KifuzoAction.ClearErrorAndInfo -> uiState = uiState.copy(errorMessage = null, errorDetail = null, infoMessage = null)
            is KifuzoAction.UpdateSidebarWidth -> {
                val newWidth = (uiState.sidebarWidth + action.delta).coerceIn(
                    dev.irof.kifuzo.models.AppConfig.MIN_SIDEBAR_WIDTH,
                    dev.irof.kifuzo.models.AppConfig.MAX_SIDEBAR_WIDTH,
                )
                AppSettings.sidebarWidth = newWidth
                uiState = uiState.copy(sidebarWidth = newWidth)
            }
            is KifuzoAction.ToggleSidebar -> uiState = uiState.copy(isSidebarVisible = !uiState.isSidebarVisible)
            is KifuzoAction.ToggleMoveList -> uiState = uiState.copy(isMoveListVisible = !uiState.isMoveListVisible)
            else -> return false
        }
        return true
    }

    private fun handlePasteAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.PasteKifu -> performPasteKifu()
            is KifuzoAction.HideSavePastedKifuDialog -> uiState = uiState.copy(pastedKifuText = null, pastedKifuProposedName = null)
            is KifuzoAction.SavePastedKifu -> {
                currentRootDirectory?.let {
                    fileActionHandler.savePastedKifu(it, action.filename, action.text)
                } ?: run { uiState = uiState.copy(errorMessage = "保存先ディレクトリが選択されていません。") }
                uiState = uiState.copy(pastedKifuText = null, pastedKifuProposedName = null)
            }
            else -> return false
        }
        return true
    }

    private fun performPasteKifu() {
        val text = dev.irof.kifuzo.utils.getFromClipboard()
        if (text.isNullOrBlank()) {
            uiState = uiState.copy(errorMessage = "クリップボードが空です。")
            return
        }

        try {
            val proposedName = repository.generateProposedNameFromText(text, uiState.filenameTemplate)
            if (proposedName == null) {
                uiState = uiState.copy(errorMessage = "棋譜として認識できませんでした。", errorDetail = text.take(ERROR_DETAIL_PREVIEW_LENGTH))
            } else {
                uiState = uiState.copy(pastedKifuText = text, pastedKifuProposedName = proposedName)
            }
        } catch (cause: Exception) {
            uiState = uiState.copy(errorMessage = "棋譜の解析に失敗しました。", errorDetail = formatThrowable(cause))
        }
    }

    @Suppress("PrintStackTrace")
    private fun formatThrowable(t: Throwable): String {
        val sw = java.io.StringWriter()
        t.printStackTrace(java.io.PrintWriter(sw))
        return "$sw"
    }

    fun refreshFiles() {
        val root = currentRootDirectory ?: return
        val mode = uiState.viewMode
        val filters = uiState.fileFilters
        val sortOption = uiState.fileSortOption

        if (mode == FileViewMode.HIERARCHY) {
            val newNodes = fileTreeManager.buildTree(root, uiState.treeNodes, filters, sortOption)
            uiState = uiState.copy(treeNodes = newNodes)
        } else {
            scope.launch {
                uiState = uiState.copy(isScanning = true)
                val newNodes = withContext(Dispatchers.IO) {
                    fileTreeManager.buildFlatList(root, filters, sortOption)
                }
                uiState = uiState.copy(treeNodes = newNodes, isScanning = false)
            }
        }

        // 棋譜情報のスキャン（対局者名、開始日時など）
        scope.launch {
            uiState = uiState.copy(isScanning = true)
            val allKifuFiles = mutableListOf<Path>()
            withContext(Dispatchers.IO) {
                java.nio.file.Files.walk(root, SCAN_DEPTH)
                    .filter { it.isRegularFile() && (it.extension.lowercase() in listOf("kifu", "kif", "csa")) }
                    .forEach { allKifuFiles.add(it) }
            }
            val infos = withContext(Dispatchers.IO) { repository.getKifuInfos(allKifuFiles) }
            uiState = uiState.copy(kifuInfos = infos, isScanning = false)
        }
    }
}
