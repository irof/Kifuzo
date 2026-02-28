package dev.irof.kifuzo.ui.sidebar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun FileFiltersAndSorting(
    currentFilters: Set<FileFilter>,
    currentSort: FileSortOption,
    onToggleFilter: (FileFilter) -> Unit,
    onSetSort: (FileSortOption) -> Unit,
) {
    Column {
        // フィルタ選択
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = ShogiDimensions.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                label = AppStrings.FILTER_RECENT,
                isSelected = currentFilters.contains(FileFilter.RECENT),
                onClick = { onToggleFilter(FileFilter.RECENT) },
            )
        }

        // ソート順
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = ShogiDimensions.Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                label = AppStrings.SORT_NAME,
                isSelected = currentSort == FileSortOption.NAME,
                onClick = { onSetSort(FileSortOption.NAME) },
            )
            Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
            FilterChip(
                label = AppStrings.SORT_DATE,
                isSelected = currentSort == FileSortOption.LAST_MODIFIED,
                onClick = { onSetSort(FileSortOption.LAST_MODIFIED) },
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) ShogiColors.Panel.Primary.copy(alpha = ShogiColors.Panel.SELECTED_BACKGROUND_ALPHA) else Color.Transparent,
        border = BorderStroke(ShogiDimensions.Board.CellBorderThickness, if (isSelected) ShogiColors.Panel.Primary else Color.LightGray),
        shape = CircleShape,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            fontSize = ShogiDimensions.Text.Caption,
            color = if (isSelected) ShogiColors.Panel.Primary else Color.Gray,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = ShogiDimensions.Spacing.Small),
        )
    }
}
