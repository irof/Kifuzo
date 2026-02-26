package dev.irof.kifuzo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

private const val ROTATION_UPRIGHT = 0f
private const val ROTATION_UPSIDE_DOWN = 180f
private const val PIECE_FONT_SCALE = 0.55f
private const val COUNT_FONT_SCALE = 0.35f
private const val NAME_FONT_SCALE = 0.45f
private const val KOMADAI_WIDTH_SCALE = 6.5f
private const val KOMADAI_MIN_HEIGHT_SCALE = 0.8f

@Composable
fun KomaDai(
    pieces: List<Piece>,
    isSente: Boolean,
    isFlipped: Boolean,
    cellSize: Dp,
) {
    val comparator = if (isSente) compareByDescending<Piece> { it.mochigomaOrder } else compareBy { it.mochigomaOrder }
    val grouped = pieces.groupBy { it }.mapValues { it.value.size }
        .toSortedMap(comparator)
    val pieceFontSize = (cellSize.value * PIECE_FONT_SCALE).sp
    val countFontSize = (cellSize.value * COUNT_FONT_SCALE).sp

    val rotation = when {
        isFlipped -> if (isSente) ROTATION_UPSIDE_DOWN else ROTATION_UPRIGHT
        else -> if (isSente) ROTATION_UPRIGHT else ROTATION_UPSIDE_DOWN
    }

    Box(
        modifier = Modifier
            .width(cellSize * KOMADAI_WIDTH_SCALE)
            .background(ShogiColors.Board.Background)
            .border(ShogiDimensions.Board.LineThickness, ShogiColors.Board.Line)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .heightIn(min = cellSize * KOMADAI_MIN_HEIGHT_SCALE),
        contentAlignment = if (isSente) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (grouped.isEmpty()) {
                Text(text = " ", fontSize = pieceFontSize)
            } else {
                MochigomaList(grouped, pieceFontSize, countFontSize, rotation)
            }
        }
    }
}

@Composable
private fun MochigomaList(
    grouped: Map<Piece, Int>,
    pieceFontSize: androidx.compose.ui.unit.TextUnit,
    countFontSize: androidx.compose.ui.unit.TextUnit,
    rotation: Float,
) {
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

@Composable
fun PlayerNameLabel(
    name: String,
    isTurn: Boolean,
    cellSize: Dp,
) {
    val fontSize = (cellSize.value * NAME_FONT_SCALE).sp
    val nameColor = if (isTurn) Color.Black else Color.Gray

    Text(
        text = name,
        fontSize = fontSize,
        fontWeight = if (isTurn) FontWeight.Bold else FontWeight.Normal,
        color = nameColor,
        modifier = Modifier.padding(horizontal = ShogiDimensions.Spacing.Medium),
    )
}
