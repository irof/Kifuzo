package dev.irof.kifuzo

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.ui.components.KifuMenuBar
import dev.irof.kifuzo.ui.components.KifuPreviewActions
import dev.irof.kifuzo.ui.components.KifuPreviewPanel
import dev.irof.kifuzo.ui.components.KifuSidebar
import dev.irof.kifuzo.ui.dialogs.KifuzoDialogs
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoAction
import dev.irof.kifuzo.viewmodel.KifuzoViewModel
import java.awt.Cursor
import java.nio.file.Path

fun main() = application {
    val windowState = rememberKifuzoWindowState()

    Window(
        onCloseRequest = {
            AppSettings.Default.saveWindowState(windowState)
            exitApplication()
        },
        title = AppStrings.APP_TITLE,
        state = windowState,
    ) {
        MaterialTheme {
            KifuzoApp()
        }
    }
}

@Composable
private fun rememberKifuzoWindowState(): androidx.compose.ui.window.WindowState = rememberWindowState(
    position = if (AppSettings.Default.windowX != null && AppSettings.Default.windowY != null) {
        WindowPosition(AppSettings.Default.windowX!!.dp, AppSettings.Default.windowY!!.dp)
    } else {
        WindowPosition.Aligned(Alignment.Center)
    },
    size = androidx.compose.ui.unit.DpSize(AppSettings.Default.windowWidth.dp, AppSettings.Default.windowHeight.dp),
)

@Composable
fun KifuzoApp() {
    val viewModel = remember { KifuzoViewModel() }

    LaunchedEffect(Unit) {
        viewModel.refreshFiles()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        KifuzoAppContent(viewModel)
        KifuzoDialogs(viewModel)
    }
}

@Composable
private fun KifuzoAppContent(viewModel: KifuzoViewModel) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event -> handleKeyEvent(event, viewModel) },
    ) {
        KifuzoMainLayout(viewModel)
    }
}

private fun handleKeyEvent(event: androidx.compose.ui.input.key.KeyEvent, viewModel: KifuzoViewModel): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionRight -> {
            viewModel.dispatch(KifuzoAction.NextStep)
            true
        }
        Key.DirectionLeft -> {
            viewModel.dispatch(KifuzoAction.PrevStep)
            true
        }
        else -> false
    }
}

@Composable
private fun RowScope.KifuzoMainLayout(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState

    KifuMenuBar(
        isSidebarVisible = state.isSidebarVisible,
        onToggleSidebar = { viewModel.dispatch(KifuzoAction.ToggleSidebar) },
        isMoveListVisible = state.isMoveListVisible,
        onToggleMoveList = { viewModel.dispatch(KifuzoAction.ToggleMoveList) },
        onImport = { viewModel.dispatch(KifuzoAction.ShowImportDialog(true)) },
        onPaste = { viewModel.dispatch(KifuzoAction.PasteKifu) },
        onShowSettings = { viewModel.dispatch(KifuzoAction.ShowSettings(true)) },
    )

    if (state.isSidebarVisible) {
        KifuzoSidebarWrapper(viewModel)
        SidebarResizer { viewModel.dispatch(KifuzoAction.UpdateSidebarWidth(it)) }
    }

    val previewActions = remember(viewModel) {
        object : KifuPreviewActions {
            override fun onToggleFlip() = viewModel.dispatch(KifuzoAction.ToggleFlipped)
            override fun onWriteResult(path: Path, result: String) = viewModel.dispatch(KifuzoAction.WriteGameResult(path, result))
            override fun onShowEditMetadata(path: Path) = viewModel.dispatch(KifuzoAction.ShowEditMetadataDialog(path))
            override fun onStepChange(step: Int) = viewModel.dispatch(KifuzoAction.ChangeStep(step))
            override fun onNextStep() = viewModel.dispatch(KifuzoAction.NextStep)
            override fun onPrevStep() = viewModel.dispatch(KifuzoAction.PrevStep)
            override fun onForceParse(path: Path) = viewModel.dispatch(KifuzoAction.ForceParseAsKifu(path))
            override fun onSelectVariation(moves: List<Move>) = viewModel.dispatch(KifuzoAction.SelectVariation(moves))
            override fun onResetToMainHistory() = viewModel.dispatch(KifuzoAction.ResetToMainHistory)
            override fun onOpenExternal(path: Path) = viewModel.dispatch(KifuzoAction.OpenInExternalApp(path))
            override fun onShowSaveDialog() = viewModel.dispatch(KifuzoAction.ShowSavePastedKifuDialog)
        }
    }

    KifuPreviewPanel(
        state = state,
        boardState = viewModel.boardState,
        actions = previewActions,
        modifier = Modifier.weight(1.0f),
    )
}

@Composable
private fun KifuzoSidebarWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
    KifuSidebar(
        state = state,
        currentRoot = viewModel.currentRootDirectory,
        onSetRoot = { viewModel.dispatch(KifuzoAction.SetRootDirectory(it)) },
        onRefresh = { viewModel.dispatch(KifuzoAction.RefreshFiles) },
        onToggleDir = { viewModel.dispatch(KifuzoAction.ToggleDirectory(it)) },
        onSelectFile = { viewModel.dispatch(KifuzoAction.SelectFile(it)) },
        onShowText = { viewModel.dispatch(KifuzoAction.SetViewingText(it)) },
        onRename = { viewModel.dispatch(KifuzoAction.ShowRenameDialog(it)) },
        onConvertCsa = { viewModel.dispatch(KifuzoAction.ConvertCsa(it)) },
        onSetViewMode = { viewModel.dispatch(KifuzoAction.SetViewMode(it)) },
        onSetFileSortOption = { viewModel.dispatch(KifuzoAction.SetFileSortOption(it)) },
        onToggleFileFilter = { viewModel.dispatch(KifuzoAction.ToggleFileFilter(it)) },
        onSelectNext = { viewModel.dispatch(KifuzoAction.SelectNextFile) },
        onSelectPrev = { viewModel.dispatch(KifuzoAction.SelectPrevFile) },
        modifier = Modifier.width(state.sidebarWidth.dp),
    )
}

@Composable
private fun SidebarResizer(onWidthChange: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(4.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onWidthChange(dragAmount.x)
                }
            }
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .background(Color.LightGray.copy(alpha = 0.5f)),
    )
}
