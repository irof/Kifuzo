package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        Text(
            text = if (step == 0) "" else step.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
        )

        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.body2, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            SignificantMoveBadge(diff)
        }

        EvaluationInfo(evaluation, diff)
    }
}

@Composable
private fun SignificantMoveBadge(diff: Int?) {
    if (diff == null || kotlin.math.abs(diff) < 500) return

    val marker = if (kotlin.math.abs(diff) >= 1000) "!!" else "!"
    val markerColor = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
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

    val evalSign = if (evaluation > 0) "+" else ""
    Text(
        text = "$evalSign$evaluation",
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        color = if (evaluation > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative,
        modifier = Modifier.width(50.dp),
        textAlign = TextAlign.End,
    )

    if (diff != null && diff != 0) {
        val diffSign = if (diff > 0) "+" else ""
        val diffColor = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
        Text(
            text = " ($diffSign$diff)",
            style = MaterialTheme.typography.caption.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            color = diffColor,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End,
        )
    } else {
        Spacer(Modifier.width(50.dp))
    }
}
