package dev.irof.kfv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.irof.kfv.models.*
import dev.irof.kfv.ui.theme.ShogiColors

@Composable
fun ShogiBoardView(state: ShogiBoardState, isFlipped: Boolean = false) {
    val board = state.currentBoard ?: return
    val session = state.session
    val isSenteTurn = state.currentStep % 2 == 0

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val calculatedCellSize = (maxWidth * 0.9f) / 9
        val cellSize = min(calculatedCellSize, 60.dp)
        val fontSize = (cellSize.value * 0.6f).sp

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            if (isFlipped) {
                MochigomaView(PieceColor.Black.toSymbol() + session.senteName, board.senteMochigoma, isSente = true, isTurn = isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            } else {
                MochigomaView(PieceColor.White.toSymbol() + session.goteName, board.goteMochigoma, isSente = false, isTurn = !isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            }

            Spacer(Modifier.height(4.dp))

            val rangeX = BoardLayout.getRangeX(isFlipped)
            val rangeY = BoardLayout.getRangeY(isFlipped)
            val sujiLabels = BoardLayout.getSujiLabels()
            val danLabels = BoardLayout.getDanLabels()
            val labelSize = (cellSize.value * 0.3f).sp

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 筋の符号
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (x in rangeX) {
                        Box(modifier = Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                            Text(text = sujiLabels[x], fontSize = labelSize, color = Color.Gray)
                        }
                    }
                    // 段の符号の幅分を空ける
                    Spacer(Modifier.width(cellSize))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 盤面
                    Column(modifier = Modifier.background(ShogiColors.BoardBackground).border(1.5.dp, ShogiColors.BoardLine).padding(2.dp)) {
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
                                        ).border(0.5.dp, ShogiColors.CellBorder),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        board.cells[y][x]?.let { (piece, color) ->
                                            val isSentePiece = color == PieceColor.Black
                                            val rotation = when {
                                                isFlipped -> if (isSentePiece) 180f else 0f
                                                else -> if (isSentePiece) 0f else 180f
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

            Spacer(Modifier.height(4.dp))
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
    val fontSize = (cellSize.value * 0.45f).sp
    val nameColor = if (isTurn) Color.Black else Color.Gray
    Row(modifier = Modifier.fillMaxWidth().background(if (isTurn) ShogiColors.TurnHighlight else Color.Transparent).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (isSente) Arrangement.Start else Arrangement.End) {
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
    val pieceFontSize = (cellSize.value * 0.5f).sp
    val countFontSize = (cellSize.value * 0.35f).sp
    Row {
        grouped.forEach { (piece, count) ->
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 2.dp)) {
                val rotation = when {
                    isFlipped -> if (isSente) 180f else 0f
                    else -> if (isSente) 0f else 180f
                }
                Text(text = piece.symbol, fontSize = pieceFontSize, modifier = Modifier.rotate(rotation))
                if (count > 1) Text(text = count.toString(), fontSize = countFontSize, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
