package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path
import kotlin.io.path.extension

@Composable
fun KifuHeaderActions(
    selectedFile: Path,
    history: List<BoardSnapshot>,
    isMoveListVisible: Boolean,
    onToggleMoveList: () -> Unit,
) {
    val ext = selectedFile.extension.lowercase()
    val hasHistory = history.isNotEmpty()

    if (!hasHistory && ext != "csa") return

    Row(
        modifier = Modifier.padding(horizontal = ShogiDimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        IconButton(
            onClick = onToggleMoveList,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = ShogiIcons.SidebarToggle,
                contentDescription = if (isMoveListVisible) "手順を隠す" else "手順を表示",
                tint = if (isMoveListVisible) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
    }

    Spacer(Modifier.height(ShogiDimensions.PaddingMedium))
}
