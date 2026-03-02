package dev.irof.kifuzo.ui.sidebar

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings
import java.nio.file.Path
import kotlin.io.path.extension

private object FileTreeConstants {
    const val INDENT_SIZE = 16
    const val DIRECTORY_ICON_TINT = 0xFFFFA000
    val LABEL_FONT_SIZE = 13.sp
    val LABEL_LINE_HEIGHT = 16.sp
}

@Composable
fun FileTreeItem(
    node: FileTreeNode,
    isSelected: Boolean,
    isError: Boolean,
    showParentName: Boolean,
    onToggle: (FileTreeNode) -> Unit,
    onSelect: (Path) -> Unit,
    onShowText: (Path) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onForceParse: (Path) -> Unit,
) {
    val backgroundColor = if (isSelected) ShogiColors.Panel.Primary.copy(alpha = ShogiColors.Panel.SELECTED_BACKGROUND_ALPHA) else Color.Transparent
    val startPadding = if (showParentName) ShogiDimensions.Spacing.Medium else (node.level * FileTreeConstants.INDENT_SIZE).dp

    ContextMenuArea(items = {
        buildContextMenuItems(node, onShowText, onRename, onConvertCsa, onForceParse)
    }) {
        FileTreeRow(node, isError, showParentName, backgroundColor, startPadding, onToggle, onSelect)
    }
}

@Composable
private fun FileTreeRow(
    node: FileTreeNode,
    isError: Boolean,
    showParentName: Boolean,
    backgroundColor: Color,
    startPadding: Dp,
    onToggle: (FileTreeNode) -> Unit,
    onSelect: (Path) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { if (node.isDirectory) onToggle(node) else onSelect(node.path) }
            .padding(vertical = 4.dp)
            .padding(start = startPadding, end = ShogiDimensions.Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (node.isDirectory) ShogiIcons.Directory else ShogiIcons.File,
            contentDescription = null,
            modifier = Modifier.size(ShogiDimensions.Icon.Small),
            tint = if (node.isDirectory) Color(FileTreeConstants.DIRECTORY_ICON_TINT) else Color.Gray,
        )
        Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
        FileLabel(node, isError, showParentName)
    }
}

private fun buildContextMenuItems(
    node: FileTreeNode,
    onShowText: (Path) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onForceParse: (Path) -> Unit,
): List<ContextMenuItem> {
    val items = mutableListOf<ContextMenuItem>()
    if (!node.isDirectory) {
        items.add(ContextMenuItem("テキストを表示") { onShowText(node.path) })
        items.add(ContextMenuItem("ファイル名を変更") { onRename(node.path) })
        items.add(ContextMenuItem(AppStrings.FORCE_PARSE_KIFU) { onForceParse(node.path) })
        if (node.path.extension.lowercase() == "csa") {
            items.add(ContextMenuItem("KIFに変換") { onConvertCsa(node.path) })
        }
    }
    return items
}

@Composable
private fun FileLabel(node: FileTreeNode, isError: Boolean, showParentName: Boolean) {
    val displayName = if (showParentName) {
        val parent = node.path.parent?.fileName?.toString() ?: ""
        if (parent.isNotEmpty()) "[$parent] ${node.name}" else node.name
    } else {
        node.name
    }

    Text(
        text = displayName,
        fontSize = FileTreeConstants.LABEL_FONT_SIZE,
        color = if (isError) Color.Red else Color.Black,
        lineHeight = FileTreeConstants.LABEL_LINE_HEIGHT,
        softWrap = false,
        maxLines = 1,
    )
}
