package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.GameResult
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.models.toMoveLabel
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings

/**
 * 指し手リスト（本譜および変化のリスト表示）。
 *
 * 棋譜の各指し手を一覧表示し、クリックによる局面移動や、
 * 変化（分岐）の切り替え、終局結果の追加などの機能を提供します。
 */
@Composable
fun KifuMoveList(
    moves: List<Move>,
    currentStep: Int,
    isMainHistory: Boolean,
    onStepChange: (Int) -> Unit,
    onWriteResult: (String) -> Unit,
    onSelectVariation: (List<Move>) -> Unit,
    onResetMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showEval = moves.any { it.evaluation.isSignificant() }
    val lastMove = moves.lastOrNull()
    val isFinished = lastMove != null && GameResult.isFinished(lastMove.moveText, lastMove.evaluation)

    LaunchedEffect(currentStep) {
        if (currentStep in 0..moves.size) listState.animateScrollToItem(currentStep)
    }

    Box(modifier = modifier.background(Color.White, MaterialTheme.shapes.medium).border(ShogiDimensions.Board.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.medium)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = ShogiDimensions.Spacing.Small)) {
            if (!isMainHistory) item { ResetToMainButton(onResetMain) }

            item { StartPositionItem(currentStep == 0, showEval, onStepChange, onSelectVariation) }

            items(moves.size) { index ->
                val move = moves[index]
                val prevEval = if (index > 0) moves[index - 1].evaluation else Evaluation.Unknown
                MoveRow(index + 1, move.toMoveLabel(), move.evaluation, calculateDiff(move.evaluation, prevEval), currentStep == index + 1, showEval, move.variations, onStepChange, onSelectVariation)
            }

            if (!isFinished && isMainHistory) item { AddResultRow(onWriteResult) }
        }
    }
}

@Composable
private fun StartPositionItem(isSelected: Boolean, showEval: Boolean, onStepChange: (Int) -> Unit, onSelectVariation: (List<Move>) -> Unit) {
    MoveRow(0, AppStrings.START_POSITION, Evaluation.Unknown, null, isSelected, showEval, emptyList(), onStepChange, onSelectVariation)
}

private fun calculateDiff(current: Evaluation, previous: Evaluation): Int? = if (current is Evaluation.Score) current.value - previous.orZero() else null

@Composable
private fun ResetToMainButton(onResetMain: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.Spacing.Large, vertical = ShogiDimensions.Spacing.Small)) {
        OutlinedButton(onClick = onResetMain, modifier = Modifier.fillMaxWidth().height(ShogiDimensions.Component.ButtonHeight), contentPadding = PaddingValues(0.dp)) {
            Text("本譜に戻る", fontSize = ShogiDimensions.Text.Small)
        }
    }
}

@Composable
private fun AddResultRow(onWriteResult: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = ShogiDimensions.Spacing.Small, horizontal = ShogiDimensions.Spacing.Large), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(32.dp))
        Box {
            OutlinedButton(onClick = { showMenu = true }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Icon(ShogiIcons.Add, contentDescription = null, modifier = Modifier.size(ShogiDimensions.Icon.Small))
                Spacer(Modifier.width(ShogiDimensions.Spacing.ExtraSmall))
                Text("終局手を追加", fontSize = ShogiDimensions.Text.Small)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                GameResult.UI_SELECTIONS.forEach { result ->
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        onWriteResult(result)
                    }) {
                        Text(result, fontSize = ShogiDimensions.Text.Body)
                    }
                }
            }
        }
    }
}
