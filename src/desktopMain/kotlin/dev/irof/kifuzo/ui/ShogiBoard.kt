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
        // 盤面(9) + 駒台(2.2*2) + 段符号(0.5) = 13.9. 余裕をもって15で割る
        val cellSize = min(maxWidth / 15f, ShogiDimensions.BoardCellMaxSize)
        val fontSize = (cellSize.value * BoardViewConstants.PIECE_FONT_SIZE_RATIO).sp
        val labelSize = (cellSize.value * BoardViewConstants.LABEL_FONT_SIZE_RATIO).sp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val rangeX = BoardLayout.getRangeX(isFlipped)
            val rangeY = BoardLayout.getRangeY(isFlipped)

            // 左列: 後手駒台(Top) / 先手名(Bottom) ※反転時は入れ替え
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(cellSize * 11f),
            ) {
                if (isFlipped) {
                    KomaDai(board.senteMochigoma, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
                    Spacer(Modifier.weight(1f))
                    PlayerNameLabel(PieceColor.White.toSymbol() + session.goteName, isTurn = !isSenteTurn, cellSize = cellSize)
                } else {
                    KomaDai(board.goteMochigoma, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
                    Spacer(Modifier.weight(1f))
                    PlayerNameLabel(PieceColor.Black.toSymbol() + session.senteName, isTurn = isSenteTurn, cellSize = cellSize)
                }
            }

            // 中列: 盤面本体
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SujiLabels(rangeX, cellSize, labelSize, onToggleFlip)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoardGrid(board, rangeX, rangeY, cellSize, fontSize, isFlipped)
                    DanLabels(rangeY, cellSize, labelSize)
                }

                // 下側にも筋のラベルを置くとバランスが良い
                SujiLabels(rangeX, cellSize, labelSize, null)
            }

            // 右列: 後手名(Top) / 先手駒台(Bottom) ※反転時は入れ替え
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(cellSize * 11f),
            ) {
                if (isFlipped) {
                    PlayerNameLabel(PieceColor.Black.toSymbol() + session.senteName, isTurn = isSenteTurn, cellSize = cellSize)
                    Spacer(Modifier.weight(1f))
                    KomaDai(board.goteMochigoma, isSente = false, isFlipped = isFlipped, cellSize = cellSize)
                } else {
                    PlayerNameLabel(PieceColor.White.toSymbol() + session.goteName, isTurn = !isSenteTurn, cellSize = cellSize)
                    Spacer(Modifier.weight(1f))
                    KomaDai(board.senteMochigoma, isSente = true, isFlipped = isFlipped, cellSize = cellSize)
                }
            }
        }
    }
}
