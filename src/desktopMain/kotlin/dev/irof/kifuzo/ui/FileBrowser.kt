package dev.irof.kifuzo.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.ui.theme.ShogiIcons
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItem(
    node: FileTreeNode,
    isSelected: Boolean = false,
    isError: Boolean = false,
    showParentName: Boolean = false,
    onToggle: (FileTreeNode) -> Unit,
    onSelect: (Path) -> Unit,
    onShowText: (Path) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
) {
    val ext = node.path.extension.lowercase()
    val isCsa = ext == "csa"
    val isKifu = ext == "kifu" || ext == "kif" || isCsa

    FileContextMenu(
        node = node,
        isCsa = isCsa,
        isKifu = isKifu,
        onShowText = onShowText,
        onRename = onRename,
        onConvertCsa = onConvertCsa,
    ) {
        FileTreeRow(
            node = node,
            isSelected = isSelected,
            isError = isError,
            showParentName = showParentName,
            onToggle = onToggle,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun FileContextMenu(
    node: FileTreeNode,
    isCsa: Boolean,
    isKifu: Boolean,
    onShowText: (Path) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    content: @Composable () -> Unit,
) {
    ContextMenuArea(items = {
        val menuItems = mutableListOf<ContextMenuItem>()
        if (!node.isDirectory) {
            menuItems.add(ContextMenuItem("テキストを表示") { onShowText(node.path) })
            if (isKifu) {
                menuItems.add(ContextMenuItem("リネーム") { onRename(node.path) })
            }
            if (isCsa) {
                menuItems.add(ContextMenuItem("KIFU形式に変換") { onConvertCsa(node.path) })
            }
        }
        menuItems
    }, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeRow(
    node: FileTreeNode,
    isSelected: Boolean,
    isError: Boolean,
    showParentName: Boolean,
    onToggle: (FileTreeNode) -> Unit,
    onSelect: (Path) -> Unit,
) {
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    val startPadding = if (showParentName) 8.dp else (node.level * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(backgroundColor)
            .combinedClickable(
                onClick = { if (node.isDirectory) onToggle(node) else onSelect(node.path) },
            )
            .padding(start = startPadding, top = 4.dp, bottom = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileTreeIcon(node, isError, showParentName)
        Spacer(Modifier.width(4.dp))
        FileTreeText(node, isError, showParentName)
    }
}

@Composable
private fun FileTreeIcon(node: FileTreeNode, isError: Boolean, showParentName: Boolean) {
    if (node.isDirectory) {
        Icon(
            imageVector = if (node.isExpanded) ShogiIcons.ExpandMore else ShogiIcons.ExpandLess,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Gray,
        )
    } else if (isError) {
        Icon(
            imageVector = ShogiIcons.Warning,
            contentDescription = "解析エラー",
            modifier = Modifier.size(16.dp),
            tint = Color.Red,
        )
    } else {
        Spacer(Modifier.width(if (showParentName) 4.dp else 16.dp))
    }
}

@Composable
private fun FileTreeText(node: FileTreeNode, isError: Boolean, showParentName: Boolean) {
    val displayName = if (showParentName && !node.isDirectory) {
        "${node.path.parent?.name ?: ""}/${node.name}"
    } else {
        node.name + if (node.isDirectory) "/" else ""
    }

    val textColor = when {
        node.isDirectory -> Color.Blue
        isError -> Color.Red
        else -> Color.Black
    }

    Text(
        text = displayName,
        fontSize = 13.sp,
        color = textColor,
        lineHeight = 16.sp,
        softWrap = false,
        maxLines = 1,
    )
}
