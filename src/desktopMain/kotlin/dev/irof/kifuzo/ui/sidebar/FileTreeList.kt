package dev.irof.kifuzo.ui.sidebar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path

@Composable
fun FileTreeList(
    state: KifuzoUiState,
    onToggleDir: (FileTreeNode) -> Unit,
    onSelectFile: (Path) -> Unit,
    onShowText: (String) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onForceParse: (Path) -> Unit,
) {
    val treeHorizontalScroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(treeHorizontalScroll),
    ) {
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            items(state.treeNodes) { node ->
                FileTreeItem(
                    node = node,
                    isSelected = (node.path == state.selectedFile),
                    isError = state.kifuInfos[node.path]?.isError ?: false,
                    showParentName = (state.viewMode == FileViewMode.FLAT),
                    onToggle = onToggleDir,
                    onSelect = onSelectFile,
                    onShowText = { onShowText(dev.irof.kifuzo.logic.io.readTextWithEncoding(it)) },
                    onRename = onRename,
                    onConvertCsa = onConvertCsa,
                    onForceParse = onForceParse,
                )
            }
        }
    }
}
