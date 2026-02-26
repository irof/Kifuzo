package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.InvalidPathException
import java.nio.file.Path
import javax.swing.JFileChooser

private val logger = KotlinLogging.logger {}

@Composable
fun ImportDialog(
    initialSourceDir: String,
    onDismiss: () -> Unit,
    onImport: (Path) -> Unit,
) {
    var sourcePath by remember { mutableStateOf(initialSourceDir) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.IMPORT_KIFU) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("インポート元フォルダ:")
                Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
                ImportSourceField(sourcePath, onPathChange = { sourcePath = it })
            }
        },
        buttons = {
            ImportFooter(sourcePath, onDismiss, onImport)
        },
    )
}

@Composable
private fun ImportSourceField(path: String, onPathChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = path,
            onValueChange = onPathChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        Spacer(Modifier.width(ShogiDimensions.Spacing.Medium))
        IconButton(onClick = {
            val chooser = createDirectoryChooser(path)
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                onPathChange(chooser.selectedFile.absolutePath)
            }
        }) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "フォルダを選択")
        }
    }
}

private fun createDirectoryChooser(initialPath: String): JFileChooser = JFileChooser().apply {
    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    if (initialPath.isNotEmpty()) {
        val f = java.io.File(initialPath)
        if (f.exists()) {
            currentDirectory = if (f.isDirectory) f else f.parentFile
        }
    }
}

@Composable
private fun ImportFooter(sourcePath: String, onDismiss: () -> Unit, onImport: (Path) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(ShogiDimensions.Spacing.Medium), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(AppStrings.CANCEL) }
        Spacer(Modifier.width(ShogiDimensions.Spacing.Medium))
        Button(
            onClick = {
                try {
                    onImport(java.nio.file.Paths.get(sourcePath))
                } catch (e: InvalidPathException) {
                    logger.error(e) { "Invalid path entered: $sourcePath" }
                }
            },
            enabled = sourcePath.isNotEmpty(),
        ) {
            Text("インポート実行")
        }
    }
}
