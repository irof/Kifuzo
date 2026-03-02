package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

@Composable
fun RenameDialog(
    file: Path,
    proposedName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var textFieldValue by remember {
        val nameWithoutExt = proposedName.substringBeforeLast(".")
        mutableStateOf(
            TextFieldValue(
                text = proposedName,
                selection = TextRange(0, nameWithoutExt.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ファイル名のリネーム", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("現在のファイル名:", style = androidx.compose.material.MaterialTheme.typography.caption)
                Text(file.fileName.toString(), modifier = Modifier.padding(bottom = 16.dp))

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text("新しいファイル名") },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    singleLine = true,
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(ShogiDimensions.Spacing.Medium),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(AppStrings.CANCEL)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onConfirm(textFieldValue.text) },
                    enabled = textFieldValue.text.isNotBlank() && textFieldValue.text != file.fileName.toString(),
                ) {
                    Text(AppStrings.OK)
                }
            }
        },
    )
}
