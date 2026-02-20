package dev.irof.kfv.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.irof.kfv.utils.AppStrings
import java.io.File

@Composable
fun OverwriteConfirmDialog(
    file: File,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val targetFileName = file.nameWithoutExtension + ".kifu"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.OVERWRITE_CONFIRM) },
        text = { Text("$targetFileName は既に存在します。上書きしますか？") },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(AppStrings.CANCEL) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onConfirm) { Text(AppStrings.OVERWRITE_ACTION) }
            }
        },
    )
}
