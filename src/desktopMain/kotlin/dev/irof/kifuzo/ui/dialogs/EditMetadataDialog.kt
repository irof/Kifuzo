package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EditMetadataDialog(
    path: Path,
    initialEvent: String,
    initialStartTime: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var event by remember { mutableStateOf(initialEvent) }
    var startTime by remember { mutableStateOf(initialStartTime) }

    Dialog(
        onCloseRequest = onDismiss,
        state = rememberDialogState(width = 440.dp, height = 400.dp),
        title = AppStrings.EDIT_METADATA,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 棋戦
                Column {
                    OutlinedTextField(
                        value = event,
                        onValueChange = { event = it },
                        label = { Text(AppStrings.LABEL_EVENT) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistanceButton(AppStrings.LABEL_QUEST) { event = AppStrings.LABEL_QUEST }
                        AssistanceButton(AppStrings.LABEL_WARS) { event = AppStrings.LABEL_WARS }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 開始日時
                Column {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text(AppStrings.LABEL_START_TIME) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    AssistanceButton(AppStrings.LABEL_FILL_TIMESTAMP) {
                        try {
                            val lastModified = Files.getLastModifiedTime(path).toInstant()
                            val date = lastModified.atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                            startTime = date
                        } catch (@Suppress("SwallowedException") e: Exception) {
                            // Ignore error
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(AppStrings.CANCEL)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(event, startTime) }) {
                        Text(AppStrings.OK)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistanceButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
    ) {
        Text(text, fontSize = ShogiDimensions.FontSizeCaption)
    }
}
