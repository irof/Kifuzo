package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.ui.theme.ShogiColors

private object GraphConstants {
    const val MAX_EVAL = 10000f
    const val THRESHOLD = 2000f
    const val COMPRESSION = 0.2f
    const val GRID_STEP = 1000
    val GRID_RANGE = -10..10
    const val ALPHA_HIGHLIGHT = 0.15f
    const val ALPHA_ZONE_LOW = 0.15f
    const val ALPHA_ZONE_HIGH = 0.35f
    const val LINE_WIDTH_EVAL = 3f
    const val POINT_RADIUS_OUTER = 4f
    const val POINT_RADIUS_INNER = 2f
    const val STEP_CENTER_OFFSET = 0.5f
    const val SENTE_TOP_THRESHOLD = 100f
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun EvaluationGraph(
    evaluations: List<Evaluation>,
    currentStep: Int,
    isFlipped: Boolean = false,
    onStepClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier.height(240.dp).fillMaxWidth(),
) {
    if (evaluations.isEmpty() || evaluations.none { it is Evaluation.Score }) return
    var hoverX by remember { mutableStateOf<Float?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .pointerInput(evaluations) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        val isHover = change.pressed || event.type == PointerEventType.Move
                        hoverX = if (isHover) change.position.x else null
                        if (event.type == PointerEventType.Release && change.position.x in 0f..size.width.toFloat()) {
                            val stepWidth = size.width.toFloat() / maxOf(1, evaluations.size)
                            onStepClick((change.position.x / stepWidth).toInt().coerceIn(0, evaluations.size - 1))
                        }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Exit) { hoverX = null },
    ) {
        val scaler = NonLinearScaler(GraphConstants.MAX_EVAL, size.height, GraphConstants.THRESHOLD, GraphConstants.COMPRESSION, ScalerMode.CENTER_ZERO, isFlipped)
        val stepWidth = size.width / maxOf(1, evaluations.size)
        drawGraphBackground(scaler, textMeasurer)
        drawCurrentStepHighlight(currentStep, evaluations.size, stepWidth)
        drawEvaluationLine(evaluations, scaler, stepWidth)
        drawRect(color = Color.Gray, style = Stroke(width = 1.dp.toPx()))
        drawHoverIndicator(hoverX, evaluations, stepWidth, scaler, textMeasurer)
    }
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoverIndicator(
    hoverX: Float?,
    evaluations: List<Evaluation>,
    stepWidth: Float,
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val hx = hoverX ?: return
    val stepIndex = (hx / stepWidth).toInt().coerceIn(0, evaluations.size - 1)
    val eval = evaluations[stepIndex]
    val label = when (eval) {
        is Evaluation.Score -> "${stepIndex}手目: ${if (eval.value > 0) "+" else ""}${eval.value}"
        is Evaluation.SenteWin -> "${stepIndex}手目: 先手勝ち"
        is Evaluation.GoteWin -> "${stepIndex}手目: 後手勝ち"
        else -> null
    } ?: return
    val pointX = (stepIndex + GraphConstants.STEP_CENTER_OFFSET) * stepWidth
    val pointY = scaler.getScaledY(eval.orNull()?.toFloat() ?: 0f)
    drawCircle(color = ShogiColors.EvalLine, radius = GraphConstants.POINT_RADIUS_OUTER.dp.toPx(), center = Offset(pointX, pointY))
    drawCircle(color = Color.White, radius = GraphConstants.POINT_RADIUS_INNER.dp.toPx(), center = Offset(pointX, pointY))
    drawGraphTooltip(x = stepIndex * stepWidth, y = pointY, label = label, textMeasurer = textMeasurer)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphBackground(
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val centerY = size.height / 2f
    val tYPos = scaler.getScaledY(GraphConstants.THRESHOLD)
    val tYNeg = scaler.getScaledY(-GraphConstants.THRESHOLD)
    val topY = minOf(tYPos, tYNeg)
    val botY = maxOf(tYPos, tYNeg)
    val isSenteTop = scaler.getScaledY(GraphConstants.SENTE_TOP_THRESHOLD) < centerY
    val posColor = if (isSenteTop) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    val negColor = if (isSenteTop) ShogiColors.EvalNegative else ShogiColors.EvalPositive

    drawRect(posColor.copy(alpha = GraphConstants.ALPHA_ZONE_LOW), Offset(0f, minOf(centerY, topY)), Size(size.width, kotlin.math.abs(centerY - topY)))
    drawRect(negColor.copy(alpha = GraphConstants.ALPHA_ZONE_LOW), Offset(0f, centerY), Size(size.width, kotlin.math.abs(centerY - botY)))
    drawRect(posColor.copy(alpha = GraphConstants.ALPHA_ZONE_HIGH), Offset(0f, 0f), Size(size.width, topY))
    drawRect(negColor.copy(alpha = GraphConstants.ALPHA_ZONE_HIGH), Offset(0f, botY), Size(size.width, size.height - botY))
    drawEvaluationGrid(scaler, textMeasurer, GraphConstants.GRID_RANGE, GraphConstants.GRID_STEP, GraphConstants.THRESHOLD.toInt())
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEvaluationLine(
    evaluations: List<Evaluation>,
    scaler: NonLinearScaler,
    stepWidth: Float,
) {
    if (evaluations.size <= 1) return
    var lastPoint: Offset? = null
    for (i in evaluations.indices) {
        val eval = evaluations[i]
        if (eval is Evaluation.Unknown) continue
        val currentPoint = Offset((i + GraphConstants.STEP_CENTER_OFFSET) * stepWidth, scaler.getScaledY(eval.orNull()?.toFloat() ?: 0f))
        if (lastPoint != null) drawLine(ShogiColors.EvalLine, lastPoint, currentPoint, GraphConstants.LINE_WIDTH_EVAL)
        lastPoint = currentPoint
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentStepHighlight(currentStep: Int, totalSteps: Int, stepWidth: Float) {
    if (totalSteps == 0) return
    val startX = currentStep.coerceIn(0, totalSteps - 1) * stepWidth
    drawRect(color = ShogiColors.Primary.copy(alpha = GraphConstants.ALPHA_HIGHLIGHT), topLeft = Offset(startX, 0f), size = Size(stepWidth, size.height))
}
