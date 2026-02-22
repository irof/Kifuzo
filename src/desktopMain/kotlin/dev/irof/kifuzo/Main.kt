package dev.irof.kifuzo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import dev.irof.kifuzo.ui.components.KifuMenuBar
import dev.irof.kifuzo.ui.components.KifuPreviewPanel
import dev.irof.kifuzo.ui.components.KifuSidebar
import dev.irof.kifuzo.ui.dialogs.ImportDialog
import dev.irof.kifuzo.ui.dialogs.KifuTextViewer
import dev.irof.kifuzo.ui.dialogs.OverwriteConfirmDialog
import dev.irof.kifuzo.ui.dialogs.SettingsDialog
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.utils.copyToClipboard
import dev.irof.kifuzo.viewmodel.KifuzoAction
import dev.irof.kifuzo.viewmodel.KifuzoViewModel
import java.awt.Cursor

fun main() = application {
    val windowState = rememberWindowState(
        position = if (AppSettings.windowX != null && AppSettings.windowY != null) {
            WindowPosition(AppSettings.windowX!!.dp, AppSettings.windowY!!.dp)
        } else {
            WindowPosition.Aligned(Alignment.Center)
        },
        size = DpSize(AppSettings.windowWidth.dp, AppSettings.windowHeight.dp),
    )

    Window(
        onCloseRequest = {
            AppSettings.windowX = windowState.position.let { if (it is WindowPosition.Absolute) it.x.value else null }
            AppSettings.windowY = windowState.position.let { if (it is WindowPosition.Absolute) it.y.value else null }
            AppSettings.windowWidth = windowState.size.width.value
            AppSettings.windowHeight = windowState.size.height.value
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KifuzoApp() {
    val viewModel = remember { KifuzoViewModel() }
    val state = viewModel.uiState
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.refreshFiles()
        focusRequester.requestFocus()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionRight -> {
                                viewModel.dispatch(KifuzoAction.NextStep)
                                true
                            }
                            Key.DirectionLeft -> {
                                viewModel.dispatch(KifuzoAction.PrevStep)
                                true
                            }
                            Key.DirectionDown -> {
                                viewModel.dispatch(KifuzoAction.SelectNextFile)
                                true
                            }
                            Key.DirectionUp -> {
                                viewModel.dispatch(KifuzoAction.SelectPrevFile)
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
        ) {
            KifuMenuBar(
                isSidebarVisible = state.isSidebarVisible,
                onToggleSidebar = { viewModel.dispatch(KifuzoAction.ToggleSidebar) },
                onImport = { viewModel.dispatch(KifuzoAction.ShowImportDialog(true)) },
                onShowSettings = { viewModel.dispatch(KifuzoAction.ShowSettings(true)) },
            )

            if (state.isSidebarVisible) {
                KifuSidebar(
                    state = state,
                    currentRoot = viewModel.currentRootDirectory,
                    onSetRoot = { viewModel.dispatch(KifuzoAction.SetRootDirectory(it)) },
                    onToggleDir = { viewModel.dispatch(KifuzoAction.ToggleDirectory(it)) },
                    onSelectFile = { viewModel.dispatch(KifuzoAction.SelectFile(it)) },
                    onShowText = { viewModel.dispatch(KifuzoAction.SetViewingText(it)) },
                    onSetViewMode = { viewModel.dispatch(KifuzoAction.SetViewMode(it)) },
                    onToggleFileFilter = { viewModel.dispatch(KifuzoAction.ToggleFileFilter(it)) },
                    modifier = Modifier.width(state.sidebarWidth.dp),
                )

                // ドラッグ可能な境界線
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.dispatch(KifuzoAction.UpdateSidebarWidth(dragAmount.x))
                            }
                        }
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .background(Color.LightGray.copy(alpha = 0.5f)),
                )
            }

            KifuPreviewPanel(
                state = state,
                boardState = viewModel.boardState,
                onToggleFlip = { viewModel.dispatch(KifuzoAction.ToggleFlipped) },
                onDetectSenkei = { viewModel.dispatch(KifuzoAction.DetectAndWriteSenkei(it)) },
                onConvertCsa = { viewModel.dispatch(KifuzoAction.ConvertCsa(it)) },
                onStepChange = { viewModel.dispatch(KifuzoAction.ChangeStep(it)) },
                modifier = Modifier.weight(1.0f),
            )
        }

        // --- オーバーレイ・ダイアログ類 ---

        if (state.errorMessage != null || state.infoMessage != null) {
            val title = if (state.errorMessage != null) AppStrings.ERROR else AppStrings.NOTIFICATION
            val msg = state.errorMessage ?: state.infoMessage!!
            AlertDialog(
                onDismissRequest = { viewModel.dispatch(KifuzoAction.ClearErrorAndInfo) },
                title = { Text(title) },
                text = { Text(msg) },
                buttons = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(ShogiDimensions.PaddingMedium),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.errorMessage != null) {
                            androidx.compose.material.OutlinedButton(
                                onClick = { copyToClipboard(msg) },
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Text(AppStrings.COPY)
                            }
                        }
                        Button(onClick = { viewModel.dispatch(KifuzoAction.ClearErrorAndInfo) }) {
                            Text(AppStrings.OK)
                        }
                    }
                },
            )
        }

        if (state.showOverwriteConfirm != null) {
            OverwriteConfirmDialog(
                file = state.showOverwriteConfirm.toFile(),
                onDismiss = { viewModel.dispatch(KifuzoAction.HideOverwriteConfirm) },
                onConfirm = { viewModel.dispatch(KifuzoAction.ConfirmOverwrite) },
            )
        }

        if (state.showSettings) {
            SettingsDialog(
                initialRegex = state.myNameRegex,
                onDismiss = { viewModel.dispatch(KifuzoAction.ShowSettings(false)) },
                onSave = { viewModel.dispatch(KifuzoAction.SaveSettings(it)) },
            )
        }

        if (state.showImportDialog) {
            ImportDialog(
                initialSourceDir = AppSettings.importSourceDir,
                onDismiss = { viewModel.dispatch(KifuzoAction.ShowImportDialog(false)) },
                onImport = { viewModel.dispatch(KifuzoAction.ImportFiles(it)) },
            )
        }

        if (state.viewingText != null) {
            KifuTextViewer(
                text = state.viewingText,
                onDismiss = { viewModel.dispatch(KifuzoAction.SetViewingText(null)) },
                onCopy = {
                    copyToClipboard(state.viewingText)
                    viewModel.dispatch(KifuzoAction.SetViewingText(null))
                },
            )
        }
    }
}
