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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
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
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.ui.theme.ShogiColors

private object GraphConstants {
    const val MAX_EVAL = 10000f
    const val THRESHOLD = 2000f
    const val COMPRESSION = 0.5f
    val DISPLAY_MAX = THRESHOLD + (MAX_EVAL - THRESHOLD) * COMPRESSION
}

private class GraphScaler(private val isFlipped: Boolean, private val height: Float) {
    private val centerY = height / 2f

    fun getScaledY(eval: Int?): Float {
        if (eval == null) return centerY
        val value = eval.coerceIn(-10000, 10000).toFloat()
        val absVal = kotlin.math.abs(value)
        val base = if (absVal <= GraphConstants.THRESHOLD) {
            value
        } else {
            val sign = if (value >= 0) 1f else -1f
            sign * (GraphConstants.THRESHOLD + (absVal - GraphConstants.THRESHOLD) * GraphConstants.COMPRESSION)
        }
        val scaledValue = if (isFlipped) -base else base
        return centerY - (scaledValue / GraphConstants.DISPLAY_MAX * centerY)
    }

    fun getThresholdY(): Float = centerY * (GraphConstants.THRESHOLD / GraphConstants.DISPLAY_MAX)
}

/**
 * 評価値の推移をグラフ表示するコンポーネント
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun EvaluationGraph(
    evaluations: List<Int?>,
    currentStep: Int,
    isFlipped: Boolean = false,
    onStepClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier.height(240.dp).fillMaxWidth(),
) {
    if (evaluations.isEmpty() || evaluations.all { it == null }) return

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
                            val stepCount = evaluations.size
                            val stepWidth = if (stepCount > 1) size.width.toFloat() / (stepCount - 1) else size.width.toFloat()
                            val stepIndex = (change.position.x / stepWidth.coerceAtLeast(1f)).toInt().coerceIn(0, stepCount - 1)
                            onStepClick(stepIndex)
                        }
                    }
                }
            }
            .onPointerEvent(PointerEventType.Exit) {
                hoverX = null
            },
    ) {
        val scaler = GraphScaler(isFlipped, size.height)
        val stepCount = evaluations.size
        val stepWidth = if (stepCount > 1) size.width / (stepCount - 1) else size.width

        drawGraphBackground(scaler, isFlipped)
        drawEvaluationLine(evaluations, scaler, stepWidth)
        drawCurrentStepLine(currentStep, evaluations.size, stepWidth)

        drawRect(
            color = Color.Gray,
            style = Stroke(width = 1.dp.toPx()),
        )

        hoverX?.let { hx ->
            val stepIndex = (hx / stepWidth).toInt().coerceIn(0, evaluations.size - 1)
            drawHoverTooltip(stepIndex, evaluations[stepIndex], stepWidth, scaler, textMeasurer)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphBackground(
    scaler: GraphScaler,
    isFlipped: Boolean,
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2f
    val thresholdY = scaler.getThresholdY()

    val posColor = if (isFlipped) ShogiColors.EvalNegative else ShogiColors.EvalPositive
    val negColor = if (isFlipped) ShogiColors.EvalPositive else ShogiColors.EvalNegative

    // Background zones
    drawRect(posColor.copy(alpha = 0.15f), Offset(0f, centerY - thresholdY), Size(width, thresholdY))
    drawRect(negColor.copy(alpha = 0.15f), Offset(0f, centerY), Size(width, thresholdY))
    drawRect(posColor.copy(alpha = 0.35f), Offset(0f, 0f), Size(width, centerY - thresholdY))
    drawRect(negColor.copy(alpha = 0.35f), Offset(0f, centerY + thresholdY), Size(width, height - (centerY + thresholdY)))

    // Grid lines
    for (i in -10..10) {
        if (i == 0) continue
        val y = scaler.getScaledY(i * 1000)
        if (y in 0f..height) {
            val isThreshold = kotlin.math.abs(i * 1000) == 2000
            drawLine(
                color = Color.Gray.copy(alpha = if (isThreshold) 0.5f else 0.2f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = if (isThreshold) 1.5f else 1f,
            )
        }
    }
    drawLine(Color.Gray.copy(alpha = 0.5f), Offset(0f, centerY), Offset(width, centerY), 1f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEvaluationLine(
    evaluations: List<Int?>,
    scaler: GraphScaler,
    stepWidth: Float,
) {
    if (evaluations.size <= 1) return
    var lastPoint: Offset? = null
    for (i in evaluations.indices) {
        val eval = evaluations[i] ?: continue
        val currentPoint = Offset(i * stepWidth, scaler.getScaledY(eval))
        if (lastPoint != null) {
            drawLine(ShogiColors.EvalLine, lastPoint, currentPoint, 3f)
        }
        lastPoint = currentPoint
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentStepLine(
    currentStep: Int,
    totalSteps: Int,
    stepWidth: Float,
) {
    if (totalSteps == 0) return
    val currentX = currentStep.coerceIn(0, totalSteps - 1) * stepWidth
    drawLine(ShogiColors.EvalCurrentPos, Offset(currentX, 0f), Offset(currentX, size.height), 2f)
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHoverTooltip(
    stepIndex: Int,
    eval: Int?,
    stepWidth: Float,
    scaler: GraphScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val x = stepIndex * stepWidth
    drawLine(Color.Black.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), 1f)

    if (eval != null) {
        val sign = if (eval > 0) "+" else ""
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString("${stepIndex}手目: $sign$eval"),
            style = TextStyle(color = Color.White, fontSize = 10.sp),
        )

        val padding = 4.dp.toPx()
        val tooltipWidth = textLayoutResult.size.width + padding * 2
        val tooltipHeight = textLayoutResult.size.height + padding * 2

        var tooltipX = x + 8.dp.toPx()
        if (tooltipX + tooltipWidth > size.width) tooltipX = x - tooltipWidth - 8.dp.toPx()
        val tooltipY = (scaler.getScaledY(eval) - tooltipHeight / 2).coerceIn(0f, size.height - tooltipHeight)

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.7f),
            topLeft = Offset(tooltipX, tooltipY),
            size = Size(tooltipWidth, tooltipHeight),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawText(textLayoutResult, Offset(tooltipX + padding, tooltipY + padding))
    }
}
