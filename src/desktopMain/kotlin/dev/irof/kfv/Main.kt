package dev.irof.kfv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.irof.kfv.models.AppSettings
import dev.irof.kfv.ui.components.KifuPreviewPanel
import dev.irof.kfv.ui.components.KifuSidebar
import dev.irof.kfv.ui.dialogs.KifuTextViewer
import dev.irof.kfv.ui.dialogs.OverwriteConfirmDialog
import dev.irof.kfv.ui.dialogs.SettingsDialog
import dev.irof.kfv.utils.AppStrings
import dev.irof.kfv.utils.copyToClipboard
import dev.irof.kfv.viewmodel.KifuManagerViewModel
import javax.swing.JFileChooser
import kotlin.io.path.toPath

fun main() = application {
    val windowState = rememberWindowState(
        position = if (AppSettings.windowX != null && AppSettings.windowY != null) {
            WindowPosition(AppSettings.windowX!!.dp, AppSettings.windowY!!.dp)
        } else {
            WindowPosition.Aligned(Alignment.Center)
        },
        size = DpSize(AppSettings.windowWidth.dp, AppSettings.windowHeight.dp)
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
        state = windowState
    ) {
        MaterialTheme {
            KifuManagerApp()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KifuManagerApp() {
    val viewModel = remember { KifuManagerViewModel() }
    val state = viewModel.uiState
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

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
                            Key.DirectionRight -> { viewModel.boardState.currentStep++; true }
                            Key.DirectionLeft -> { viewModel.boardState.currentStep--; true }
                            else -> false
                        }
                    } else false
                }
        ) {
            KifuSidebar(
                state = state,
                currentRoot = viewModel.currentRootDirectory,
                onSetRoot = { viewModel.setRootDirectory(it) },
                onImport = {
                    val savedDir = AppSettings.importSourceDir
                    val chooser = JFileChooser().apply { 
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        if (savedDir.isNotEmpty()) {
                            val f = java.io.File(savedDir)
                            if (f.exists()) currentDirectory = f
                        }
                    }
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        viewModel.importFiles(chooser.selectedFile.toPath())
                    }
                },
                onShowSettings = { viewModel.showSettings(true) },
                onSelectSenkei = { viewModel.setSelectedSenkei(it) },
                onToggleDir = { viewModel.toggleDirectory(it) },
                onSelectFile = { viewModel.selectFile(it) },
                onShowText = { viewModel.setViewingText(it) },
                modifier = Modifier.weight(0.4f)
            )

            KifuPreviewPanel(
                state = state,
                boardState = viewModel.boardState,
                onToggleFlip = { viewModel.toggleFlipped() },
                onDetectSenkei = { viewModel.detectAndWriteSenkei(it) },
                onConvertCsa = { viewModel.convertCsa(it) },
                onStepChange = { viewModel.boardState.currentStep = it },
                modifier = Modifier.weight(0.6f)
            )
        }

        // ダイアログ類
        if (state.errorMessage != null || state.infoMessage != null) {
            val title = if (state.errorMessage != null) AppStrings.ERROR else AppStrings.NOTIFICATION
            val msg = state.errorMessage ?: state.infoMessage!!
            AlertDialog(
                onDismissRequest = { viewModel.clearErrorAndInfo() },
                title = { Text(title) },
                text = { Text(msg) },
                buttons = {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                        Button(onClick = { viewModel.clearErrorAndInfo() }) { Text(AppStrings.OK) }
                    }
                }
            )
        }

        if (state.showOverwriteConfirm != null) {
            OverwriteConfirmDialog(
                file = state.showOverwriteConfirm.toFile(),
                onDismiss = { viewModel.hideOverwriteConfirm() },
                onConfirm = { viewModel.confirmOverwrite() }
            )
        }

        if (state.showSettings) {
            SettingsDialog(
                initialRegex = state.myNameRegex,
                onDismiss = { viewModel.showSettings(false) },
                onSave = { viewModel.saveSettings(it) }
            )
        }

        if (state.viewingText != null) {
            KifuTextViewer(
                text = state.viewingText,
                onDismiss = { viewModel.setViewingText(null) },
                onCopy = { copyToClipboard(state.viewingText); viewModel.setViewingText(null) }
            )
        }
    }
}
