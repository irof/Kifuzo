package dev.irof.kifuzo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions

@Composable
fun MochigomaView(
    name: String,
    pieces: List<Piece>,
    isSente: Boolean,
    isTurn: Boolean,
    isFlipped: Boolean,
    cellSize: Dp,
) {
    val grouped = pieces.groupBy { it }.mapValues { it.value.size }
        .toSortedMap(compareBy { it.mochigomaOrder })
    val fontSize = (cellSize.value * 0.45f).sp
    val nameColor = if (isTurn) Color.Black else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isTurn) ShogiColors.TurnHighlight else Color.Transparent)
            .padding(horizontal = ShogiDimensions.PaddingMedium, vertical = ShogiDimensions.BoardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isSente) Arrangement.Start else Arrangement.End,
    ) {
        if (!isSente) {
            MochigomaList(grouped, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
            Spacer(Modifier.width(12.dp))
            Text(text = name, fontSize = fontSize, fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal, color = nameColor)
        } else {
            Text(text = name, fontSize = fontSize, fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal, color = nameColor)
            Spacer(Modifier.width(12.dp))
            MochigomaList(grouped, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
        }
    }
}

@Composable
private fun MochigomaList(
    grouped: Map<Piece, Int>,
    isSente: Boolean,
    isFlipped: Boolean,
    cellSize: Dp,
) {
    val pieceFontSize = (cellSize.value * 0.5f).sp
    val countFontSize = (cellSize.value * 0.35f).sp
    Row {
        grouped.forEach { (piece, count) ->
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = ShogiDimensions.BoardPadding)) {
                val rotation = when {
                    isFlipped -> if (isSente) 180f else 0f
                    else -> if (isSente) 0f else 180f
                }
                Text(text = piece.symbol, fontSize = pieceFontSize, modifier = Modifier.rotate(rotation))
                if (count > 1) {
                    Text(text = count.toString(), fontSize = countFontSize, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
