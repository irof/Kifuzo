package dev.irof.kfv.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kfv.models.FileTreeNode
import dev.irof.kfv.ui.theme.ShogiIcons
import java.nio.file.Path
import kotlin.io.path.name

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileTreeItem(
    node: FileTreeNode,
    isSelected: Boolean = false,
    onToggle: (FileTreeNode) -> Unit,
    onSelect: (Path) -> Unit,
    onShowText: (Path) -> Unit
) {
    ContextMenuArea(items = {
        if (!node.isDirectory) {
            listOf(ContextMenuItem("テキストを表示") { onShowText(node.path) })
        } else emptyList()
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                .combinedClickable(
                    onClick = { 
                        if (node.isDirectory) onToggle(node)
                        else onSelect(node.path)
                    }
                )
                .padding(start = (node.level * 16).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                Icon(
                    imageVector = if (node.isExpanded) ShogiIcons.ExpandMore else ShogiIcons.ExpandLess,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }
            
            Spacer(Modifier.width(4.dp))
            
            Text(
                text = node.name + if (node.isDirectory) "/" else "",
                fontSize = 13.sp,
                color = if (node.isDirectory) Color.Blue else Color.Black,
                lineHeight = 16.sp,
                softWrap = false,
                maxLines = 1
            )
        }
    }
}
