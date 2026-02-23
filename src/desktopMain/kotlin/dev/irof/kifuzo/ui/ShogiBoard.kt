package dev.irof.kifuzo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardLayout
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

private object BoardViewConstants {
    const val BOARD_WIDTH_RATIO = 0.9f
    const val PIECE_FONT_SIZE_RATIO = 0.6f
    const val LABEL_FONT_SIZE_RATIO = 0.3f
    const val FLIP_BUTTON_SIZE_RATIO = 0.6f
    const val MOCHIGOMA_FONT_SIZE_RATIO = 0.45f
    const val MOCHIGOMA_PIECE_FONT_SIZE_RATIO = 0.5f
    const val MOCHIGOMA_COUNT_FONT_SIZE_RATIO = 0.35f
    const val ROTATION_UPSIDE_DOWN = 180f
}

@Composable
fun ShogiBoardView(
    state: ShogiBoardState,
    isFlipped: Boolean = false,
    onToggleFlip: (() -> Unit)? = null,
) {
    val board = state.currentBoard ?: return
    val session = state.session
    val isSenteTurn = state.currentStep % 2 == 0

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingMedium)) {
        val calculatedCellSize = (maxWidth * BoardViewConstants.BOARD_WIDTH_RATIO) / ShogiConstants.BOARD_SIZE
        val cellSize = min(calculatedCellSize, ShogiDimensions.BoardCellMaxSize)
        val fontSize = (cellSize.value * BoardViewConstants.PIECE_FONT_SIZE_RATIO).sp

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isFlipped) {
                MochigomaView(PieceColor.Black.toSymbol() + session.senteName, board.senteMochigoma, isSente = true, isTurn = isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            } else {
                MochigomaView(PieceColor.White.toSymbol() + session.goteName, board.goteMochigoma, isSente = false, isTurn = !isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            }

            Spacer(Modifier.height(ShogiDimensions.PaddingSmall))

            val rangeX = BoardLayout.getRangeX(isFlipped)
            val rangeY = BoardLayout.getRangeY(isFlipped)
            val sujiLabels = BoardLayout.getSujiLabels()
            val danLabels = BoardLayout.getDanLabels()
            val labelSize = (cellSize.value * BoardViewConstants.LABEL_FONT_SIZE_RATIO).sp

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 筋の符号
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (x in rangeX) {
                        Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                            Text(text = sujiLabels[x], fontSize = labelSize, color = Color.Gray)
                        }
                    }
                    // 盤右上の角に反転ボタンを配置
                    Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                        onToggleFlip?.let {
                            IconButton(onClick = it, modifier = Modifier.size(cellSize * BoardViewConstants.FLIP_BUTTON_SIZE_RATIO)) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = AppStrings.FLIP_BOARD,
                                    tint = Color.Gray.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 盤面
                    Column(modifier = Modifier.background(ShogiColors.BoardBackground).border(ShogiDimensions.BoardLineThickness, ShogiColors.BoardLine).padding(ShogiDimensions.BoardPadding)) {
                        for (y in rangeY) {
                            Row {
                                for (x in rangeX) {
                                    val currentSquare = Square.fromIndex(x, y)
                                    val isLastFrom = board.lastFrom == currentSquare
                                    val isLastTo = board.lastTo == currentSquare
                                    Box(
                                        modifier = Modifier.size(cellSize).background(
                                            when {
                                                isLastTo -> ShogiColors.HighlightLastTo
                                                isLastFrom -> ShogiColors.HighlightLastFrom
                                                else -> Color.Transparent
                                            },
                                        ).border(ShogiDimensions.CellBorderThickness, ShogiColors.CellBorder),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        board.cells[y][x]?.let { (piece, color) ->
                                            val isSentePiece = color == PieceColor.Black
                                            val rotation = when {
                                                isFlipped -> if (isSentePiece) BoardViewConstants.ROTATION_UPSIDE_DOWN else 0f
                                                else -> if (isSentePiece) 0f else BoardViewConstants.ROTATION_UPSIDE_DOWN
                                            }
                                            Text(text = piece.symbol, fontSize = fontSize, color = if (piece.isPromoted()) ShogiColors.PiecePromoted else ShogiColors.PieceSente, modifier = Modifier.rotate(rotation))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 段の符号
                    Column {
                        for (y in rangeY) {
                            Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                                Text(text = danLabels[y], fontSize = labelSize, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(ShogiDimensions.PaddingSmall))
            if (isFlipped) {
                MochigomaView(PieceColor.White.toSymbol() + session.goteName, board.goteMochigoma, isSente = false, isTurn = !isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            } else {
                MochigomaView(PieceColor.Black.toSymbol() + session.senteName, board.senteMochigoma, isSente = true, isTurn = isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            }
        }
    }
}

@Composable
fun MochigomaView(name: String, pieces: List<Piece>, isSente: Boolean, isTurn: Boolean, isFlipped: Boolean, cellSize: androidx.compose.ui.unit.Dp) {
    val grouped = pieces.groupBy { it }.mapValues { it.value.size }
        .toSortedMap(compareBy { it.mochigomaOrder })
    val fontSize = (cellSize.value * BoardViewConstants.MOCHIGOMA_FONT_SIZE_RATIO).sp
    val nameColor = if (isTurn) Color.Black else Color.Gray
    Row(modifier = Modifier.fillMaxWidth().background(if (isTurn) ShogiColors.TurnHighlight else Color.Transparent).padding(horizontal = ShogiDimensions.PaddingMedium, vertical = ShogiDimensions.BoardPadding), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (isSente) Arrangement.Start else Arrangement.End) {
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
fun MochigomaList(grouped: Map<Piece, Int>, isSente: Boolean, isFlipped: Boolean, cellSize: androidx.compose.ui.unit.Dp) {
    val pieceFontSize = (cellSize.value * BoardViewConstants.MOCHIGOMA_PIECE_FONT_SIZE_RATIO).sp
    val countFontSize = (cellSize.value * BoardViewConstants.MOCHIGOMA_COUNT_FONT_SIZE_RATIO).sp
    Row {
        grouped.forEach { (piece, count) ->
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = ShogiDimensions.BoardPadding)) {
                val rotation = when {
                    isFlipped -> if (isSente) BoardViewConstants.ROTATION_UPSIDE_DOWN else 0f
                    else -> if (isSente) 0f else BoardViewConstants.ROTATION_UPSIDE_DOWN
                }
                Text(text = piece.symbol, fontSize = pieceFontSize, modifier = Modifier.rotate(rotation))
                if (count > 1) Text(text = count.toString(), fontSize = countFontSize, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
