import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.File
import javax.swing.JFileChooser

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "棋譜管理アプリ") {
        MaterialTheme {
            KifuManagerApp()
        }
    }
}

@Composable
fun KifuManagerApp() {
    var selectedDirectory by remember { mutableStateOf<File?>(null) }
    var kifuFiles by remember { mutableStateOf(listOf<File>()) }

    // フォルダが選択されたらファイルを更新
    LaunchedEffect(selectedDirectory) {
        kifuFiles = selectedDirectory?.listFiles { file ->
            val ext = file.extension.lowercase()
            file.isFile && ext in listOf("kif", "kifz", "csa", "jkf")
        }?.toList() ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "棋譜フォルダを選択"
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedDirectory = chooser.selectedFile
                }
            }) {
                Text("フォルダを選択")
            }
            Spacer(Modifier.width(8.dp))
            Text(selectedDirectory?.absolutePath ?: "フォルダが選択されていません")
        }

        Spacer(Modifier.height(16.dp))

        Text("棋譜一覧 (${kifuFiles.size}件)", style = MaterialTheme.typography.h6)
        
        Divider(Modifier.padding(vertical = 8.dp))

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(kifuFiles) { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(file.name, style = MaterialTheme.typography.subtitle1)
                            Text(file.absolutePath, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}
