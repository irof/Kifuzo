package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons

@Composable
fun MoveRow(
    step: Int,
    label: String,
    evaluation: Evaluation,
    diff: Int?,
    isSelected: Boolean,
    showEvaluation: Boolean,
    variations: List<List<BoardSnapshot>>,
    onStepChange: (Int) -> Unit,
    onSelectVariation: (List<BoardSnapshot>) -> Unit,
) {
    val alpha = 0.15f
    val backgroundColor = if (isSelected) ShogiColors.Primary.copy(alpha = alpha) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onStepChange(step) }
            .padding(vertical = ShogiDimensions.PaddingSmall, horizontal = ShogiDimensions.PaddingLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (step == 0) "" else step.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
        )
        MoveLabel(label, diff, isSelected, showEvaluation, variations, onSelectVariation, modifier = Modifier.weight(1f))
        if (showEvaluation) {
            EvaluationInfo(evaluation, diff)
        }
    }
}

@Composable
private fun MoveLabel(
    label: String,
    diff: Int?,
    isSelected: Boolean,
    showEvaluation: Boolean,
    variations: List<List<BoardSnapshot>>,
    onSelectVariation: (List<BoardSnapshot>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        if (showEvaluation) {
            SignificantMoveBadge(diff)
        }
        if (variations.isNotEmpty()) {
            VariationBadge(variations, onSelectVariation)
        }
    }
}

@Composable
private fun VariationBadge(
    variations: List<List<BoardSnapshot>>,
    onSelectVariation: (List<BoardSnapshot>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(start = 4.dp)) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(16.dp),
        ) {
            Icon(imageVector = ShogiIcons.SidebarToggle, contentDescription = "変化手順を表示", tint = Color.Blue)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            variations.forEachIndexed { index, variation ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    onSelectVariation(variation)
                }) {
                    val nextMove = variation.getOrNull(1)?.lastMoveText?.trim()?.split(Regex("""\s+"""))?.getOrNull(1) ?: "不明"
                    Text("変化 ${index + 1}: $nextMove...", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SignificantMoveBadge(diff: Int?) {
    val sigThreshold = 500
    val verySigThreshold = 1000
    val absDiff = if (diff != null) kotlin.math.abs(diff) else 0
    if (absDiff < sigThreshold) return

    val marker = if (absDiff >= verySigThreshold) "!!" else "!"
    val markerColor = if (diff!! > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    Box(
        modifier = Modifier
            .padding(start = ShogiDimensions.PaddingSmall)
            .background(markerColor, shape = MaterialTheme.shapes.small)
            .padding(horizontal = ShogiDimensions.PaddingSmall, vertical = 1.dp),
    ) {
        Text(text = marker, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
    }
}

@Composable
private fun EvaluationInfo(evaluation: Evaluation, diff: Int?) {
    val infoWidth = 50.dp
    when (evaluation) {
        is Evaluation.Score -> {
            EvaluationBadge(evaluation.value)
            EvaluationDiff(diff)
        }
        is Evaluation.SenteWin, is Evaluation.GoteWin -> {
            Text(
                text = if (evaluation is Evaluation.SenteWin) "先手勝ち" else "後手勝ち",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = if (evaluation is Evaluation.SenteWin) ShogiColors.EvalPositive else ShogiColors.EvalNegative,
                modifier = Modifier.width(infoWidth * 2),
                textAlign = TextAlign.End,
            )
        }
        else -> {}
    }
}

@Composable
private fun EvaluationBadge(evaluation: Int) {
    val infoWidth = 50.dp
    Text(
        text = "${if (evaluation > 0) "+" else ""}$evaluation",
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.Bold,
        color = if (evaluation > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative,
        modifier = Modifier.width(infoWidth),
        textAlign = TextAlign.End,
    )
}

@Composable
private fun EvaluationDiff(diff: Int?) {
    val infoWidth = 50.dp
    if (diff == null || diff == 0) {
        Spacer(Modifier.width(infoWidth))
        return
    }
    Text(
        text = " (${if (diff > 0) "+" else ""}$diff)",
        style = MaterialTheme.typography.caption.copy(fontSize = 9.sp),
        fontWeight = FontWeight.Bold,
        color = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative,
        modifier = Modifier.width(infoWidth),
        textAlign = TextAlign.End,
    )
}
