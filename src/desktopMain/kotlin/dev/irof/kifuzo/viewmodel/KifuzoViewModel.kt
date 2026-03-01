package dev.irof.kifuzo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuDiscoveryService
import dev.irof.kifuzo.logic.service.KifuImportService
import dev.irof.kifuzo.logic.service.KifuImportServiceImpl
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * Kifuzo アプリケーションのメインとなる ViewModel。
 */
class KifuzoViewModel(
    private val repository: KifuRepository = KifuRepositoryImpl(),
    private val settings: AppSettings = AppSettings.Default,
    private val fileTreeManager: FileTreeManager = FileTreeManager(repository),
    private val importService: KifuImportService = KifuImportServiceImpl(),
) {
    private val discoveryService = KifuDiscoveryService(repository, fileTreeManager)
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
        importService = importService,
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

    private val fileTreeActionHandler = FileTreeActionHandler(this, settings, fileTreeManager)
    private val fileEditActionHandler = FileEditActionHandler(this, repository, fileActionHandler)
    private val uiActionHandler = UiActionHandler(this, repository, settings, fileActionHandler, importHandler, settingsHandler)

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
        val handled = fileTreeActionHandler.handle(action) ||
            fileEditActionHandler.handle(action) ||
            uiActionHandler.handle(action)
        if (!handled) {
            when (action) {
                is KifuzoAction.FileDrop -> performFileDrop(action.path)
                else -> handleBoardAction(action)
            }
        }
    }

    private fun handleBoardAction(action: KifuzoAction) {
        when (action) {
            is KifuzoAction.ChangeStep -> boardState.currentStep = action.step
            is KifuzoAction.NextStep -> boardState.currentStep++
            is KifuzoAction.PrevStep -> boardState.currentStep--
            is KifuzoAction.SelectVariation -> boardState.switchHistory(action.moves)
            is KifuzoAction.ResetToMainHistory -> boardState.resetToMainHistory()
            else -> {}
        }
    }

    internal fun updateRootDirectory(path: Path) {
        currentRootDirectory = path
        settings.lastRootDir = path.toString()
        updateUiState { it.copy(treeNodes = emptyList()) }
        refreshFiles()
    }

    internal fun updateUiState(transform: (KifuzoUiState) -> KifuzoUiState) {
        uiState = transform(uiState)
    }

    internal fun performPasteKifu() {
        val text = dev.irof.kifuzo.utils.getFromClipboard()
        if (text.isNullOrBlank()) {
            updateUiState { it.copy(errorMessage = "クリップボードが空です。") }
            return
        }
        parseAndSetKifu(text)
    }

    internal fun performFileDrop(path: Path) {
        try {
            val text = java.nio.file.Files.readString(path)
            parseAndSetKifu(text)
        } catch (e: java.io.IOException) {
            updateUiState { it.copy(errorMessage = "ファイルの読み込みに失敗しました。", errorDetail = e.message) }
        }
    }

    private fun parseAndSetKifu(text: String) {
        try {
            repository.parseManually(text.lines(), boardState)
            updateUiState { it.copy(pastedKifuText = text, selectedFile = null) }
            settingsHandler.updateAutoFlip(uiState.myNameRegex)
        } catch (e: dev.irof.kifuzo.logic.parser.KifuParseException) {
            updateUiState { it.copy(errorMessage = "棋譜の解析に失敗しました。", errorDetail = e.message) }
        }
    }

    fun refreshFiles() {
        val root = currentRootDirectory ?: return
        scope.launch {
            updateUiState { it.copy(isScanning = true) }
            try {
                val newNodes = withContext(Dispatchers.IO) {
                    discoveryService.buildFileList(root, uiState.viewMode, uiState.treeNodes, uiState.fileFilters, uiState.fileSortOption)
                }
                val infos = withContext(Dispatchers.IO) {
                    discoveryService.scanVisibleKifuInfos(root, uiState.viewMode, newNodes)
                }
                updateUiState { it.copy(treeNodes = newNodes, kifuInfos = infos, isScanning = false) }
            } catch (e: dev.irof.kifuzo.logic.parser.TooManyErrorsException) {
                logger.error(e) { "Too many permission errors" }
                updateUiState { it.copy(errorMessage = "アクセス拒否が多発したため中断しました。", errorDetail = e.message, isScanning = false) }
            } catch (e: java.io.IOException) {
                logger.error(e) { "Failed to refresh files" }
                updateUiState { it.copy(errorMessage = "ファイルの取得に失敗しました。", errorDetail = e.stackTraceToString(), isScanning = false) }
            }
        }
    }
}
