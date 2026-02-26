package dev.irof.kifuzo.ui.sidebar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun FileViewModeSelector(
    currentMode: FileViewMode,
    onSetViewMode: (FileViewMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .border(ShogiDimensions.Board.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.small)
                .padding(ShogiDimensions.Board.Padding),
        ) {
            ViewModeButton(
                label = AppStrings.HIERARCHY,
                isSelected = currentMode == FileViewMode.HIERARCHY,
                onClick = { onSetViewMode(FileViewMode.HIERARCHY) },
            )
            ViewModeButton(
                label = AppStrings.FLAT,
                isSelected = currentMode == FileViewMode.FLAT,
                onClick = { onSetViewMode(FileViewMode.FLAT) },
            )
        }
    }
}

@Composable
private fun ViewModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) ShogiColors.Panel.Primary else Color.Transparent,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            fontSize = ShogiDimensions.Text.Small,
            color = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.padding(horizontal = ShogiDimensions.Spacing.Medium, vertical = ShogiDimensions.Spacing.Small),
        )
    }
}
