package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.logic.readTextWithEncoding
import dev.irof.kifuzo.ui.FileTreeItem
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoAction
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlin.io.path.toPath

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KifuSidebar(
    state: KifuzoUiState,
    currentRoot: Path?,
    onSetRoot: (Path) -> Unit,
    onRefresh: () -> Unit,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
    onSelectFile: (Path) -> Unit,
    onShowText: (String) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onSetViewMode: (dev.irof.kifuzo.models.FileViewMode) -> Unit,
    onSetFileSortOption: (dev.irof.kifuzo.models.FileSortOption) -> Unit,
    onToggleFileFilter: (dev.irof.kifuzo.models.FileFilter) -> Unit,
    onSelectNext: () -> Unit,
    onSelectPrev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(ShogiDimensions.PaddingLarge)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        onSelectNext()
                        true
                    }
                    Key.DirectionUp -> {
                        onSelectPrev()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { focusRequester.requestFocus() }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            // --- 表示モード切替 ---
            Row(modifier = Modifier.border(ShogiDimensions.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.small).padding(ShogiDimensions.BoardPadding)) {
                val mode = state.viewMode
                ViewModeButton(AppStrings.HIERARCHY, mode == dev.irof.kifuzo.models.FileViewMode.HIERARCHY) { onSetViewMode(dev.irof.kifuzo.models.FileViewMode.HIERARCHY) }
                ViewModeButton(AppStrings.FLAT, mode == dev.irof.kifuzo.models.FileViewMode.FLAT) { onSetViewMode(dev.irof.kifuzo.models.FileViewMode.FLAT) }
            }
        }

        Spacer(Modifier.height(ShogiDimensions.PaddingMedium))

        // --- フィルタ選択 ---
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = ShogiDimensions.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
            val currentFilters = state.fileFilters
            FilterChip(AppStrings.FILTER_KIFU, currentFilters.contains(dev.irof.kifuzo.models.FileFilter.KIFU_ONLY)) { onToggleFileFilter(dev.irof.kifuzo.models.FileFilter.KIFU_ONLY) }
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            FilterChip(AppStrings.FILTER_RECENT, currentFilters.contains(dev.irof.kifuzo.models.FileFilter.RECENT)) { onToggleFileFilter(dev.irof.kifuzo.models.FileFilter.RECENT) }
        }

        // --- ソート順 ---
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = ShogiDimensions.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
            val currentSort = state.fileSortOption
            FilterChip(AppStrings.SORT_NAME, currentSort == dev.irof.kifuzo.models.FileSortOption.NAME) { onSetFileSortOption(dev.irof.kifuzo.models.FileSortOption.NAME) }
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            FilterChip(AppStrings.SORT_DATE, currentSort == dev.irof.kifuzo.models.FileSortOption.LAST_MODIFIED) { onSetFileSortOption(dev.irof.kifuzo.models.FileSortOption.LAST_MODIFIED) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f).clickable {
                    val chooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        val initialDir = currentRoot?.toFile() ?: dev.irof.kifuzo.models.AppConfig.USER_HOME_PATH.toFile()
                        currentDirectory = initialDir
                    }
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        onSetRoot(chooser.selectedFile.toPath())
                    }
                },
                elevation = 0.dp,
                backgroundColor = Color.White,
                border = BorderStroke(ShogiDimensions.CellBorderThickness, Color.LightGray),
            ) {
                Row(modifier = Modifier.padding(ShogiDimensions.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
                    Icon(ShogiIcons.FolderSelect, contentDescription = null, tint = ShogiColors.Primary, modifier = Modifier.size(ShogiDimensions.IconSizeSmall))
                    Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
                    Text(text = currentRoot?.toString() ?: AppStrings.SELECT_KIFU_ROOT, fontSize = ShogiDimensions.FontSizeCaption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = if (currentRoot == null) Color.Gray else Color.Black)
                }
            }

            if (currentRoot != null) {
                Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
                IconButton(onClick = onRefresh, modifier = Modifier.size(ShogiDimensions.ButtonHeight)) {
                    Icon(ShogiIcons.Refresh, contentDescription = "再読み込み", tint = ShogiColors.Primary)
                }
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
                        showParentName = (state.viewMode == dev.irof.kifuzo.models.FileViewMode.FLAT),
                        onToggle = onToggleDir,
                        onSelect = onSelectFile,
                        onShowText = { onShowText(readTextWithEncoding(it)) },
                        onRename = onRename,
                        onConvertCsa = onConvertCsa,
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
            fontSize = ShogiDimensions.FontSizeSmall,
            color = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.padding(horizontal = ShogiDimensions.PaddingMedium, vertical = ShogiDimensions.PaddingSmall),
        )
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) ShogiColors.Primary.copy(alpha = ShogiDimensions.CHIP_SELECTED_ALPHA) else Color.Transparent,
        border = BorderStroke(ShogiDimensions.CellBorderThickness, if (isSelected) ShogiColors.Primary else Color.LightGray),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            fontSize = ShogiDimensions.FontSizeCaption,
            color = if (isSelected) ShogiColors.Primary else Color.Gray,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = ShogiDimensions.PaddingSmall),
        )
    }
}
