package dev.irof.kfv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
    currentRoot: Path?,
    onSetRoot: (Path) -> Unit,
    onImport: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleDir: (dev.irof.kfv.models.FileTreeNode) -> Unit,
    onSelectFile: (Path) -> Unit,
    onShowText: (String) -> Unit,
    onSetViewMode: (dev.irof.kfv.viewmodel.FileViewMode) -> Unit,
    onSetFileFilter: (dev.irof.kfv.viewmodel.FileFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxHeight().padding(ShogiDimensions.PaddingLarge)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TooltipArea(
                    tooltip = {
                        Surface(modifier = Modifier.shadow(4.dp), color = Color(0xFF333333), shape = MaterialTheme.shapes.small) {
                            Text(AppStrings.IMPORT_KIFU, modifier = Modifier.padding(8.dp), color = Color.White, fontSize = ShogiDimensions.FontSizeCaption)
                        }
                    },
                ) {
                    IconButton(onClick = onImport, enabled = currentRoot != null) {
                        Icon(ShogiIcons.Import, contentDescription = AppStrings.IMPORT_KIFU, tint = if (currentRoot != null) ShogiColors.Primary else Color.Gray)
                    }
                }
                Spacer(Modifier.width(8.dp))
                // --- 表示モード切替 ---
                Row(modifier = Modifier.border(1.dp, Color.LightGray, MaterialTheme.shapes.small).padding(2.dp)) {
                    val mode = state.viewMode
                    ViewModeButton(AppStrings.HIERARCHY, mode == dev.irof.kfv.viewmodel.FileViewMode.HIERARCHY) { onSetViewMode(dev.irof.kfv.viewmodel.FileViewMode.HIERARCHY) }
                    ViewModeButton(AppStrings.FLAT, mode == dev.irof.kfv.viewmodel.FileViewMode.FLAT) { onSetViewMode(dev.irof.kfv.viewmodel.FileViewMode.FLAT) }
                }
            }
            SidebarIconButton(AppStrings.SETTINGS, ShogiIcons.Settings, Color.Gray, onShowSettings)
        }

        Spacer(Modifier.height(ShogiDimensions.PaddingMedium))

        // --- フィルタ選択 ---
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val currentFilter = state.fileFilter
            FilterChip(AppStrings.FILTER_ALL, currentFilter == dev.irof.kfv.viewmodel.FileFilter.ALL) { onSetFileFilter(dev.irof.kfv.viewmodel.FileFilter.ALL) }
            Spacer(Modifier.width(4.dp))
            FilterChip(AppStrings.FILTER_KIFU, currentFilter == dev.irof.kfv.viewmodel.FileFilter.KIFU_ONLY) { onSetFileFilter(dev.irof.kfv.viewmodel.FileFilter.KIFU_ONLY) }
            Spacer(Modifier.width(4.dp))
            FilterChip(AppStrings.FILTER_RECENT, currentFilter == dev.irof.kfv.viewmodel.FileFilter.RECENT) { onSetFileFilter(dev.irof.kfv.viewmodel.FileFilter.RECENT) }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    val initialDir = currentRoot?.toFile() ?: dev.irof.kfv.models.AppConfig.USER_HOME_PATH.toFile()
                    currentDirectory = initialDir
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    onSetRoot(chooser.selectedFile.toPath())
                }
            },
            elevation = 0.dp,
            backgroundColor = Color.White,
            border = BorderStroke(1.dp, Color.LightGray),
        ) {
            Row(modifier = Modifier.padding(ShogiDimensions.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
                Icon(ShogiIcons.FolderSelect, contentDescription = null, tint = ShogiColors.Primary, modifier = Modifier.size(ShogiDimensions.IconSizeSmall))
                Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
                Text(text = currentRoot?.toString() ?: AppStrings.SELECT_KIFU_ROOT, fontSize = ShogiDimensions.FontSizeCaption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = if (currentRoot == null) Color.Gray else Color.Black)
            }
        }

        Spacer(Modifier.height(ShogiDimensions.PaddingMedium))

        val treeHorizontalScroll = rememberScrollState()
        Box(modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(treeHorizontalScroll)) {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(state.treeNodes) { node ->
                    FileTreeItem(
                        node = node,
                        isSelected = (node.path == state.selectedFile),
                        isError = state.kifuInfos[node.path]?.isError ?: false,
                        showParentName = (state.viewMode == dev.irof.kfv.viewmodel.FileViewMode.FLAT),
                        onToggle = onToggleDir,
                        onSelect = onSelectFile,
                        onShowText = { onShowText(readTextWithEncoding(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) ShogiColors.Primary else Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) ShogiColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) ShogiColors.Primary else Color.LightGray),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) ShogiColors.Primary else Color.Gray,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarIconButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    TooltipArea(
        tooltip = {
            Surface(modifier = Modifier.shadow(4.dp), color = Color(0xFF333333), shape = MaterialTheme.shapes.small) {
                Text(label, modifier = Modifier.padding(ShogiDimensions.PaddingMedium), color = Color.White, fontSize = ShogiDimensions.FontSizeCaption)
            }
        },
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint)
        }
    }
}
