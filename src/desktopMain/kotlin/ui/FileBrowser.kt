package ui

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
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileEntryItem(
    file: File,
    isParentLink: Boolean = false,
    isSelected: Boolean = false,
    onNavigate: (File) -> Unit,
    onSelect: (File) -> Unit
) {
    val isDirectory = file.isDirectory || isParentLink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (!isDirectory) onSelect(file) },
                onDoubleClick = { onNavigate(file) }
            )
            .padding(6.dp)
    ) {
        Text(
            text = if (isParentLink) ".." else file.name + if (file.isDirectory) "/" else "",
            fontSize = 13.sp,
            color = if (isDirectory) Color.Blue else Color.Black
        )
    }
}
