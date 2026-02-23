package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.GameResult
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

private object MoveListConstants {
    const val SELECTED_BACKGROUND_ALPHA = 0.15f
    const val SIGNIFICANT_MOVE_THRESHOLD = 500
    const val VERY_SIGNIFICANT_MOVE_THRESHOLD = 1000
    val STEP_NUMBER_WIDTH = 32.dp
    val EVALUATION_INFO_WIDTH = 50.dp
    val FONT_SIZE_DIFF = 9.sp
    val FONT_SIZE_MARKER = 11.sp
}

@Composable
fun KifuMoveList(
    history: List<BoardSnapshot>,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    onWriteResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showEvaluation = history.any {
        it.evaluation is Evaluation.SenteWin ||
            it.evaluation is Evaluation.GoteWin ||
            (it.evaluation is Evaluation.Score && it.evaluation.value != 0)
    }

    val lastSnapshot = history.lastOrNull()
    val lastMove = lastSnapshot?.lastMoveText ?: ""
    val evaluation = lastSnapshot?.evaluation?.orZero() ?: 0
    val isMate = kotlin.math.abs(evaluation) >= ShogiConstants.MATE_SCORE_THRESHOLD
    val isFinished = isMate || GameResult.ALL_KEYWORDS.any { lastMove.contains(it) }

    LaunchedEffect(currentStep) {
        if (currentStep in history.indices) {
            listState.animateScrollToItem(currentStep)
        }
    }

    Box(
        modifier = modifier
            .background(Color.White, MaterialTheme.shapes.medium)
            .border(ShogiDimensions.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.medium),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(vertical = ShogiDimensions.PaddingSmall),
        ) {
            item {
                MoveRow(0, AppStrings.START_POSITION, history[0].evaluation, null, currentStep == 0, showEvaluation, onStepChange)
            }

            items(history.size - 1) { index ->
                val i = index + 1
                val board = history[i]
                val prevEval = history[i - 1].evaluation.orZero()
                val currentEvaluation = board.evaluation
                val diff = if (currentEvaluation is Evaluation.Score) currentEvaluation.value - prevEval else null

                val colorSymbol = if (i % 2 != 0) "▲" else "△"
                val moveText = board.lastMoveText.trim().split(Regex("""\s+""")).getOrNull(1)?.substringBefore("(") ?: board.lastMoveText

                MoveRow(i, "$colorSymbol$moveText", currentEvaluation, diff, currentStep == i, showEvaluation, onStepChange)
            }

            if (!isFinished) {
                item {
                    AddResultRow(onWriteResult)
                }
            }
        }
    }
}

@Composable
private fun AddResultRow(onWriteResult: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ShogiDimensions.PaddingSmall, horizontal = ShogiDimensions.PaddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(MoveListConstants.STEP_NUMBER_WIDTH))
        Box {
            OutlinedButton(
                onClick = { showMenu = true },
                modifier = Modifier.height(ShogiDimensions.ButtonHeight),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
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

@Composable
private fun MoveRow(
    step: Int,
    label: String,
    evaluation: Evaluation,
    diff: Int?,
    isSelected: Boolean,
    showEvaluation: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val backgroundColor = if (isSelected) ShogiColors.Primary.copy(alpha = MoveListConstants.SELECTED_BACKGROUND_ALPHA) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onStepChange(step) }
            .padding(vertical = ShogiDimensions.PaddingSmall, horizontal = ShogiDimensions.PaddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepNumber(step)
        MoveLabel(label, diff, isSelected, showEvaluation, modifier = Modifier.weight(1f))
        if (showEvaluation) {
            EvaluationInfo(evaluation, diff)
        }
    }
}

@Composable
private fun StepNumber(step: Int) {
    Text(
        text = if (step == 0) "" else step.toString(),
        modifier = Modifier.width(MoveListConstants.STEP_NUMBER_WIDTH),
        style = MaterialTheme.typography.caption,
        color = Color.Gray,
    )
}

@Composable
private fun MoveLabel(label: String, diff: Int?, isSelected: Boolean, showEvaluation: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        if (showEvaluation) {
            SignificantMoveBadge(diff)
        }
    }
}

@Composable
private fun SignificantMoveBadge(diff: Int?) {
    val absDiff = if (diff != null) kotlin.math.abs(diff) else 0
    if (absDiff < MoveListConstants.SIGNIFICANT_MOVE_THRESHOLD) return

    val marker = if (absDiff >= MoveListConstants.VERY_SIGNIFICANT_MOVE_THRESHOLD) "!!" else "!"
    val markerColor = if (diff!! > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    Box(
        modifier = Modifier
            .padding(start = ShogiDimensions.PaddingSmall)
            .background(markerColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = ShogiDimensions.PaddingSmall, vertical = 1.dp),
    ) {
        Text(text = marker, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = MoveListConstants.FONT_SIZE_MARKER)
    }
}

@Composable
private fun EvaluationInfo(evaluation: Evaluation, diff: Int?) {
    when (evaluation) {
        is Evaluation.Score -> {
            EvaluationBadge(evaluation.value)
            EvaluationDiff(diff)
        }
        is Evaluation.SenteWin -> {
            Text(
                text = "先手勝ち",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = ShogiColors.EvalPositive,
                modifier = Modifier.width(MoveListConstants.EVALUATION_INFO_WIDTH * 2),
                textAlign = TextAlign.End,
            )
        }
        is Evaluation.GoteWin -> {
            Text(
                text = "後手勝ち",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = ShogiColors.EvalNegative,
                modifier = Modifier.width(MoveListConstants.EVALUATION_INFO_WIDTH * 2),
                textAlign = TextAlign.End,
            )
        }
        is Evaluation.Unknown -> {
            // Do nothing
        }
    }
}

@Composable
private fun EvaluationBadge(evaluation: Int) {
    val evalSign = if (evaluation > 0) "+" else ""
    val color = if (evaluation > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    Text(
        text = "$evalSign$evaluation",
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.width(MoveListConstants.EVALUATION_INFO_WIDTH),
        textAlign = TextAlign.End,
    )
}

@Composable
private fun EvaluationDiff(diff: Int?) {
    if (diff == null || diff == 0) {
        Spacer(Modifier.width(MoveListConstants.EVALUATION_INFO_WIDTH))
        return
    }
    val diffSign = if (diff > 0) "+" else ""
    val color = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    Text(
        text = " ($diffSign$diff)",
        style = MaterialTheme.typography.caption.copy(fontSize = MoveListConstants.FONT_SIZE_DIFF),
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.width(MoveListConstants.EVALUATION_INFO_WIDTH),
        textAlign = TextAlign.End,
    )
}
