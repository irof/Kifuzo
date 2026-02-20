import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import logic.*
import models.AppConfig
import models.AppSettings
import models.ShogiBoardState
import ui.FileEntryItem
import ui.ShogiBoardView
import utils.copyToClipboard
import java.io.File
import javax.swing.JFileChooser

import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        position = if (AppSettings.windowX != null && AppSettings.windowY != null) {
            WindowPosition(AppSettings.windowX!!.dp, AppSettings.windowY!!.dp)
        } else {
            WindowPosition.Aligned(Alignment.Center)
        },
        size = DpSize(AppSettings.windowWidth.dp, AppSettings.windowHeight.dp)
    )

    Window(
        onCloseRequest = {
            // ウィンドウの状態を保存
            AppSettings.windowX = windowState.position.let { if (it is WindowPosition.Absolute) it.x.value else null }
            AppSettings.windowY = windowState.position.let { if (it is WindowPosition.Absolute) it.y.value else null }
            AppSettings.windowWidth = windowState.size.width.value
            AppSettings.windowHeight = windowState.size.height.value
            exitApplication()
        },
        title = "棋譜管理アプリ",
        state = windowState
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

    LaunchedEffect(boardState.senteName, boardState.goteName, myNameRegex) { updateAutoFlip() }

    fun nextStep() { if (boardState.currentStep < boardState.history.size - 1) boardState.currentStep++ }
    fun prevStep() { if (boardState.currentStep > 0) boardState.currentStep-- }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun refreshFiles() {
        val contents = currentDirectory.listFiles { file ->
            file.isDirectory || (file.isFile && file.extension.lowercase() in listOf("kif", "kifu", "kifz", "csa", "jkf", "txt"))
        }?.toList() ?: emptyList()
        directoryContents = contents.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        
        val newInfos = mutableMapOf<File, KifuInfo>()
        contents.filter { it.isFile && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }.forEach { file ->
            newInfos[file] = scanKifuInfo(file)
        }
        kifuInfos = newInfos
    }

    LaunchedEffect(currentDirectory) { selectedSenkei = null; refreshFiles() }
    
    val filteredContents = remember(directoryContents, selectedSenkei, kifuInfos) {
        if (selectedSenkei == null) directoryContents
        else directoryContents.filter { file -> file.isDirectory || kifuInfos[file]?.senkei == selectedSenkei }
    }

    val availableSenkei = remember(kifuInfos) {
        kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    LaunchedEffect(selectedFile) {
        val ext = selectedFile?.extension?.lowercase()
        if (ext == "kifu" || ext == "kif") {
            try { parseKifu(selectedFile!!, boardState) } catch (e: Exception) {
                errorMessage = "解析中断: ${selectedFile?.name}\n\n${e.message}"
                boardState.history = emptyList()
            }
        } else { boardState.history = emptyList() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                
                if (availableSenkei.isNotEmpty()) {
                    Text("戦型フィルタ:", style = MaterialTheme.typography.caption)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { selectedSenkei = null }, colors = ButtonDefaults.textButtonColors(contentColor = if (selectedSenkei == null) Color.Blue else Color.Gray)) { Text("すべて", fontSize = 10.sp) }
                        availableSenkei.forEach { senkei ->
                            TextButton(onClick = { selectedSenkei = senkei }, colors = ButtonDefaults.textButtonColors(contentColor = if (selectedSenkei == senkei) Color.Blue else Color.Gray)) { Text(senkei, fontSize = 10.sp) }
                        }
                    }
                    Divider()
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    currentDirectory.parentFile?.let { parent ->
                        item { FileEntryItem(file = parent, isParentLink = true, onNavigate = { currentDirectory = it }, onSelect = { selectedFile = it }, onShowText = { viewingText = readTextWithEncoding(it) }) }
                    }
                    items(filteredContents) { file ->
                        FileEntryItem(file = file, isSelected = (file == selectedFile), onNavigate = { currentDirectory = it }, onSelect = { selectedFile = it }, onShowText = { viewingText = readTextWithEncoding(it) })
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
                                val ext = selectedFile!!.extension.lowercase()
                                val isKifuFile = ext == "kifu" || ext == "kif"
                                val hasHistory = boardState.history.isNotEmpty()
                
                                if (hasHistory || ext == "csa") {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                                                if (hasHistory) {
                                                                    OutlinedButton(onClick = { isFlipped = !isFlipped }, modifier = Modifier.height(32.dp), colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (isFlipped) Color.LightGray else Color.White)) { Text("盤面反転", fontSize = 10.sp) }
                                                                    
                                                                    if (isKifuFile) {
                                                                        val existingSenkei = kifuInfos[selectedFile]?.senkei
                                                                        Spacer(Modifier.width(8.dp))
                                                                        
                                                                        if (!existingSenkei.isNullOrEmpty()) {
                                                                            // 戦型が表示されている場合
                                                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp).background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.LightGray, MaterialTheme.shapes.small).padding(horizontal = 8.dp)) {
                                                                                Text("戦型: $existingSenkei", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                                Spacer(Modifier.width(4.dp))
                                                                                IconButton(
                                                                                    onClick = { 
                                                                                        val senkei = detectSenkei(boardState.history)
                                                                                        if (senkei.isNotEmpty()) { updateKifuSenkei(selectedFile!!, senkei); refreshFiles(); infoMessage = "戦型を「$senkei」として更新しました。" }
                                                                                    },
                                                                                    modifier = Modifier.size(18.dp)
                                                                                ) {
                                                                                    Icon(Icons.Default.Refresh, contentDescription = "再判定", tint = Color(0xFF2196F3))
                                                                                }
                                                                            }
                                                                        } else {
                                                                            // 戦型が未設定の場合
                                                                            Button(
                                                                                onClick = { 
                                                                                    val senkei = detectSenkei(boardState.history)
                                                                                    if (senkei.isNotEmpty()) { updateKifuSenkei(selectedFile!!, senkei); refreshFiles(); infoMessage = "戦型を「$senkei」として追記しました。" }
                                                                                },
                                                                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3), contentColor = Color.White),
                                                                                modifier = Modifier.height(32.dp)
                                                                            ) {
                                                                                Text("戦型判定", fontSize = 10.sp)
                                                                            }
                                                                        }
                                                                    }
                                                                }                
                                                                if (ext == "csa") {
                                                                    if (hasHistory) Spacer(Modifier.width(8.dp))
                                                                    Button(
                                                                        onClick = { 
                                                                            val targetFile = File(selectedFile!!.parent, selectedFile!!.nameWithoutExtension + ".kifu")
                                                                            val performConversion = {
                                                                                convertCsaToKifu(selectedFile!!)
                                                                                // 変換後に戦型を自動判定して追記
                                                                                try {
                                                                                    val tempState = ShogiBoardState()
                                                                                    parseKifu(targetFile, tempState)
                                                                                    val senkei = detectSenkei(tempState.history)
                                                                                    if (senkei.isNotEmpty()) {
                                                                                        updateKifuSenkei(targetFile, senkei)
                                                                                    }
                                                                                } catch (e: Exception) {
                                                                                    println("Auto-senkei detection failed: ${e.message}")
                                                                                }
                                                                                refreshFiles()
                                                                            }
                                        
                                                                            if (targetFile.exists()) {
                                                                                showOverwriteConfirm = selectedFile
                                                                            } else {
                                                                                performConversion()
                                                                            }
                                                                        }, 
                                                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50), contentColor = Color.White), 
                                                                        modifier = Modifier.height(32.dp)
                                                                    ) { 
                                                                        Text("KIFUに変換", fontSize = 10.sp) 
                                                                    }
                                                                }                                    }
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

        // --- オーバーレイ・ダイアログ類 ---

        // 共通エラー・通知
        if (errorMessage != null || infoMessage != null) {
            val title = if (errorMessage != null) "エラー" else "通知"
            val msg = errorMessage ?: infoMessage!!
            AlertDialog(
                onDismissRequest = { errorMessage = null; infoMessage = null },
                title = { Text(title) },
                text = { Text(msg, fontSize = 12.sp) },
                buttons = {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                        Button(onClick = { errorMessage = null; infoMessage = null }) { Text("OK") }
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
                            Button(onClick = {
                                val fileToConvert = showOverwriteConfirm!!
                                convertCsaToKifu(fileToConvert)
                                
                                // 変換後に戦型を自動判定して追記
                                try {
                                    val tempState = ShogiBoardState()
                                    parseKifu(targetFile, tempState)
                                    val senkei = detectSenkei(tempState.history)
                                    if (senkei.isNotEmpty()) {
                                        updateKifuSenkei(targetFile, senkei)
                                    }
                                } catch (e: Exception) {
                                    println("Auto-senkei detection failed: ${e.message}")
                                }
        
                                refreshFiles()
                                showOverwriteConfirm = null
                            }) { Text("上書きする") }
                        }
                    }
                )
            }
        if (showSettings) {
            var tempRegex by remember { mutableStateOf(myNameRegex) }
            var rawSettings by remember { mutableStateOf(AppSettings.getAllSettings()) }

            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("設定") },
                modifier = Modifier.width(500.dp),
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("自分の名前の判定用（正規表現）:", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            value = tempRegex, 
                            onValueChange = { tempRegex = it }, 
                            placeholder = { Text("例: (irof|名無し)") }, 
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("※自分が後手の場合、自動的に盤面を反転します。", fontSize = 10.sp, color = Color.Gray)
                        
                        Spacer(Modifier.height(24.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                        
                        Text("保存されている詳細データ (Preferences):", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        rawSettings.forEach { (key, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(key, fontSize = 10.sp, color = Color.Gray)
                                    var editingValue by remember(key) { mutableStateOf(value) }
                                    BasicTextField(
                                        value = editingValue,
                                        onValueChange = { 
                                            editingValue = it
                                            AppSettings.putSetting(key, it)
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                        modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f)).padding(4.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    AppSettings.removeSetting(key)
                                    rawSettings = AppSettings.getAllSettings()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                buttons = {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showSettings = false }) { Text("閉じる") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            myNameRegex = tempRegex
                            AppSettings.myNameRegex = tempRegex
                            showSettings = false
                        }) { Text("名前設定を保存") }
                    }
                }
            )
        }

        // 棋譜テキスト表示（カスタムオーバーレイ）
        if (viewingText != null) {
            // 背景（シールド）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { viewingText = null }, // 背景クリックで閉じる
                contentAlignment = Alignment.Center
            ) {
                // ダイアログ本体
                Card(
                    modifier = Modifier
                        .size(600.dp, 550.dp)
                        .clickable(enabled = false) { }, // 中身のクリックを背景に逃がさない
                    elevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("棋譜テキスト", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(16.dp))
                        
                        val scrollSVer = rememberScrollState()
                        val scrollSHor = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.White)
                                .border(1.dp, Color.LightGray)
                        ) {
                            Text(
                                text = viewingText!!,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                softWrap = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(scrollSVer)
                                    .horizontalScroll(scrollSHor)
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { copyToClipboard(viewingText!!); viewingText = null }) { Text("コピーして閉じる") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { viewingText = null }) { Text("閉じる") }
                        }
                    }
                }
            }
        }
    }
}
