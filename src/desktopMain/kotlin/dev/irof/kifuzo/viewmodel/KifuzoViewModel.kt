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
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
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
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Suppress("TooManyFunctions")
class KifuzoViewModel(
    private val repository: KifuRepository = KifuRepositoryImpl(),
    private val settings: AppSettings = AppSettings.Default,
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
        settings = settings,
        boardState = boardState,
        onFilesChanged = { refreshFiles() },
        onAutoFlip = { flipped -> uiState = uiState.copy(isFlipped = flipped) },
    )
    var currentRootDirectory by mutableStateOf<Path?>(
        settings.lastRootDir.let {
            val path = if (it.isNotEmpty()) java.nio.file.Paths.get(it) else java.nio.file.Paths.get(System.getProperty("user.home"))
            if (java.nio.file.Files.exists(path)) path else java.nio.file.Paths.get(System.getProperty("user.home"))
        },
    )
        private set

    var uiState by mutableStateOf(
        KifuzoUiState(
            myNameRegex = settings.myNameRegex,
            filenameTemplate = settings.filenameTemplate,
            sidebarWidth = settings.sidebarWidth,
            fileSortOption = settings.fileSortOption,
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

    @Suppress("TooGenericExceptionCaught")
    private fun handleFileTreeAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.SetRootDirectory -> {
                currentRootDirectory = action.path
                settings.lastRootDir = action.path.toString()
                uiState = uiState.copy(treeNodes = emptyList())
                refreshFiles()
            }
            is KifuzoAction.ToggleDirectory -> {
                try {
                    val newNodes = fileTreeManager.toggleNode(action.node, uiState.treeNodes, uiState.fileSortOption)
                    uiState = uiState.copy(treeNodes = newNodes)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to toggle directory: ${action.node.path}" }
                    uiState = uiState.copy(errorMessage = "フォルダの展開に失敗しました。", errorDetail = formatThrowable(e))
                }
            }
            is KifuzoAction.SetViewMode -> {
                uiState = uiState.copy(viewMode = action.mode)
                refreshFiles()
            }
            is KifuzoAction.SetFileSortOption -> {
                settings.fileSortOption = action.option
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
                uiState = uiState.copy(selectedFile = action.path, pastedKifuText = null)
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

    @Suppress("TooGenericExceptionCaught")
    private fun handleRenameAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.ShowRenameDialog -> {
                val info = uiState.kifuInfos[action.path] ?: try {
                    repository.scanKifuInfo(java.nio.file.Files.readAllLines(action.path))
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to scan kifu info for rename dialog: ${action.path}" }
                    dev.irof.kifuzo.models.KifuInfo(action.path)
                }
                val proposedName = repository.generateProposedName(action.path, info, uiState.filenameTemplate) ?: action.path.fileName.toString()
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
            is KifuzoAction.OpenInExternalApp -> fileActionHandler.openInExternalApp(action.path)
            else -> return false
        }
        return true
    }

    private fun handleUiAction(action: KifuzoAction): Boolean = handleConfigAction(action) || handleDisplayAction(action) || handlePasteAction(action)

    private fun handleConfigAction(action: KifuzoAction): Boolean {
        when (action) {
            is KifuzoAction.SaveSettings -> {
                settings.myNameRegex = action.regex
                settings.filenameTemplate = action.template
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
                settings.sidebarWidth = newWidth
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
            is KifuzoAction.ShowSavePastedKifuDialog -> {
                uiState.pastedKifuText?.let { text ->
                    val info = repository.scanKifuInfo(text.lines())
                    val proposedName = repository.generateProposedNameFromText(text, info, uiState.filenameTemplate)
                    uiState = uiState.copy(pastedKifuProposedName = proposedName ?: "pasted_kifu.kifu")
                }
            }
            is KifuzoAction.HideSavePastedKifuDialog -> uiState = uiState.copy(pastedKifuProposedName = null)
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
            // パースして盤面を更新（ファイル保存はしない）
            val lines = text.lines()
            repository.parseManually(lines, boardState)
            uiState = uiState.copy(pastedKifuText = text, selectedFile = null)
            settingsHandler.updateAutoFlip(uiState.myNameRegex)
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

    @Suppress("TooGenericExceptionCaught")
    fun refreshFiles() {
        val root = currentRootDirectory ?: return
        val mode = uiState.viewMode
        val filters = uiState.fileFilters
        val sortOption = uiState.fileSortOption

        if (mode == FileViewMode.HIERARCHY) {
            refreshHierarchy(root, filters, sortOption)
        } else {
            refreshFlatList(root, filters, sortOption)
        }

        scanKifuInfos(root)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun refreshHierarchy(root: Path, filters: Set<FileFilter>, sortOption: FileSortOption) {
        try {
            val newNodes = fileTreeManager.buildTree(root, uiState.treeNodes, filters, sortOption)
            uiState = uiState.copy(treeNodes = newNodes)
        } catch (e: dev.irof.kifuzo.logic.parser.TooManyErrorsException) {
            logger.error(e) { "Too many permission errors during hierarchy build" }
            uiState = uiState.copy(errorMessage = "アクセス拒否が多発したため中断しました。", errorDetail = e.message)
        } catch (e: Exception) {
            logger.error(e) { "Failed to build file tree" }
            uiState = uiState.copy(errorMessage = "フォルダの走査に失敗しました。", errorDetail = formatThrowable(e))
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun refreshFlatList(root: Path, filters: Set<FileFilter>, sortOption: FileSortOption) {
        scope.launch {
            uiState = uiState.copy(isScanning = true)
            try {
                val newNodes = withContext(Dispatchers.IO) {
                    fileTreeManager.buildFlatList(root, filters, sortOption)
                }
                uiState = uiState.copy(treeNodes = newNodes, isScanning = false)
            } catch (e: dev.irof.kifuzo.logic.parser.TooManyErrorsException) {
                logger.error(e) { "Too many permission errors during flat list build" }
                uiState = uiState.copy(errorMessage = "アクセス拒否が多発したため中断しました。", errorDetail = e.message, isScanning = false)
            } catch (e: Exception) {
                logger.error(e) { "Failed to build flat list" }
                uiState = uiState.copy(
                    errorMessage = "ファイルの取得に失敗しました。",
                    errorDetail = formatThrowable(e),
                    isScanning = false,
                )
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun scanKifuInfos(root: Path) {
        // 棋譜情報のスキャン（対局者名、開始日時など）
        // 階層表示の場合は現在見えている（展開済みの）ディレクトリのみ、
        // フラット表示の場合はルート直下のみをスキャン対象とする。
        scope.launch {
            uiState = uiState.copy(isScanning = true)
            try {
                val targetDirs = getScanTargetDirectories(root)
                val allKifuFiles = mutableListOf<Path>()

                withContext(Dispatchers.IO) {
                    targetDirs.forEach { dir ->
                        try {
                            java.nio.file.Files.list(dir).use { stream ->
                                stream.filter { isKifuFile(it) }.forEach { allKifuFiles.add(it) }
                            }
                        } catch (e: Exception) {
                            logger.warn(e) { "Failed to list directory for kifu infos: $dir" }
                        }
                    }
                }
                val infos = withContext(Dispatchers.IO) { repository.getKifuInfos(allKifuFiles) }
                uiState = uiState.copy(kifuInfos = infos, isScanning = false)
            } catch (e: Exception) {
                logger.error(e) { "Failed to scan kifu infos" }
                // 棋譜情報のスキャン失敗は致命的ではないため詳細ログのみ
                uiState = uiState.copy(isScanning = false)
            }
        }
    }

    private fun getScanTargetDirectories(root: Path): List<Path> = if (uiState.viewMode == FileViewMode.HIERARCHY) {
        // ルート + 展開済みのディレクトリ
        listOf(root) + uiState.treeNodes.filter { it.isDirectory && it.isExpanded }.map { it.path }
    } else {
        listOf(root)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun isKifuFile(path: Path): Boolean = try {
        !path.name.startsWith(".") && path.isRegularFile() && (path.extension.lowercase() in listOf("kifu", "kif", "csa"))
    } catch (e: Exception) {
        logger.debug(e) { "Failed to check if kifu file: $path" }
        false
    }
}
