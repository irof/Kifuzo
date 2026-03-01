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
import dev.irof.kifuzo.ui.theme.ShogiDimensions

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
    const val SENTE_TOP_VAL = 100f
    const val BORDER_WIDTH = 1f
    const val ZERO_FLOAT = 0f
}

/**
 * 形勢判断グラフ（評価値の推移グラフ）。
 *
 * 対局の各手における評価値を時系列で描画し、有利・不利の推移を視覚化します。
 * 非線形スケーリングにより、大きな評価値の変化も詳細に表示します。
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun EvaluationGraph(
    evaluations: List<Evaluation>,
    currentStep: Int,
    isFlipped: Boolean = false,
    onStepClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier.height(ShogiDimensions.Chart.DefaultHeight).fillMaxWidth(),
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
                        val pos = event.changes.first().position.x
                        hoverX = if (event.type != PointerEventType.Release) pos else null
                        if (event.type == PointerEventType.Release && pos in GraphConstants.ZERO_FLOAT..size.width.toFloat()) {
                            onStepClick(getStepIndex(pos, size.width.toFloat(), evaluations.size))
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
        drawRect(color = Color.Gray, style = Stroke(width = GraphConstants.BORDER_WIDTH.dp.toPx()))
        drawHoverIndicator(hoverX, evaluations, stepWidth, scaler, textMeasurer)
    }
}

private fun getStepIndex(x: Float, width: Float, total: Int): Int = (x / (width / maxOf(1, total))).toInt().coerceIn(0, total - 1)

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoverIndicator(
    hoverX: Float?,
    evaluations: List<Evaluation>,
    stepWidth: Float,
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val hx = hoverX ?: return
    val stepIndex = getStepIndex(hx, size.width, evaluations.size)
    val eval = evaluations[stepIndex]
    val label = getEvaluationLabelText(stepIndex, eval) ?: return
    val pointX = (stepIndex + GraphConstants.STEP_CENTER_OFFSET) * stepWidth
    val pointY = scaler.getScaledY(eval.orNull()?.toFloat() ?: GraphConstants.ZERO_FLOAT)
    drawCircle(color = ShogiColors.Chart.Line, radius = GraphConstants.POINT_RADIUS_OUTER.dp.toPx(), center = Offset(pointX, pointY))
    drawCircle(color = ShogiColors.Tooltip.Content, radius = GraphConstants.POINT_RADIUS_INNER.dp.toPx(), center = Offset(pointX, pointY))
    drawGraphTooltip(x = stepIndex * stepWidth, y = pointY, label = label, textMeasurer = textMeasurer)
}

private fun getEvaluationLabelText(stepIndex: Int, eval: Evaluation): String? = when (eval) {
    is Evaluation.Score -> "${stepIndex}手目: ${if (eval.value > 0) "+" else ""}${eval.value}"
    is Evaluation.SenteWin -> "${stepIndex}手目: 先手勝ち"
    is Evaluation.GoteWin -> "${stepIndex}手目: 後手勝ち"
    else -> null
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphBackground(
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val centerY = size.height / 2f
    val tYPos = scaler.getScaledY(GraphConstants.THRESHOLD)
    val tYNeg = scaler.getScaledY(-GraphConstants.THRESHOLD)
    val isSenteTop = scaler.getScaledY(GraphConstants.SENTE_TOP_VAL) < centerY
    val posColor = if (isSenteTop) ShogiColors.Chart.SenteAdvantage else ShogiColors.Chart.GoteAdvantage
    val negColor = if (isSenteTop) ShogiColors.Chart.GoteAdvantage else ShogiColors.Chart.SenteAdvantage

    drawRect(posColor.copy(alpha = GraphConstants.ALPHA_ZONE_LOW), Offset(0f, minOf(centerY, minOf(tYPos, tYNeg))), Size(size.width, kotlin.math.abs(centerY - minOf(tYPos, tYNeg))))
    drawRect(negColor.copy(alpha = GraphConstants.ALPHA_ZONE_LOW), Offset(0f, centerY), Size(size.width, kotlin.math.abs(centerY - maxOf(tYPos, tYNeg))))
    drawRect(posColor.copy(alpha = GraphConstants.ALPHA_ZONE_HIGH), Offset(0f, 0f), Size(size.width, minOf(tYPos, tYNeg)))
    drawRect(negColor.copy(alpha = GraphConstants.ALPHA_ZONE_HIGH), Offset(0f, maxOf(tYPos, tYNeg)), Size(size.width, size.height - maxOf(tYPos, tYNeg)))
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
        val currentPoint = Offset((i + GraphConstants.STEP_CENTER_OFFSET) * stepWidth, scaler.getScaledY(eval.orNull()?.toFloat() ?: GraphConstants.ZERO_FLOAT))
        if (lastPoint != null) drawLine(ShogiColors.Chart.Line, lastPoint, currentPoint, GraphConstants.LINE_WIDTH_EVAL)
        lastPoint = currentPoint
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentStepHighlight(currentStep: Int, totalSteps: Int, stepWidth: Float) {
    if (totalSteps == 0) return
    val startX = currentStep.coerceIn(0, totalSteps - 1) * stepWidth
    drawRect(color = ShogiColors.Chart.CurrentStepMarker, topLeft = Offset(startX, 0f), size = Size(stepWidth, size.height))
}
