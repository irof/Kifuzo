package dev.irof.kfv.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.irof.kfv.utils.AppStrings
import java.nio.file.Path
import javax.swing.JFileChooser

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
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = sourcePath,
                        onValueChange = { sourcePath = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        val chooser = JFileChooser().apply {
                            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            if (sourcePath.isNotEmpty()) {
                                val f = java.io.File(sourcePath)
                                if (f.exists()) {
                                    currentDirectory = if (f.isDirectory) f else f.parentFile
                                }
                            }
                        }
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                            sourcePath = chooser.selectedFile.absolutePath
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "フォルダを選択")
                    }
                }
            }
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(AppStrings.CANCEL) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        try {
                            val path = java.nio.file.Paths.get(sourcePath)
                            onImport(path)
                        } catch (e: Exception) {
                            // 不正なパス入力への簡易的な対応
                        }
                    },
                    enabled = sourcePath.isNotEmpty(),
                ) {
                    Text("インポート実行")
                }
            }
        },
    )
}
