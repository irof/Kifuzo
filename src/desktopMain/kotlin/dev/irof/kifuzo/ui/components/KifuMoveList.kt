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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun KifuMoveList(
    history: List<BoardSnapshot>,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentStep) {
        if (currentStep in history.indices) {
            listState.animateScrollToItem(currentStep)
        }
    }

    Box(
        modifier = modifier
            .background(Color.White, MaterialTheme.shapes.medium)
            .border(1.dp, Color.LightGray, MaterialTheme.shapes.medium),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        ) {
            item {
                MoveRow(0, AppStrings.START_POSITION, null, null, currentStep == 0, onStepChange)
            }

            items(history.size - 1) { index ->
                val i = index + 1
                val board = history[i]
                val prevEval = history[i - 1].evaluation ?: 0
                val curEval = board.evaluation
                val diff = if (curEval != null) curEval - prevEval else null

                val colorSymbol = if (i % 2 != 0) "▲" else "△"
                val moveText = board.lastMoveText.trim().split(Regex("""\s+""")).getOrNull(1)?.substringBefore("(") ?: board.lastMoveText

                MoveRow(i, "$colorSymbol$moveText", curEval, diff, currentStep == i, onStepChange)
            }
        }
    }
}

@Composable
private fun MoveRow(
    step: Int,
    label: String,
    evaluation: Int?,
    diff: Int?,
    isSelected: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val backgroundColor = if (isSelected) ShogiColors.Primary.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onStepChange(step) }
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepNumber(step)
        MoveLabel(label, diff, isSelected, modifier = Modifier.weight(1f))
        EvaluationInfo(evaluation, diff)
    }
}

@Composable
private fun StepNumber(step: Int) {
    Text(
        text = if (step == 0) "" else step.toString(),
        modifier = Modifier.width(32.dp),
        style = MaterialTheme.typography.caption,
        color = Color.Gray,
    )
}

@Composable
private fun MoveLabel(label: String, diff: Int?, isSelected: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        SignificantMoveBadge(diff)
    }
}

@Composable
private fun SignificantMoveBadge(diff: Int?) {
    val absDiff = if (diff != null) kotlin.math.abs(diff) else 0
    if (absDiff < 500) return

    val marker = if (absDiff >= 1000) "!!" else "!"
    val markerColor = if (diff!! > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .background(markerColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(text = marker, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
    }
}

@Composable
private fun EvaluationInfo(evaluation: Int?, diff: Int?) {
    if (evaluation == null) return
    EvaluationBadge(evaluation)
    EvaluationDiff(diff)
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
        modifier = Modifier.width(50.dp),
        textAlign = TextAlign.End,
    )
}

@Composable
private fun EvaluationDiff(diff: Int?) {
    if (diff == null || diff == 0) {
        Spacer(Modifier.width(50.dp))
        return
    }
    val diffSign = if (diff > 0) "+" else ""
    val color = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    Text(
        text = " ($diffSign$diff)",
        style = MaterialTheme.typography.caption.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.width(50.dp),
        textAlign = TextAlign.End,
    )
}
