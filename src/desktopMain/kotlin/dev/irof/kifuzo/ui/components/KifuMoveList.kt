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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.GameResult
import dev.irof.kifuzo.models.toMoveLabel
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun KifuMoveList(
    history: List<BoardSnapshot>,
    currentStep: Int,
    isMainHistory: Boolean,
    onStepChange: (Int) -> Unit,
    onWriteResult: (String) -> Unit,
    onSelectVariation: (List<BoardSnapshot>) -> Unit,
    onResetMain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showEvaluation = history.any { it.evaluation.isSignificant() }
    val lastSnapshot = history.lastOrNull()
    val isFinished = lastSnapshot != null && GameResult.isFinished(lastSnapshot.lastMoveText, lastSnapshot.evaluation)

    LaunchedEffect(currentStep) {
        if (currentStep in history.indices) listState.animateScrollToItem(currentStep)
    }

    Box(modifier = modifier.background(Color.White, MaterialTheme.shapes.medium).border(ShogiDimensions.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.medium)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(vertical = ShogiDimensions.PaddingSmall)) {
            if (!isMainHistory) item { ResetToMainButton(onResetMain) }
            items(history.size) { step ->
                val board = history[step]
                val diff = if (step > 0) calculateEvaluationDiff(board.evaluation, history[step - 1].evaluation) else null
                MoveRow(step, board.toMoveLabel(step), board.evaluation, diff, currentStep == step, showEvaluation, board.variations, onStepChange, onSelectVariation)
            }
            if (!isFinished && isMainHistory) item { AddResultRow(onWriteResult) }
        }
    }
}

private fun calculateEvaluationDiff(current: Evaluation, previous: Evaluation): Int? = if (current is Evaluation.Score) current.value - previous.orZero() else null

@Composable
private fun ResetToMainButton(onResetMain: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingLarge, vertical = ShogiDimensions.PaddingSmall)) {
        OutlinedButton(onClick = onResetMain, modifier = Modifier.fillMaxWidth().height(ShogiDimensions.ButtonHeight), contentPadding = PaddingValues(0.dp)) {
            Text("本譜に戻る", fontSize = 11.sp)
        }
    }
}

@Composable
private fun AddResultRow(onWriteResult: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = ShogiDimensions.PaddingSmall, horizontal = ShogiDimensions.PaddingLarge), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(32.dp))
        Box {
            OutlinedButton(onClick = { showMenu = true }, modifier = Modifier.height(ShogiDimensions.ButtonHeight), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ShogiDimensions.IconSizeSmall))
                Spacer(Modifier.width(ShogiDimensions.PaddingExtraSmall))
                Text("終局手を追加", fontSize = 11.sp)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                GameResult.UI_SELECTIONS.forEach { result ->
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        onWriteResult(result)
                    }) {
                        Text(result, fontSize = ShogiDimensions.FontSizeBody)
                    }
                }
            }
        }
    }
}
