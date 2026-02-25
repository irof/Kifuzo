package dev.irof.kifuzo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
fun KomaDai(
    pieces: List<Piece>,
    isSente: Boolean,
    isFlipped: Boolean,
    cellSize: Dp,
) {
    val grouped = pieces.groupBy { it }.mapValues { it.value.size }
        .toSortedMap(compareBy { it.mochigomaOrder })
    val pieceFontSize = (cellSize.value * 0.55f).sp
    val countFontSize = (cellSize.value * 0.35f).sp

    val rotation = when {
        isFlipped -> if (isSente) 180f else 0f
        else -> if (isSente) 0f else 180f
    }

    Box(
        modifier = Modifier
            .background(ShogiColors.BoardBackground)
            .border(ShogiDimensions.BoardLineThickness, ShogiColors.BoardLine)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .heightIn(min = cellSize * 0.8f),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (grouped.isEmpty()) {
                Text(text = " ", fontSize = pieceFontSize)
            } else {
                grouped.forEach { (piece, count) ->
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        Text(
                            text = piece.symbol,
                            fontSize = pieceFontSize,
                            modifier = Modifier.rotate(rotation),
                        )
                        if (count > 1) {
                            Text(
                                text = count.toString(),
                                fontSize = countFontSize,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerNameLabel(
    name: String,
    isTurn: Boolean,
    cellSize: Dp,
) {
    val fontSize = (cellSize.value * 0.45f).sp
    val nameColor = if (isTurn) Color.Black else Color.Gray

    Text(
        text = name,
        fontSize = fontSize,
        fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal,
        color = nameColor,
        modifier = Modifier.padding(horizontal = ShogiDimensions.PaddingMedium),
    )
}
