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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.ui.theme.ShogiColors

private object GraphConstants {
    const val MAX_EVAL = 10000f
    const val THRESHOLD = 2000f
    const val COMPRESSION = 0.2f

    const val GRID_STEP = 1000
    const val GRID_RANGE_MIN = -10
    const val GRID_RANGE_MAX = 10
    val GRID_RANGE = GRID_RANGE_MIN..GRID_RANGE_MAX

    const val ALPHA_HIGHLIGHT = 0.15f
    const val ALPHA_ZONE_LOW = 0.15f
    const val ALPHA_ZONE_HIGH = 0.35f
    const val LINE_WIDTH_EVAL = 3f
    const val POINT_RADIUS_OUTER = 4f
    const val POINT_RADIUS_INNER = 2f
}

/**
 * 評価値の推移をグラフ表示するコンポーネント
 */
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
                        hoverX = if (change.pressed || event.type == PointerEventType.Move) {
                            change.position.x
                        } else {
                            null
                        }

                        if (event.type == PointerEventType.Release && change.position.x in 0f..size.width.toFloat()) {
                            val totalSteps = evaluations.size
                            val stepWidth = size.width.toFloat() / maxOf(1, totalSteps)
                            val stepIndex = (change.position.x / stepWidth).toInt().coerceIn(0, totalSteps - 1)
                            onStepClick(stepIndex)
                        }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoverX = null
            },
    ) {
        val scaler = NonLinearScaler(
            maxActualValue = GraphConstants.MAX_EVAL,
            height = size.height,
            threshold = GraphConstants.THRESHOLD,
            compression = GraphConstants.COMPRESSION,
            mode = ScalerMode.CENTER_ZERO,
            isFlipped = isFlipped,
        )
        val totalSteps = evaluations.size
        val stepWidth = size.width / maxOf(1, totalSteps)

        drawGraphBackground(scaler, textMeasurer)
        drawCurrentStepHighlight(currentStep, totalSteps, stepWidth)
        drawEvaluationLine(evaluations, scaler, stepWidth)
        drawGraphBorder()
        drawHoverIndicator(hoverX, evaluations, stepWidth, scaler, textMeasurer)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphBorder() {
    drawRect(
        color = Color.Gray,
        style = Stroke(width = 1.dp.toPx()),
    )
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
    val label = getEvaluationLabel(stepIndex, eval) ?: return

    val pointX = (stepIndex + 0.5f) * stepWidth
    val pointY = scaler.getScaledY(eval.orNull()?.toFloat() ?: 0f)

    // データポイントの丸点を表示
    drawCircle(
        color = ShogiColors.EvalLine,
        radius = GraphConstants.POINT_RADIUS_OUTER.dp.toPx(),
        center = Offset(pointX, pointY),
    )
    drawCircle(
        color = Color.White,
        radius = GraphConstants.POINT_RADIUS_INNER.dp.toPx(),
        center = Offset(pointX, pointY),
    )

    drawGraphTooltip(
        x = stepIndex * stepWidth,
        y = pointY,
        label = label,
        textMeasurer = textMeasurer,
    )
}

private fun getEvaluationLabel(stepIndex: Int, eval: Evaluation): String? = when (eval) {
    is Evaluation.Score -> {
        val score = eval.value
        val sign = if (score > 0) "+" else ""
        "${stepIndex}手目: $sign$score"
    }
    is Evaluation.SenteWin -> "${stepIndex}手目: 先手勝ち"
    is Evaluation.GoteWin -> "${stepIndex}手目: 後手勝ち"
    is Evaluation.Unknown -> null
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphBackground(
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    drawBackgroundZones(scaler)
    drawGridLines(scaler, textMeasurer)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackgroundZones(
    scaler: NonLinearScaler,
) {
    val width = size.width
    val centerY = size.height / 2f
    val thresholdYPos = scaler.getScaledY(GraphConstants.THRESHOLD)
    val thresholdYNeg = scaler.getScaledY(-GraphConstants.THRESHOLD)
    val topY = minOf(thresholdYPos, thresholdYNeg)
    val bottomY = maxOf(thresholdYPos, thresholdYNeg)

    val isSenteTop = scaler.getScaledY(100f) < centerY
    val posColor = if (isSenteTop) ShogiColors.EvalPositive else ShogiColors.EvalNegative
    val negColor = if (isSenteTop) ShogiColors.EvalNegative else ShogiColors.EvalPositive

    // 接戦ゾーン (0 .. threshold)
    drawRect(posColor.copy(alpha = GraphConstants.ALPHA_ZONE_LOW), Offset(0f, minOf(centerY, topY)), Size(width, kotlin.math.abs(centerY - topY)))
    drawRect(negColor.copy(alpha = GraphConstants.ALPHA_ZONE_LOW), Offset(0f, centerY), Size(width, kotlin.math.abs(centerY - bottomY)))
    // 大差ゾーン (threshold .. MAX)
    drawRect(posColor.copy(alpha = GraphConstants.ALPHA_ZONE_HIGH), Offset(0f, 0f), Size(width, topY))
    drawRect(negColor.copy(alpha = GraphConstants.ALPHA_ZONE_HIGH), Offset(0f, bottomY), Size(width, size.height - bottomY))
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLines(
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val width = size.width
    val height = size.height

    for (i in GraphConstants.GRID_RANGE) {
        if (i == 0) continue
        val evalValue = i * GraphConstants.GRID_STEP
        val y = scaler.getScaledY(evalValue.toFloat())
        if (y in 0f..height) {
            drawGridLine(y, evalValue)
            drawGridLabel(y, evalValue, textMeasurer)
        }
    }
    // Zero line
    drawLine(Color.Gray.copy(alpha = GraphCommonConstants.ALPHA_GRID_DARK), Offset(0f, height / 2f), Offset(width, height / 2f), GraphCommonConstants.LINE_WIDTH_THIN)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLine(y: Float, evalValue: Int) {
    val isThreshold = kotlin.math.abs(evalValue) == GraphConstants.THRESHOLD.toInt()
    val alpha = if (isThreshold) GraphCommonConstants.ALPHA_GRID_DARK else GraphCommonConstants.ALPHA_GRID_LIGHT
    drawLine(
        color = Color.Gray.copy(alpha = alpha),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = if (isThreshold) GraphCommonConstants.LINE_WIDTH_NORMAL else GraphCommonConstants.LINE_WIDTH_THIN,
    )
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridLabel(
    y: Float,
    evalValue: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    if (kotlin.math.abs(evalValue) <= GraphConstants.THRESHOLD) {
        val sign = if (evalValue > 0) "+" else ""
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString("$sign$evalValue"),
            style = TextStyle(color = Color.Gray.copy(alpha = GraphCommonConstants.ALPHA_LABEL), fontSize = GraphCommonConstants.LABEL_FONT_SIZE),
        )
        drawText(
            textLayoutResult,
            topLeft = Offset(GraphCommonConstants.LABEL_OFFSET_X.toPx(), y - textLayoutResult.size.height),
        )
    }
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
        if (eval is Evaluation.Unknown) {
            continue
        }
        val x = (i + 0.5f) * stepWidth
        val currentPoint = Offset(x, scaler.getScaledY(eval.orNull()?.toFloat() ?: 0f))
        if (lastPoint != null) {
            drawLine(ShogiColors.EvalLine, lastPoint, currentPoint, GraphConstants.LINE_WIDTH_EVAL)
        }
        lastPoint = currentPoint
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentStepHighlight(
    currentStep: Int,
    totalSteps: Int,
    stepWidth: Float,
) {
    if (totalSteps == 0) return
    val startX = currentStep.coerceIn(0, totalSteps - 1) * stepWidth
    drawRect(
        color = ShogiColors.Primary.copy(alpha = GraphConstants.ALPHA_HIGHLIGHT),
        topLeft = Offset(startX, 0f),
        size = Size(stepWidth, size.height),
    )
}
