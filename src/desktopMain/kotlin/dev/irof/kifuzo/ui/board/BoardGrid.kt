package dev.irof.kifuzo.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.Square
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions

private const val ROTATION_UPRIGHT = 0f
private const val ROTATION_UPSIDE_DOWN = 180f

@Composable
fun BoardGrid(
    board: BoardSnapshot,
    rangeX: IntProgression,
    rangeY: IntProgression,
    cellSize: Dp,
    fontSize: TextUnit,
    isFlipped: Boolean,
) {
    Column(
        modifier = Modifier
            .background(ShogiColors.BoardBackground)
            .border(ShogiDimensions.BoardLineThickness, ShogiColors.BoardLine)
            .padding(ShogiDimensions.BoardPadding),
    ) {
        for (y in rangeY) {
            Row {
                for (x in rangeX) {
                    BoardCell(
                        x = x,
                        y = y,
                        board = board,
                        cellSize = cellSize,
                        fontSize = fontSize,
                        isFlipped = isFlipped,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    x: Int,
    y: Int,
    board: BoardSnapshot,
    cellSize: Dp,
    fontSize: TextUnit,
    isFlipped: Boolean,
) {
    val currentSquare = Square.fromIndex(x, y)
    val isLastFrom = board.lastFrom == currentSquare
    val isLastTo = board.lastTo == currentSquare

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(getCellBackgroundColor(isLastFrom, isLastTo))
            .border(ShogiDimensions.CellBorderThickness, ShogiColors.CellBorder),
        contentAlignment = Alignment.Center,
    ) {
        board.cells[y][x]?.let { (piece, color) ->
            PieceView(piece, color, isFlipped, fontSize)
        }
    }
}

private fun getCellBackgroundColor(isLastFrom: Boolean, isLastTo: Boolean): Color = when {
    isLastTo -> ShogiColors.HighlightLastTo
    isLastFrom -> ShogiColors.HighlightLastFrom
    else -> Color.Transparent
}

@Composable
private fun PieceView(
    piece: dev.irof.kifuzo.models.Piece,
    color: PieceColor,
    isFlipped: Boolean,
    fontSize: TextUnit,
) {
    val isSentePiece = color == PieceColor.Black
    val rotation = when {
        isFlipped -> if (isSentePiece) ROTATION_UPSIDE_DOWN else ROTATION_UPRIGHT
        else -> if (isSentePiece) ROTATION_UPRIGHT else ROTATION_UPSIDE_DOWN
    }
    Text(
        text = piece.symbol,
        fontSize = fontSize,
        color = if (piece.isPromoted()) ShogiColors.PiecePromoted else ShogiColors.PieceSente,
        modifier = Modifier.rotate(rotation),
    )
}
