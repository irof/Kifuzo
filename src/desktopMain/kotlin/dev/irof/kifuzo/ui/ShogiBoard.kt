package dev.irof.kifuzo.ui

import androidx.compose.foundation.layout.Arrangement
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
import dev.irof.kifuzo.ui.board.KomaDai
import dev.irof.kifuzo.ui.board.PlayerNameLabel
import dev.irof.kifuzo.ui.board.SujiLabels
import dev.irof.kifuzo.ui.theme.ShogiDimensions

private object BoardViewConstants {
    const val LABEL_FONT_SIZE_RATIO = 0.3f
    const val PIECE_FONT_SIZE_RATIO = 0.6f
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
        // 盤面(9) + 段符号(0.5) + 少し余裕 = 11. 11で割る
        val cellSize = min(maxWidth / 11f, ShogiDimensions.BoardCellMaxSize)
        val fontSize = (cellSize.value * BoardViewConstants.PIECE_FONT_SIZE_RATIO).sp
        val labelSize = (cellSize.value * BoardViewConstants.LABEL_FONT_SIZE_RATIO).sp

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val rangeX = BoardLayout.getRangeX(isFlipped)
            val rangeY = BoardLayout.getRangeY(isFlipped)

            // 上段: 後手(上手)の駒台 ※反転時は先手
            Row(
                modifier = Modifier.widthInBoard(cellSize),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFlipped) {
                    KomaDai(board.senteMochigoma, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
                } else {
                    KomaDai(board.goteMochigoma, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
                }
            }

            Spacer(Modifier.height(ShogiDimensions.PaddingSmall))

            // 盤面本体
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SujiLabels(rangeX, cellSize, labelSize, onToggleFlip)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoardGrid(board, rangeX, rangeY, cellSize, fontSize, isFlipped)
                    DanLabels(rangeY, cellSize, labelSize)
                }
            }

            Spacer(Modifier.height(ShogiDimensions.PaddingSmall))

            // 下段: 先手(下手)の駒台 ※反転時は後手
            Row(
                modifier = Modifier.widthInBoard(cellSize),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFlipped) {
                    KomaDai(board.goteMochigoma, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
                } else {
                    KomaDai(board.senteMochigoma, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
                }
            }
        }
    }
}

private fun Modifier.widthInBoard(cellSize: androidx.compose.ui.unit.Dp): Modifier = this.padding(horizontal = cellSize * 0.5f).fillMaxWidth(0.9f)
