package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.utils.copyToClipboard
import dev.irof.kifuzo.viewmodel.KifuzoAction
import dev.irof.kifuzo.viewmodel.KifuzoViewModel

@Composable
fun KifuzoDialogs(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState

    if (state.errorMessage != null || state.infoMessage != null) {
        MessageDialog(
            errorMessage = state.errorMessage,
            errorDetail = state.errorDetail,
            infoMessage = state.infoMessage,
            onDismiss = { viewModel.dispatch(KifuzoAction.ClearErrorAndInfo) },
        )
    }

    OverwriteConfirmDialogWrapper(viewModel)
    SettingsDialogWrapper(viewModel)
    ImportDialogWrapper(viewModel)
    RenameDialogWrapper(viewModel)
    TextViewerDialogWrapper(viewModel)
    EditMetadataDialogWrapper(viewModel)
}

@Composable
private fun OverwriteConfirmDialogWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
    if (state.showOverwriteConfirm != null) {
        OverwriteConfirmDialog(
            file = state.showOverwriteConfirm.toFile(),
            onDismiss = { viewModel.dispatch(KifuzoAction.HideOverwriteConfirm) },
            onConfirm = { viewModel.dispatch(KifuzoAction.ConfirmOverwrite) },
        )
    }
}

@Composable
private fun SettingsDialogWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
    if (state.showSettings) {
        SettingsDialog(
            initialRegex = state.myNameRegex,
            initialTemplate = state.filenameTemplate,
            onDismiss = { viewModel.dispatch(KifuzoAction.ShowSettings(false)) },
            onSave = { regex, template -> viewModel.dispatch(KifuzoAction.SaveSettings(regex, template)) },
        )
    }
}

@Composable
private fun ImportDialogWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
    if (state.showImportDialog) {
        ImportDialog(
            initialSourceDir = AppSettings.importSourceDir,
            onDismiss = { viewModel.dispatch(KifuzoAction.ShowImportDialog(false)) },
            onImport = { viewModel.dispatch(KifuzoAction.ImportFiles(it)) },
        )
    }
}

@Composable
private fun RenameDialogWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
    if (state.renameTarget != null && state.proposedRenameName != null) {
        RenameDialog(
            file = state.renameTarget,
            proposedName = state.proposedRenameName,
            onDismiss = { viewModel.dispatch(KifuzoAction.HideRenameDialog) },
            onConfirm = { viewModel.dispatch(KifuzoAction.PerformRename(state.renameTarget, it)) },
        )
    }
}

@Composable
private fun TextViewerDialogWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
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

@Composable
private fun EditMetadataDialogWrapper(viewModel: KifuzoViewModel) {
    val state = viewModel.uiState
    if (state.editMetadataTarget != null) {
        EditMetadataDialog(
            path = state.editMetadataTarget,
            initialEvent = viewModel.boardState.session.event,
            initialStartTime = viewModel.boardState.session.startTime,
            onDismiss = { viewModel.dispatch(KifuzoAction.HideEditMetadataDialog) },
            onConfirm = { event, start ->
                viewModel.dispatch(KifuzoAction.UpdateMetadata(state.editMetadataTarget, event, start))
                viewModel.dispatch(KifuzoAction.HideEditMetadataDialog)
            },
        )
    }
}

@Composable
private fun MessageDialog(
    errorMessage: String?,
    errorDetail: String?,
    infoMessage: String?,
    onDismiss: () -> Unit,
) {
    val title = if (errorMessage != null) AppStrings.ERROR else AppStrings.NOTIFICATION
    val msg = errorMessage ?: infoMessage ?: ""
    val copyText = if (errorDetail != null) "$msg\n\n$errorDetail" else msg

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(msg) },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(ShogiDimensions.Spacing.Medium),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (errorMessage != null) {
                    androidx.compose.material.OutlinedButton(
                        onClick = { copyToClipboard(copyText) },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(AppStrings.COPY)
                    }
                }
                Button(onClick = onDismiss) {
                    Text(AppStrings.OK)
                }
            }
        },
    )
}
