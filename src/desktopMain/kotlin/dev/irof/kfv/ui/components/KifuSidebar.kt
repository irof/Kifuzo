package dev.irof.kfv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kfv.logic.readTextWithEncoding
import dev.irof.kfv.ui.FileTreeItem
import dev.irof.kfv.ui.theme.ShogiColors
import dev.irof.kfv.utils.AppStrings
import dev.irof.kfv.viewmodel.KifuManagerUiState
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlin.io.path.toPath

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KifuSidebar(
    state: KifuManagerUiState,
    currentRoot: Path,
    onSetRoot: (Path) -> Unit,
    onImport: () -> Unit,
    onShowSettings: () -> Unit,
    onSelectSenkei: (String?) -> Unit,
    onToggleDir: (dev.irof.kfv.models.FileTreeNode) -> Unit,
    onSelectFile: (Path) -> Unit,
    onShowText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight().padding(16.dp)) {
        // ボタン群
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SidebarIconButton(AppStrings.IMPORT_KIFU, Icons.Default.Add, ShogiColors.Primary, onImport)
            }
            SidebarIconButton(AppStrings.SETTINGS, Icons.Default.Settings, Color.Gray, onShowSettings)
        }
        
        Spacer(Modifier.height(8.dp))

        // ルートパス表示
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                val chooser = JFileChooser().apply { 
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    currentDirectory = currentRoot.toFile()
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    onSetRoot(chooser.selectedFile.toPath())
                }
            },
            elevation = 0.dp,
            backgroundColor = Color.White,
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Menu, contentDescription = null, tint = ShogiColors.Primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = currentRoot.toString(), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(8.dp))
        
        // 戦型フィルタ
        if (state.availableSenkei.isNotEmpty() || state.isScanning) {
            if (state.isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { onSelectSenkei(null) }, colors = ButtonDefaults.textButtonColors(contentColor = if (state.selectedSenkei == null) Color.Blue else Color.Gray)) { Text(AppStrings.ALL_SENKEI, fontSize = 10.sp) }
                state.availableSenkei.forEach { senkei ->
                    TextButton(onClick = { onSelectSenkei(senkei) }, colors = ButtonDefaults.textButtonColors(contentColor = if (state.selectedSenkei == senkei) Color.Blue else Color.Gray)) { Text(senkei, fontSize = 10.sp) }
                }
            }
            Divider()
        }

        // ファイルツリー
        val treeHorizontalScroll = rememberScrollState()
        Box(modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(treeHorizontalScroll)) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(state.filteredNodes) { node ->
                    FileTreeItem(
                        node = node, 
                        isSelected = (node.path == state.selectedFile), 
                        onToggle = onToggleDir,
                        onSelect = onSelectFile, 
                        onShowText = { onShowText(readTextWithEncoding(it)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarIconButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(modifier = Modifier.shadow(4.dp), color = Color(0xFF333333), shape = MaterialTheme.shapes.small) {
                Text(label, modifier = Modifier.padding(8.dp), color = Color.White, fontSize = 12.sp)
            }
        }
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint)
        }
    }
}
