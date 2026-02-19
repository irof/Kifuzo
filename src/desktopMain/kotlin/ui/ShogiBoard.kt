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
import models.Piece
import models.ShogiBoardState

@Composable
fun ShogiBoardView(state: ShogiBoardState) {
    val boardColor = Color(0xFFF3C077); val cellSize = 40.dp
    val board = state.currentBoard ?: return
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MochigomaView(state.goteName, board.goteMochigoma, isSente = false)
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.background(boardColor).border(1.5.dp, Color.Black).padding(2.dp)) {
            for (y in 0..8) { Row { for (x in 0..8) {
                Box(modifier = Modifier.size(cellSize).border(0.5.dp, Color.Gray), contentAlignment = Alignment.Center) {
                    board.cells[y][x]?.let { (piece, isSente) ->
                        Text(
                            text = piece.symbol, 
                            fontSize = 22.sp, 
                            color = if (piece.isPromoted()) Color.Red else Color.Black,
                            modifier = if (!isSente) Modifier.rotate(180f) else Modifier
                        )
                    }
                }
            } } }
        }
        Spacer(Modifier.height(8.dp))
        MochigomaView(state.senteName, board.senteMochigoma, isSente = true)
    }
}

@Composable
fun MochigomaView(name: String, pieces: List<Piece>, isSente: Boolean) {
    val grouped = pieces.groupBy { it }.mapValues { it.value.size }
    Row(
        modifier = Modifier.width(380.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isSente) Arrangement.Start else Arrangement.End
    ) {
        if (!isSente) {
            MochigomaList(grouped, isSente)
            Spacer(Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.subtitle2)
        } else {
            Text(name, style = MaterialTheme.typography.subtitle2)
            Spacer(Modifier.width(8.dp))
            MochigomaList(grouped, isSente)
        }
    }
}

@Composable
fun MochigomaList(grouped: Map<Piece, Int>, isSente: Boolean) {
    Row {
        grouped.forEach { (piece, count) ->
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(horizontal = 2.dp)) {
                Text(text = piece.symbol, fontSize = 18.sp, modifier = if (!isSente) Modifier.rotate(180f) else Modifier)
                if (count > 1) { Text(text = count.toString(), fontSize = 12.sp, color = Color.Gray) }
            }
        }
    }
}
