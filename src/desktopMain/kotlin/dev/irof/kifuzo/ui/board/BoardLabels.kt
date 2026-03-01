package dev.irof.kifuzo.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.irof.kifuzo.models.BoardLayout
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings

private object LabelConstants {
    const val FLIP_BUTTON_SIZE_RATIO = 0.6f
    const val FLIP_BUTTON_ALPHA = 0.7f
}

@Composable
fun SujiLabels(
    rangeX: IntProgression,
    cellSize: Dp,
    labelSize: TextUnit,
    onToggleFlip: (() -> Unit)? = null,
) {
    val sujiLabels = BoardLayout.getSujiLabels()
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (x in rangeX) {
            Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                Text(text = sujiLabels[x], fontSize = labelSize, color = Color.Gray)
            }
        }
        // 盤右上の角に反転ボタンを配置
        Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
            onToggleFlip?.let {
                IconButton(onClick = it, modifier = Modifier.size(cellSize * LabelConstants.FLIP_BUTTON_SIZE_RATIO)) {
                    Icon(
                        imageVector = ShogiIcons.Refresh,
                        contentDescription = AppStrings.FLIP_BOARD,
                        tint = Color.Gray.copy(alpha = LabelConstants.FLIP_BUTTON_ALPHA),
                    )
                }
            }
        }
    }
}

@Composable
fun DanLabels(
    rangeY: IntProgression,
    cellSize: Dp,
    labelSize: TextUnit,
) {
    val danLabels = BoardLayout.getDanLabels()
    Column {
        for (y in rangeY) {
            Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                Text(text = danLabels[y], fontSize = labelSize, color = Color.Gray)
            }
        }
    }
}
