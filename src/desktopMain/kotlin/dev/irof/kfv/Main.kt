package dev.irof.kfv

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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.irof.kfv.logic.readTextWithEncoding
import dev.irof.kfv.models.AppSettings
import dev.irof.kfv.ui.FileEntryItem
import dev.irof.kfv.ui.ShogiBoardView
import dev.irof.kfv.viewmodel.KifuManagerViewModel
import dev.irof.kfv.utils.copyToClipboard
import java.io.File
import javax.swing.JFileChooser

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
    val viewModel = remember { KifuManagerViewModel() }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    // 手数を進める/戻す関数
    fun nextStep() { if (viewModel.boardState.currentStep < viewModel.boardState.history.size - 1) viewModel.boardState.currentStep++ }
    fun prevStep() { if (viewModel.boardState.currentStep > 0) viewModel.boardState.currentStep-- }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        viewModel.refreshFiles()
    }
    
    // ディレクトリが変わったらリフレッシュ
    LaunchedEffect(viewModel.currentDirectory) {
        viewModel.selectedSenkei = null
        viewModel.refreshFiles()
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
                    IconButton(onClick = { viewModel.currentDirectory.parentFile?.let { viewModel.currentDirectory = it } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                    Button(onClick = {
                        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY; selectedFile = viewModel.currentDirectory }
                        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) viewModel.currentDirectory = chooser.selectedFile
                    }, modifier = Modifier.weight(1f)) { Text("フォルダ選択", fontSize = 11.sp) }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { viewModel.showSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "設定") }
                }
                
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.importFiles() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE), contentColor = Color.White)) { Text("Downloadsから棋譜をインポート", fontSize = 11.sp) }
                
                Spacer(Modifier.height(8.dp))
                
                if (viewModel.availableSenkei.isNotEmpty()) {
                    Text("戦型フィルタ:", style = MaterialTheme.typography.caption)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { viewModel.selectedSenkei = null }, colors = ButtonDefaults.textButtonColors(contentColor = if (viewModel.selectedSenkei == null) Color.Blue else Color.Gray)) { Text("すべて", fontSize = 10.sp) }
                        viewModel.availableSenkei.forEach { senkei ->
                            TextButton(onClick = { viewModel.selectedSenkei = senkei }, colors = ButtonDefaults.textButtonColors(contentColor = if (viewModel.selectedSenkei == senkei) Color.Blue else Color.Gray)) { Text(senkei, fontSize = 10.sp) }
                        }
                    }
                    Divider()
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    viewModel.currentDirectory.parentFile?.let { parent ->
                        item { FileEntryItem(file = parent, isParentLink = true, onNavigate = { viewModel.currentDirectory = it }, onSelect = { viewModel.selectFile(it) }, onShowText = { viewModel.viewingText = readTextWithEncoding(it) }) }
                    }
                    items(viewModel.filteredContents) { file ->
                        FileEntryItem(file = file, isSelected = (file == viewModel.selectedFile), onNavigate = { viewModel.currentDirectory = it }, onSelect = { viewModel.selectFile(it) }, onShowText = { viewModel.viewingText = readTextWithEncoding(it) })
                    }
                }
            }

            // 右側：プレビュー・操作パネル
            Column(
                modifier = Modifier.fillMaxHeight().weight(0.6f).background(Color(0xFFEEEEEE)).verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(8.dp))
                Text(text = viewModel.selectedFile?.name ?: "kifuファイルを選択してください", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                
                if (viewModel.selectedFile?.isFile == true) {
                    val ext = viewModel.selectedFile!!.extension.lowercase()
                    val isKifuFile = ext == "kifu" || ext == "kif"
                    val hasHistory = viewModel.boardState.history.isNotEmpty()

                    if (hasHistory || ext == "csa") {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            if (hasHistory) {
                                OutlinedButton(onClick = { viewModel.isFlipped = !viewModel.isFlipped }, modifier = Modifier.height(32.dp), colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (viewModel.isFlipped) Color.LightGray else Color.White)) { Text("盤面反転", fontSize = 10.sp) }
                                
                                if (isKifuFile) {
                                    val existingSenkei = viewModel.kifuInfos[viewModel.selectedFile]?.senkei
                                    Spacer(Modifier.width(8.dp))
                                    
                                    if (!existingSenkei.isNullOrEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp).background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.LightGray, MaterialTheme.shapes.small).padding(horizontal = 8.dp)) {
                                            Text("戦型: $existingSenkei", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { viewModel.detectAndWriteSenkei(viewModel.selectedFile!!) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "再判定", tint = Color(0xFF2196F3))
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.detectAndWriteSenkei(viewModel.selectedFile!!) },
                                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3), contentColor = Color.White),
                                            modifier = Modifier.height(32.dp)
                                        ) { Text("戦型判定", fontSize = 10.sp) }
                                    }
                                }
                            }

                            if (ext == "csa") {
                                if (hasHistory) Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.convertCsa(viewModel.selectedFile!!) }, 
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50), contentColor = Color.White), 
                                    modifier = Modifier.height(32.dp)
                                ) { Text("KIFUに変換", fontSize = 10.sp) }
                            }
                        }
                    }
                }

                if (viewModel.boardState.history.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ShogiBoardView(viewModel.boardState, isFlipped = viewModel.isFlipped)
                    Spacer(Modifier.height(8.dp))
                    Text(text = "手数: ${viewModel.boardState.currentStep} / ${viewModel.boardState.history.size - 1}", style = MaterialTheme.typography.caption)
                    Text(text = viewModel.boardState.currentBoard?.lastMoveText ?: "", style = MaterialTheme.typography.body2, modifier = Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { viewModel.boardState.currentStep = 0 }, modifier = Modifier.height(32.dp)) { Text("開始", fontSize = 10.sp) }
                        Spacer(Modifier.width(4.dp))
                        OutlinedButton(onClick = { prevStep() }, modifier = Modifier.height(32.dp)) { Text("◀", fontSize = 10.sp) }
                        Spacer(Modifier.width(4.dp))
                        if (viewModel.boardState.firstContactStep != -1) {
                            Button(onClick = { viewModel.boardState.currentStep = viewModel.boardState.firstContactStep }, modifier = Modifier.height(32.dp)) { Text("衝突", fontSize = 10.sp) }
                            Spacer(Modifier.width(4.dp))
                        }
                        OutlinedButton(onClick = { nextStep() }, modifier = Modifier.height(32.dp)) { Text("▶", fontSize = 10.sp) }
                        Spacer(Modifier.width(4.dp))
                        Button(onClick = { viewModel.boardState.currentStep = viewModel.boardState.history.size - 1 }, modifier = Modifier.height(32.dp)) { Text("終局", fontSize = 10.sp) }
                    }
                    Slider(value = viewModel.boardState.currentStep.toInt().toFloat(), onValueChange = { viewModel.boardState.currentStep = it.toInt() }, valueRange = 0f..(viewModel.boardState.history.size - 1).toFloat(), steps = if (viewModel.boardState.history.size > 2) viewModel.boardState.history.size - 2 else 0, modifier = Modifier.width(280.dp))
                }
            }
        }

        // --- オーバーレイ・ダイアログ類 ---

        if (viewModel.errorMessage != null || viewModel.infoMessage != null) {
            val title = if (viewModel.errorMessage != null) "エラー" else "通知"
            val msg = viewModel.errorMessage ?: viewModel.infoMessage!!
            AlertDialog(
                onDismissRequest = { viewModel.errorMessage = null; viewModel.infoMessage = null },
                title = { Text(title) },
                text = { Text(msg, fontSize = 12.sp) },
                buttons = {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                        Button(onClick = { viewModel.errorMessage = null; viewModel.infoMessage = null }) { Text("OK") }
                    }
                }
            )
        }

        if (viewModel.showOverwriteConfirm != null) {
            val targetFile = File(viewModel.showOverwriteConfirm!!.parent, viewModel.showOverwriteConfirm!!.nameWithoutExtension + ".kifu")
            AlertDialog(
                onDismissRequest = { viewModel.showOverwriteConfirm = null },
                title = { Text("上書き確認") },
                text = { Text("${targetFile.name} は既に存在します。上書きしますか？") },
                buttons = {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.showOverwriteConfirm = null }) { Text("キャンセル") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.confirmOverwrite() }) { Text("上書きする") }
                    }
                }
            )
        }

        if (viewModel.showSettings) {
            var tempRegex by remember { mutableStateOf(viewModel.myNameRegex) }
            var rawSettings by remember { mutableStateOf(AppSettings.getAllSettings()) }
            AlertDialog(
                onDismissRequest = { viewModel.showSettings = false },
                title = { Text("設定") },
                modifier = Modifier.width(500.dp),
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("自分の名前の判定用（正規表現）:", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        TextField(value = tempRegex, onValueChange = { tempRegex = it }, placeholder = { Text("例: (irof|名無し)") }, modifier = Modifier.fillMaxWidth())
                        Text("※自分が後手の場合、自動的に盤面を反転します。", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(24.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                        Text("保存されている詳細データ (Preferences):", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        rawSettings.forEach { (key, value) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(key, fontSize = 10.sp, color = Color.Gray)
                                    var editingValue by remember(key) { mutableStateOf(value) }
                                    BasicTextField(value = editingValue, onValueChange = { editingValue = it; AppSettings.putSetting(key, it) }, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp), modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f)).padding(4.dp))
                                }
                                IconButton(onClick = { AppSettings.removeSetting(key); rawSettings = AppSettings.getAllSettings() }) { Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.Red, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                },
                buttons = {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { viewModel.showSettings = false }) { Text("閉じる") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.saveSettings(tempRegex) }) { Text("名前設定を保存") }
                    }
                }
            )
        }

        if (viewModel.viewingText != null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { viewModel.viewingText = null }, contentAlignment = Alignment.Center) {
                Card(modifier = Modifier.size(600.dp, 550.dp).clickable(enabled = false) { }, elevation = 8.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("棋譜テキスト", style = MaterialTheme.typography.h6)
                        Spacer(Modifier.height(16.dp))
                        val scrollSVer = rememberScrollState()
                        val scrollSHor = rememberScrollState()
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White).border(1.dp, Color.LightGray)) {
                            Text(text = viewModel.viewingText!!, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, softWrap = false, modifier = Modifier.fillMaxSize().padding(8.dp).verticalScroll(scrollSVer).horizontalScroll(scrollSHor))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { copyToClipboard(viewModel.viewingText!!); viewModel.viewingText = null }) { Text("コピーして閉じる") }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { viewModel.viewingText = null }) { Text("閉じる") }
                        }
                    }
                }
            }
        }
    }
}
