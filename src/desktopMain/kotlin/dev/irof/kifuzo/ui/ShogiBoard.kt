package dev.irof.kifuzo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardLayout
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.ui.board.BoardGrid
import dev.irof.kifuzo.ui.board.DanLabels
import dev.irof.kifuzo.ui.board.MochigomaView
import dev.irof.kifuzo.ui.board.SujiLabels
import dev.irof.kifuzo.ui.theme.ShogiDimensions

private object BoardViewConstants {
    const val BOARD_WIDTH_RATIO = 0.9f
    const val PIECE_FONT_SIZE_RATIO = 0.6f
    const val LABEL_FONT_SIZE_RATIO = 0.3f
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

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingMedium),
    ) {
        val calculatedCellSize = (maxWidth * BoardViewConstants.BOARD_WIDTH_RATIO) / ShogiConstants.BOARD_SIZE
        val cellSize = min(calculatedCellSize, ShogiDimensions.BoardCellMaxSize)
        val fontSize = (cellSize.value * BoardViewConstants.PIECE_FONT_SIZE_RATIO).sp
        val labelSize = (cellSize.value * BoardViewConstants.LABEL_FONT_SIZE_RATIO).sp

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // 上段の持駒
            if (isFlipped) {
                MochigomaView(PieceColor.Black.toSymbol() + session.senteName, board.senteMochigoma, isSente = true, isTurn = isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            } else {
                MochigomaView(PieceColor.White.toSymbol() + session.goteName, board.goteMochigoma, isSente = false, isTurn = !isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            }

            Spacer(Modifier.height(ShogiDimensions.PaddingSmall))

            val rangeX = BoardLayout.getRangeX(isFlipped)
            val rangeY = BoardLayout.getRangeY(isFlipped)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 筋の符号
                SujiLabels(rangeX, cellSize, labelSize, onToggleFlip)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 盤面
                    BoardGrid(board, rangeX, rangeY, cellSize, fontSize, isFlipped)

                    // 段の符号
                    DanLabels(rangeY, cellSize, labelSize)
                }
            }

            Spacer(Modifier.height(ShogiDimensions.PaddingSmall))

            // 下段の持駒
            if (isFlipped) {
                MochigomaView(PieceColor.White.toSymbol() + session.goteName, board.goteMochigoma, isSente = false, isTurn = !isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            } else {
                MochigomaView(PieceColor.Black.toSymbol() + session.senteName, board.senteMochigoma, isSente = true, isTurn = isSenteTurn, isFlipped = isFlipped, cellSize = cellSize)
            }
        }
    }
}
