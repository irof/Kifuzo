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
import dev.irof.kfv.logic.readTextWithEncoding
import dev.irof.kfv.ui.FileTreeItem
import dev.irof.kfv.ui.theme.ShogiColors
import dev.irof.kfv.ui.theme.ShogiDimensions
import dev.irof.kfv.ui.theme.ShogiIcons
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
    Column(modifier = modifier.fillMaxHeight().padding(ShogiDimensions.PaddingLarge)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TooltipArea(
                    tooltip = {
                        Surface(modifier = Modifier.shadow(4.dp), color = Color(0xFF333333), shape = MaterialTheme.shapes.small) {
                            Text(AppStrings.IMPORT_KIFU, modifier = Modifier.padding(8.dp), color = Color.White, fontSize = ShogiDimensions.FontSizeCaption)
                        }
                    }
                ) {
                    IconButton(onClick = onImport) {
                        Icon(ShogiIcons.Import, contentDescription = AppStrings.IMPORT_KIFU, tint = ShogiColors.Primary)
                    }
                }
            }
            SidebarIconButton(AppStrings.SETTINGS, ShogiIcons.Settings, Color.Gray, onShowSettings)
        }
        
        Spacer(Modifier.height(ShogiDimensions.PaddingMedium))

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
            Row(modifier = Modifier.padding(ShogiDimensions.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
                Icon(ShogiIcons.FolderSelect, contentDescription = null, tint = ShogiColors.Primary, modifier = Modifier.size(ShogiDimensions.IconSizeSmall))
                Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
                Text(text = currentRoot.toString(), fontSize = ShogiDimensions.FontSizeCaption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(ShogiDimensions.PaddingMedium))
        
        if (state.availableSenkei.isNotEmpty() || state.isScanning) {
            if (state.isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = ShogiDimensions.PaddingSmall).horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { onSelectSenkei(null) }, colors = ButtonDefaults.textButtonColors(contentColor = if (state.selectedSenkei == null) Color.Blue else Color.Gray)) { Text(AppStrings.ALL_SENKEI, fontSize = ShogiDimensions.FontSizeCaption) }
                state.availableSenkei.forEach { senkei ->
                    TextButton(onClick = { onSelectSenkei(senkei) }, colors = ButtonDefaults.textButtonColors(contentColor = if (state.selectedSenkei == senkei) Color.Blue else Color.Gray)) { Text(senkei, fontSize = ShogiDimensions.FontSizeCaption) }
                }
            }
            Divider()
        }

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
                Text(label, modifier = Modifier.padding(ShogiDimensions.PaddingMedium), color = Color.White, fontSize = ShogiDimensions.FontSizeCaption)
            }
        }
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint)
        }
    }
}
