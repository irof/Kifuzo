package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
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
            Row(modifier = Modifier.fillMaxWidth().padding(ShogiDimensions.Spacing.Medium), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(AppStrings.CANCEL) }
                Spacer(Modifier.width(ShogiDimensions.Spacing.Medium))
                Button(onClick = onConfirm) { Text(AppStrings.OVERWRITE_ACTION) }
            }
        },
    )
}
