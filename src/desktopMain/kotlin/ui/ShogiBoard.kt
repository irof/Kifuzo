package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.min
import models.Piece
import models.ShogiBoardState

@Composable
fun ShogiBoardView(state: ShogiBoardState) {
    val boardColor = Color(0xFFF3C077)
    val board = state.currentBoard ?: return
    
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        // 利用可能な幅に基づいてマスのサイズを計算（最大60dp）
        val calculatedCellSize = (maxWidth * 0.9f) / 9
        val cellSize = min(calculatedCellSize, 60.dp)
        val fontSize = (cellSize.value * 0.6f).sp

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            MochigomaView(state.goteName, board.goteMochigoma, isSente = false, cellSize = cellSize)
            Spacer(Modifier.height(4.dp))
            
            // 盤面
            Column(modifier = Modifier.background(boardColor).border(1.5.dp, Color.Black).padding(2.dp)) {
                for (y in 0..8) {
                    Row {
                        for (x in 0..8) {
                            val isLastFrom = board.lastFrom?.let { it.first == x && it.second == y } ?: false
                            val isLastTo = board.lastTo?.let { it.first == x && it.second == y } ?: false
                            
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .background(
                                        when {
                                            isLastTo -> Color(0xFFFFD54F).copy(alpha = 0.7f) // 移動先
                                            isLastFrom -> Color(0xFFFFE082).copy(alpha = 0.5f) // 移動元
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(0.5.dp, Color.Gray.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                board.cells[y][x]?.let { (piece, isSente) ->
                                    Text(
                                        text = piece.symbol, 
                                        fontSize = fontSize, 
                                        color = if (piece.isPromoted()) Color.Red else Color.Black,
                                        modifier = if (!isSente) Modifier.rotate(180f) else Modifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(4.dp))
            MochigomaView(state.senteName, board.senteMochigoma, isSente = true, cellSize = cellSize)
        }
    }
}

@Composable
fun MochigomaView(name: String, pieces: List<Piece>, isSente: Boolean, cellSize: androidx.compose.ui.unit.Dp) {
    val grouped = pieces.groupBy { it }.mapValues { it.value.size }
    val fontSize = (cellSize.value * 0.4f).sp
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isSente) Arrangement.Start else Arrangement.End
    ) {
        if (!isSente) {
            MochigomaList(grouped, isSente, cellSize)
            Spacer(Modifier.width(12.dp))
            Text(name, fontSize = fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        } else {
            Text(name, fontSize = fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            MochigomaList(grouped, isSente, cellSize)
        }
    }
}

@Composable
fun MochigomaList(grouped: Map<Piece, Int>, isSente: Boolean, cellSize: androidx.compose.ui.unit.Dp) {
    val pieceFontSize = (cellSize.value * 0.5f).sp
    val countFontSize = (cellSize.value * 0.3f).sp
    
    Row {
        grouped.forEach { (piece, count) ->
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 2.dp)) {
                Text(
                    text = piece.symbol, 
                    fontSize = pieceFontSize, 
                    modifier = if (!isSente) Modifier.rotate(180f) else Modifier
                )
                if (count > 1) {
                    Text(text = count.toString(), fontSize = countFontSize, color = Color.Gray)
                }
            }
        }
    }
}
