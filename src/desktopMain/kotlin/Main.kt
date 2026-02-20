import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import logic.KifuInfo
import logic.convertCsaToKifu
import logic.detectSenkei
import logic.importShogiQuestFiles
import logic.parseKifu
import logic.scanKifuInfo
import logic.updateKifuSenkei
import models.AppConfig
import models.AppSettings
import models.ShogiBoardState
import ui.FileEntryItem
import ui.ShogiBoardView
import utils.copyToClipboard
import java.io.File
import javax.swing.JFileChooser

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
    var kifuInfos by remember { mutableStateOf(mapOf<File, KifuInfo>()) }
    var selectedSenkei by remember { mutableStateOf<String?>(null) }
    
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showOverwriteConfirm by remember { mutableStateOf<File?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var viewingText by remember { mutableStateOf<String?>(null) }
    var myNameRegex by remember { mutableStateOf(AppSettings.myNameRegex) }
    var isFlipped by remember { mutableStateOf(false) }
    val boardState = remember { ShogiBoardState() }
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }

    // 自動反転ロジック
    fun updateAutoFlip() {
        if (myNameRegex.isEmpty()) return
        val regex = try { Regex(myNameRegex) } catch (e: Exception) { null } ?: return
        
        if (regex.containsMatchIn(boardState.goteName) && !regex.containsMatchIn(boardState.senteName)) {
            isFlipped = true
        } else if (regex.containsMatchIn(boardState.senteName)) {
            isFlipped = false
        }
    }

    LaunchedEffect(boardState.senteName, boardState.goteName, myNameRegex) {
        updateAutoFlip()
    }

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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // ディレクトリ内容の更新とスキャン
    fun refreshFiles() {
        val contents = currentDirectory.listFiles { file ->
            file.isDirectory || (file.isFile && file.extension.lowercase() in listOf("kif", "kifu", "kifz", "csa", "jkf", "txt"))
        }?.toList() ?: emptyList()
        directoryContents = contents.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        
        // 棋譜情報のスキャン（戦型抽出）
        val newInfos = mutableMapOf<File, KifuInfo>()
        contents.filter { it.isFile && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }.forEach { file ->
            newInfos[file] = scanKifuInfo(file)
        }
        kifuInfos = newInfos
    }

    LaunchedEffect(currentDirectory) {
        selectedSenkei = null
        refreshFiles()
    }
    
    // フィルタリングされたリスト
    val filteredContents = remember(directoryContents, selectedSenkei, kifuInfos) {
        if (selectedSenkei == null) directoryContents
        else directoryContents.filter { file ->
            file.isDirectory || kifuInfos[file]?.senkei == selectedSenkei
        }
    }

    // 利用可能な戦型リスト
    val availableSenkei = remember(kifuInfos) {
        kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()
    }

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

    // ダイアログ類
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("解析エラー") },
            text = { Text(errorMessage!!, fontSize = 12.sp) },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { copyToClipboard(errorMessage!!); errorMessage = null }) { Text("コピーして閉じる") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { errorMessage = null }) { Text("閉じる") }
                }
            }
        )
    }

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

    // テキスト表示ダイアログ
    if (viewingText != null) {
        AlertDialog(
            onDismissRequest = { viewingText = null },
            title = { Text("棋譜テキスト") },
            modifier = Modifier.width(600.dp), // 幅だけ固定
            text = {
                val scrollStateVertical = rememberScrollState()
                val scrollStateHorizontal = rememberScrollState()
                // 内部のサイズを厳密に固定
                Column(modifier = Modifier.width(560.dp).height(400.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .border(1.dp, Color.LightGray)
                            .verticalScroll(scrollStateVertical)
                            .horizontalScroll(scrollStateHorizontal)
                    ) {
                        Text(
                            text = viewingText!!,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            softWrap = false,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { copyToClipboard(viewingText!!); viewingText = null }) { Text("コピーして閉じる") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { viewingText = null }) { Text("閉じる") }
                }
            }
        )
    }

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
                    Button(onClick = { convertCsaToKifu(showOverwriteConfirm!!); refreshFiles(); showOverwriteConfirm = null }) { Text("上書きする") }
                }
            }
        )
    }

    if (showSettings) {
        var tempRegex by remember { mutableStateOf(myNameRegex) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("設定") },
            text = {
                Column {
                    Text("自分の名前の判定用（正規表現）:")
                    Spacer(Modifier.height(8.dp))
                    TextField(value = tempRegex, onValueChange = { tempRegex = it }, placeholder = { Text("例: (irof|名無し)") }, modifier = Modifier.fillMaxWidth())
                    Text("※自分が後手の場合、自動的に盤面を反転します。", fontSize = 10.sp, color = Color.Gray)
                }
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showSettings = false }) { Text("キャンセル") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { myNameRegex = tempRegex; AppSettings.myNameRegex = tempRegex; showSettings = false }) { Text("保存") }
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
                    val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY; selectedFile = currentDirectory }
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) currentDirectory = chooser.selectedFile
                }, modifier = Modifier.weight(1f)) { Text("フォルダ選択", fontSize = 11.sp) }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "設定") }
            }
            
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val count = importShogiQuestFiles()
                if (count > 0) { infoMessage = "${count}件の棋譜をインポートしました。"; refreshFiles() }
                else { infoMessage = "Downloadsフォルダに該当する棋譜が見つかりませんでした。" }
            }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)) { Text("Downloadsから棋譜をインポート", fontSize = 11.sp) }
            
            Spacer(Modifier.height(8.dp))
            
            // 戦型フィルタエリア
            if (availableSenkei.isNotEmpty()) {
                Text("戦型フィルタ:", style = MaterialTheme.typography.caption)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState())) {
                    TextButton(
                        onClick = { selectedSenkei = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (selectedSenkei == null) Color.Blue else Color.Gray)
                    ) { Text("すべて", fontSize = 10.sp) }
                    availableSenkei.forEach { senkei ->
                        TextButton(
                            onClick = { selectedSenkei = senkei },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (selectedSenkei == senkei) Color.Blue else Color.Gray)
                        ) { Text(senkei, fontSize = 10.sp) }
                    }
                }
                Divider()
            }

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            currentDirectory.parentFile?.let { parent ->
                                item {
                                    FileEntryItem(
                                        file = parent,
                                        isParentLink = true,
                                        onNavigate = { currentDirectory = it },
                                        onSelect = { selectedFile = it },
                                        onShowText = { viewingText = it.readText() }
                                    )
                                }
                            }
                            items(filteredContents) { file ->
                                FileEntryItem(
                                    file = file,
                                    isSelected = (file == selectedFile),
                                    onNavigate = { currentDirectory = it },
                                    onSelect = { selectedFile = it },
                                    onShowText = { viewingText = it.readText() }
                                )
                            }
                        }
        }

        // 右側：プレビュー・操作パネル
        Column(
            modifier = Modifier.fillMaxHeight().weight(0.6f).background(Color(0xFFEEEEEE)).verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(8.dp))
            Text(text = selectedFile?.name ?: "kifuファイルを選択してください", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            
            if (selectedFile?.isFile == true) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { isFlipped = !isFlipped }, modifier = Modifier.height(32.dp), colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (isFlipped) Color.LightGray else Color.White)) { Text("盤面反転", fontSize = 10.sp) }
                    
                    val ext = selectedFile!!.extension.lowercase()
                    if (ext == "kifu" || ext == "kif") {
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                val senkei = detectSenkei(boardState.history)
                                if (senkei.isNotEmpty()) {
                                    updateKifuSenkei(selectedFile!!, senkei)
                                    refreshFiles()
                                    infoMessage = "戦型を「$senkei」として追記しました。"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3), contentColor = Color.White),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("戦型判定", fontSize = 10.sp)
                        }
                    }

                    if (ext == "csa") {
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { 
                            val targetFile = File(selectedFile!!.parent, selectedFile!!.nameWithoutExtension + ".kifu")
                            if (targetFile.exists()) { showOverwriteConfirm = selectedFile } 
                            else { convertCsaToKifu(selectedFile!!); refreshFiles() }
                        }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50), contentColor = Color.White), modifier = Modifier.height(32.dp)) { Text("KIFUに変換", fontSize = 10.sp) }
                    }
                }
            }

            if (boardState.history.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ShogiBoardView(boardState, isFlipped = isFlipped)
                Spacer(Modifier.height(8.dp))
                Text(text = "手数: ${boardState.currentStep} / ${boardState.history.size - 1}", style = MaterialTheme.typography.caption)
                Text(text = boardState.currentBoard?.lastMoveText ?: "", style = MaterialTheme.typography.body2, modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { boardState.currentStep = 0 }, modifier = Modifier.height(32.dp)) { Text("開始", fontSize = 10.sp) }
                    Spacer(Modifier.width(4.dp))
                    OutlinedButton(onClick = { prevStep() }, modifier = Modifier.height(32.dp)) { Text("◀", fontSize = 10.sp) }
                    Spacer(Modifier.width(4.dp))
                    if (boardState.firstContactStep != -1) {
                        Button(onClick = { boardState.currentStep = boardState.firstContactStep }, modifier = Modifier.height(32.dp)) { Text("衝突", fontSize = 10.sp) }
                        Spacer(Modifier.width(4.dp))
                    }
                    OutlinedButton(onClick = { nextStep() }, modifier = Modifier.height(32.dp)) { Text("▶", fontSize = 10.sp) }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { boardState.currentStep = boardState.history.size - 1 }, modifier = Modifier.height(32.dp)) { Text("終局", fontSize = 10.sp) }
                }
                Slider(value = boardState.currentStep.toInt().toFloat(), onValueChange = { boardState.currentStep = it.toInt() }, valueRange = 0f..(boardState.history.size - 1).toFloat(), steps = if (boardState.history.size > 2) boardState.history.size - 2 else 0, modifier = Modifier.width(280.dp))
            }
        }
    }
}
