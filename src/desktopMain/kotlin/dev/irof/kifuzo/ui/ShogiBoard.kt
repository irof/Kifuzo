package dev.irof.kifuzo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardLayout
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.ui.board.BoardGrid
import dev.irof.kifuzo.ui.board.DanLabels
import dev.irof.kifuzo.ui.board.KomaDai
import dev.irof.kifuzo.ui.board.PlayerNameLabel
import dev.irof.kifuzo.ui.board.SujiLabels
import dev.irof.kifuzo.ui.theme.ShogiDimensions

private object BoardViewConstants {
    const val LABEL_FONT_SIZE_RATIO = 0.3f
    const val PIECE_FONT_SIZE_RATIO = 0.6f
    const val CELL_COUNT_FOR_WIDTH = 11f
    const val CELL_COUNT_FOR_HEIGHT = 13f
    const val BOARD_WIDTH_CELLS = 10
}

/**
 * 将棋盤ビュー（盤面、駒台、対局者情報の表示）。
 *
 * 9x9の盤面、先手・後手の駒台、対局者名、および盤面反転ボタンを
 * 描画し、指定された局面の状態を視覚化します。
 */
@Composable
fun ShogiBoardView(
    state: ShogiBoardState,
    isFlipped: Boolean = false,
    onToggleFlip: (() -> Unit)? = null,
) {
    val board = state.currentBoard
    val session = state.session
    val isSenteTurn = state.currentStep % 2 == 0

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.Spacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        // 縦横どちらかが小さくても収まるようにcellSizeを決定する
        val cellSizeWidth = maxWidth / BoardViewConstants.CELL_COUNT_FOR_WIDTH
        val cellSizeHeight = maxHeight / BoardViewConstants.CELL_COUNT_FOR_HEIGHT
        val cellSize = min(min(cellSizeWidth, cellSizeHeight), ShogiDimensions.Board.CellMaxSize)
        val fontSize = (cellSize.value * BoardViewConstants.PIECE_FONT_SIZE_RATIO).sp
        val labelSize = (cellSize.value * BoardViewConstants.LABEL_FONT_SIZE_RATIO).sp
        val boardWidth = cellSize * BoardViewConstants.BOARD_WIDTH_CELLS

        Column(
            modifier = Modifier.width(boardWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val rangeX = BoardLayout.getRangeX(isFlipped)
            val rangeY = BoardLayout.getRangeY(isFlipped)

            // 上段: 後手(上手)の駒台 ※反転時は先手
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFlipped) {
                    KomaDai(board.senteMochigoma, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
                    PlayerNameLabel("先手", isTurn = isSenteTurn, cellSize = cellSize)
                } else {
                    KomaDai(board.goteMochigoma, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
                    PlayerNameLabel("後手", isTurn = !isSenteTurn, cellSize = cellSize)
                }
            }

            Spacer(Modifier.height(ShogiDimensions.Spacing.Small))

            // 盤面本体
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SujiLabels(rangeX, cellSize, labelSize, onToggleFlip)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoardGrid(board, rangeX, rangeY, cellSize, fontSize, isFlipped)
                    DanLabels(rangeY, cellSize, labelSize)
                }
            }

            Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))

            // 下段: 先手(下手)の駒台 ※反転時は後手
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isFlipped) {
                    PlayerNameLabel("後手", isTurn = !isSenteTurn, cellSize = cellSize)
                    KomaDai(board.goteMochigoma, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
                } else {
                    PlayerNameLabel("先手", isTurn = isSenteTurn, cellSize = cellSize)
                    KomaDai(board.senteMochigoma, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
                }
            }
        }
    }
}
