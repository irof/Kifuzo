import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import logic.convertCsaToKifu
import logic.getInitialCells
import logic.importShogiQuestFiles
import logic.parseKifu
import models.AppConfig
import models.ShogiBoardState
import ui.FileEntryItem
import ui.ShogiBoardView
import utils.copyToClipboard
import java.io.File
import javax.swing.JFileChooser
import kotlin.math.max
import kotlin.math.min

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "棋譜管理アプリ",
        state = WindowState(size = DpSize(800.dp, 750.dp))
    ) {
        MaterialTheme {
            KifuManagerApp()
        }
    }
}

@Composable
fun KifuManagerApp() {
    // 状態管理
    var currentDirectory by remember { 
        mutableStateOf(if (AppConfig.KIFU_ROOT.exists() && AppConfig.KIFU_ROOT.isDirectory) AppConfig.KIFU_ROOT else File(AppConfig.USER_HOME)) 
    }
    var directoryContents by remember { mutableStateOf(listOf<File>()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showOverwriteConfirm by remember { mutableStateOf<File?>(null) }
    val boardState = remember { ShogiBoardState() }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    // 手数を進める/戻す関数
    fun nextStep() {
        if (boardState.currentStep < boardState.history.size - 1) {
            boardState.currentStep++
        }
    }
    fun prevStep() {
        if (boardState.currentStep > 0) {
            boardState.currentStep--
        }
    }

    // フォーカスの要求
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // ディレクトリ内容の更新
    fun refreshFiles() {
        val contents = currentDirectory.listFiles { file ->
            file.isDirectory || (file.isFile && file.extension.lowercase() in listOf("kif", "kifu", "kifz", "csa", "jkf", "txt"))
        }?.toList() ?: emptyList()
        directoryContents = contents.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    // ディレクトリ変更時のリフレッシュ
    LaunchedEffect(currentDirectory) {
        refreshFiles()
    }
    
    // ファイル選択時の解析
    LaunchedEffect(selectedFile) {
        val ext = selectedFile?.extension?.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try {
                parseKifu(selectedFile!!, boardState)
            } catch (e: Exception) {
                errorMessage = "解析中断: ${selectedFile?.name}\n\n${e.message}"
                boardState.history = emptyList()
            }
        } else {
            boardState.history = emptyList()
        }
    }

    // エラーダイアログ
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("解析エラー") },
            text = { Text(errorMessage!!, fontSize = 12.sp) },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = { 
                        copyToClipboard(errorMessage!!)
                        errorMessage = null 
                    }) {
                        Text("コピーして閉じる")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { errorMessage = null }) {
                        Text("閉じる")
                    }
                }
            }
        )
    }

    // 情報ダイアログ（インポート完了通知など）
    if (infoMessage != null) {
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text("通知") },
            text = { Text(infoMessage!!) },
            buttons = {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                    Button(onClick = { infoMessage = null }) { Text("OK") }
                }
            }
        )
    }

    // 上書き確認ダイアログ
    if (showOverwriteConfirm != null) {
        val targetFile = File(showOverwriteConfirm!!.parent, showOverwriteConfirm!!.nameWithoutExtension + ".kifu")
        AlertDialog(
            onDismissRequest = { showOverwriteConfirm = null },
            title = { Text("上書き確認") },
            text = { Text("${targetFile.name} は既に存在します。上書きしますか？") },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showOverwriteConfirm = null }) { Text("キャンセル") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        convertCsaToKifu(showOverwriteConfirm!!)
                        refreshFiles()
                        showOverwriteConfirm = null
                    }) { Text("上書きする") }
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionRight -> { nextStep(); true }
                        Key.DirectionLeft -> { prevStep(); true }
                        else -> false
                    }
                } else false
            }
    ) {
        // 左側：ファイルブラウザ
        Column(modifier = Modifier.fillMaxHeight().weight(0.4f).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { currentDirectory.parentFile?.let { currentDirectory = it } }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                }
                Button(onClick = {
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        selectedFile = currentDirectory
                    }
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        currentDirectory = chooser.selectedFile
                    }
                }) {
                    Text("フォルダ選択", fontSize = 11.sp)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // クエスト棋譜インポートボタン
            Button(
                onClick = {
                    val count = importShogiQuestFiles()
                    if (count > 0) {
                        infoMessage = "${count}件の棋譜をインポートしました。"
                        refreshFiles()
                    } else {
                        infoMessage = "Downloadsフォルダに該当する棋譜が見つかりませんでした。"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)
            ) {
                Text("Downloadsから棋譜をインポート", fontSize = 11.sp)
            }
            
            Spacer(Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                currentDirectory.parentFile?.let { parent ->
                    item {
                        FileEntryItem(
                            file = parent,
                            isParentLink = true,
                            onNavigate = { currentDirectory = it },
                            onSelect = { selectedFile = it },
                            onCopy = { copyToClipboard(it.readText()) }
                        )
                    }
                }
                items(directoryContents) { file ->
                    FileEntryItem(
                        file = file,
                        isSelected = (file == selectedFile),
                        onNavigate = { currentDirectory = it },
                        onSelect = { selectedFile = it },
                        onCopy = { copyToClipboard(it.readText()) }
                    )
                }
            }
        }

        // 右側：プレビュー・操作パネル
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.6f)
                .background(Color(0xFFEEEEEE))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = selectedFile?.name ?: "kifuファイルを選択してください",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            // ファイル操作ボタン（上部に集約）
            if (selectedFile?.isFile == true && selectedFile!!.extension.lowercase() == "csa") {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { 
                        val targetFile = File(selectedFile!!.parent, selectedFile!!.nameWithoutExtension + ".kifu")
                        if (targetFile.exists()) {
                            showOverwriteConfirm = selectedFile
                        } else {
                            convertCsaToKifu(selectedFile!!)
                            refreshFiles()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50), contentColor = Color.White)
                ) {
                    Text("KIFUに変換", fontSize = 10.sp)
                }
            }

            if (boardState.history.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ShogiBoardView(boardState)
                Spacer(Modifier.height(8.dp))
                
                // 棋譜操作コントロール
                Text(
                    text = "手数: ${boardState.currentStep} / ${boardState.history.size - 1}",
                    style = MaterialTheme.typography.caption
                )
                Text(
                    text = boardState.currentBoard?.lastMoveText ?: "",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.height(24.dp) // 高さを抑える
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { boardState.currentStep = 0 }, modifier = Modifier.height(32.dp)) {
                        Text("開始", fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    
                    // 戻るボタン
                    OutlinedButton(onClick = { prevStep() }, modifier = Modifier.height(32.dp)) {
                        Text("◀", fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(4.dp))

                    if (boardState.firstContactStep != -1) {
                        Button(onClick = { boardState.currentStep = boardState.firstContactStep }, modifier = Modifier.height(32.dp)) {
                            Text("衝突", fontSize = 10.sp)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    
                    // 進むボタン
                    OutlinedButton(onClick = { nextStep() }, modifier = Modifier.height(32.dp)) {
                        Text("▶", fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(4.dp))

                    Button(onClick = { boardState.currentStep = boardState.history.size - 1 }, modifier = Modifier.height(32.dp)) {
                        Text("終局", fontSize = 10.sp)
                    }
                }
                Slider(
                    value = boardState.currentStep.toInt().toFloat(),
                    onValueChange = { boardState.currentStep = it.toInt() },
                    valueRange = 0f..(boardState.history.size - 1).toFloat(),
                    steps = if (boardState.history.size > 2) boardState.history.size - 2 else 0,
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }
}
