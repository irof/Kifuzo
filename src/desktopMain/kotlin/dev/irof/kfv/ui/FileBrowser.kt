package dev.irof.kfv.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileEntryItem(
    path: Path,
    isParentLink: Boolean = false,
    isSelected: Boolean = false,
    onNavigate: (Path) -> Unit,
    onSelect: (Path) -> Unit,
    onShowText: (Path) -> Unit
) {
    val isDirectory = path.isDirectory() || isParentLink
    ContextMenuArea(items = {
        if (!isDirectory && !isParentLink) {
            listOf(ContextMenuItem("テキストを表示") { onShowText(path) })
        } else emptyList()
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
                .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                .combinedClickable(onClick = { if (!isDirectory) onSelect(path) }, onDoubleClick = { onNavigate(path) })
                .padding(6.dp)
        ) {
            Text(
                text = if (isParentLink) ".." else path.name + if (path.isDirectory()) "/" else "",
                fontSize = 13.sp,
                color = if (isDirectory) Color.Blue else Color.Black,
                lineHeight = 16.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}
