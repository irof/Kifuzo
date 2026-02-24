package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun EditMetadataDialog(
    initialEvent: String,
    initialStartTime: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var event by remember { mutableStateOf(initialEvent) }
    var startTime by remember { mutableStateOf(initialStartTime) }

    Dialog(
        onCloseRequest = onDismiss,
        state = rememberDialogState(width = 400.dp, height = 300.dp),
        title = AppStrings.EDIT_METADATA,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = event,
                    onValueChange = { event = it },
                    label = { Text(AppStrings.LABEL_EVENT) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text(AppStrings.LABEL_START_TIME) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

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
