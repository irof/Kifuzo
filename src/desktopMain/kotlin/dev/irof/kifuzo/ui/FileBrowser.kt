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
) {
    ContextMenuArea(items = {
        if (!node.isDirectory) {
            listOf(ContextMenuItem("テキストを表示") { onShowText(node.path) })
        } else {
            emptyList()
        }
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                .combinedClickable(
                    onClick = {
                        if (node.isDirectory) {
                            onToggle(node)
                        } else {
                            onSelect(node.path)
                        }
                    },
                )
                .padding(start = if (showParentName) 8.dp else (node.level * 16).dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (node.isDirectory) {
                Icon(
                    imageVector = if (node.isExpanded) ShogiIcons.ExpandMore else ShogiIcons.ExpandLess,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray,
                )
            } else {
                if (isError) {
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

            Spacer(Modifier.width(4.dp))

            val displayName = if (showParentName && !node.isDirectory) {
                "${node.path.parent?.name ?: ""}/${node.name}"
            } else {
                node.name + if (node.isDirectory) "/" else ""
            }

            Text(
                text = displayName,
                fontSize = 13.sp,
                color = if (node.isDirectory) {
                    Color.Blue
                } else if (isError) {
                    Color.Red
                } else {
                    Color.Black
                },
                lineHeight = 16.sp,
                softWrap = false,
                maxLines = 1,
            )
        }
    }
}
