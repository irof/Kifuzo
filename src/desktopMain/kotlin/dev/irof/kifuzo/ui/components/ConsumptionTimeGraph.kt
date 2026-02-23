package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
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
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import kotlin.math.max

private object TimeGraphConstants {
    const val ALPHA_BAR = 0.6f
    const val ALPHA_GRID = 0.2f
    const val ALPHA_TOOLTIP = 0.7f
    const val ALPHA_LABEL = 0.8f
    val TOOLTIP_PADDING = 4.dp
    val TOOLTIP_OFFSET = 8.dp
    val TOOLTIP_CORNER_RADIUS = 4.dp
    const val LINE_WIDTH_THIN = 1f
    const val LINE_WIDTH_INDICATOR = 2f
    const val MIN_MAX_SECONDS = 60f // 最低でも1分は表示

    const val GRID_SEC_SMALL = 60f
    const val GRID_SEC_MEDIUM = 300f
    const val GRID_SEC_LARGE = 1200f
    const val INTERVAL_SEC_15 = 15f
    const val INTERVAL_SEC_60 = 60f
    const val INTERVAL_SEC_300 = 300f
    const val INTERVAL_SEC_600 = 600f

    val LABEL_FONT_SIZE = 8.sp
    val LABEL_OFFSET_X = 4.dp
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun ConsumptionTimeGraph(
    times: List<Int?>,
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier.height(160.dp).fillMaxWidth(),
) {
    var hoverStep by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .background(Color.White, MaterialTheme.shapes.small)
            .border(ShogiDimensions.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.small)
            .padding(ShogiDimensions.PaddingSmall),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(times) {
                    detectTapGestures { offset ->
                        val stepWidth = size.width / max(1, times.size)
                        val step = (offset.x / stepWidth).toInt().coerceIn(0, times.size - 1)
                        onStepClick(step)
                    }
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val offset = event.changes.first().position
                    val stepWidth = size.width / max(1, times.size)
                    hoverStep = (offset.x / stepWidth).toInt().coerceIn(0, times.size - 1)
                }
                .onPointerEvent(PointerEventType.Exit) {
                    hoverStep = null
                },
        ) {
            val totalSteps = times.size
            if (totalSteps == 0) return@Canvas

            val stepWidth = size.width / totalSteps
            val maxSeconds = max(TimeGraphConstants.MIN_MAX_SECONDS, (times.filterNotNull().maxOrNull() ?: 0).toFloat())
            val scaleY = size.height / maxSeconds

            drawTimeGridLines(maxSeconds, scaleY, textMeasurer)
            drawTimeBars(times, stepWidth, scaleY)
            drawTimeIndicator(currentStep, totalSteps, stepWidth)

            hoverStep?.let { step ->
                drawTimeTooltip(step, times.getOrNull(step), stepWidth, scaleY, textMeasurer)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeGridLines(
    maxSeconds: Float,
    scaleY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val gridInterval = when {
        maxSeconds <= TimeGraphConstants.GRID_SEC_SMALL -> TimeGraphConstants.INTERVAL_SEC_15
        maxSeconds <= TimeGraphConstants.GRID_SEC_MEDIUM -> TimeGraphConstants.INTERVAL_SEC_60
        maxSeconds <= TimeGraphConstants.GRID_SEC_LARGE -> TimeGraphConstants.INTERVAL_SEC_300
        else -> TimeGraphConstants.INTERVAL_SEC_600
    }
    var gridSeconds = gridInterval
    while (gridSeconds <= maxSeconds) {
        val y = size.height - (gridSeconds * scaleY)
        if (y < 0) break

        drawLine(
            color = Color.Gray.copy(alpha = TimeGraphConstants.ALPHA_GRID),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = TimeGraphConstants.LINE_WIDTH_THIN,
        )

        val label = when {
            gridSeconds < ShogiConstants.SECONDS_IN_MINUTE -> "${gridSeconds.toInt()}s"
            gridSeconds % ShogiConstants.SECONDS_IN_MINUTE == 0f -> "${(gridSeconds / ShogiConstants.SECONDS_IN_MINUTE).toInt()}m"
            else -> {
                val m = (gridSeconds / ShogiConstants.SECONDS_IN_MINUTE).toInt()
                val s = (gridSeconds % ShogiConstants.SECONDS_IN_MINUTE).toInt()
                "${m}m${s}s"
            }
        }
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString(label),
            style = TextStyle(color = Color.Gray.copy(alpha = TimeGraphConstants.ALPHA_LABEL), fontSize = TimeGraphConstants.LABEL_FONT_SIZE),
        )
        drawText(
            textLayoutResult,
            topLeft = Offset(TimeGraphConstants.LABEL_OFFSET_X.toPx(), y - textLayoutResult.size.height),
        )

        gridSeconds += gridInterval
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeBars(times: List<Int?>, stepWidth: Float, scaleY: Float) {
    for (i in times.indices) {
        val seconds = times[i] ?: continue
        val barHeight = seconds * scaleY
        val isSente = i % 2 != 0
        val color = if (isSente) ShogiColors.EvalPositive else ShogiColors.EvalNegative

        drawRect(
            color = color.copy(alpha = TimeGraphConstants.ALPHA_BAR),
            topLeft = Offset(i * stepWidth, size.height - barHeight),
            size = Size(stepWidth.coerceAtLeast(TimeGraphConstants.LINE_WIDTH_THIN), barHeight),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeIndicator(currentStep: Int, totalSteps: Int, stepWidth: Float) {
    val currentX = currentStep.coerceIn(0, totalSteps - 1) * stepWidth
    drawLine(
        color = Color.Black,
        start = Offset(currentX, 0f),
        end = Offset(currentX, size.height),
        strokeWidth = TimeGraphConstants.LINE_WIDTH_INDICATOR,
    )
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeTooltip(
    step: Int,
    seconds: Int?,
    stepWidth: Float,
    scaleY: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    if (seconds == null) return
    val x = step * stepWidth
    val timeStr = "${seconds / ShogiConstants.SECONDS_IN_MINUTE}:${(seconds % ShogiConstants.SECONDS_IN_MINUTE).toString().padStart(2, '0')}"
    val textLayoutResult = textMeasurer.measure(
        text = AnnotatedString("${step}手目: $timeStr"),
        style = TextStyle(color = Color.White, fontSize = 10.sp),
    )

    val padding = TimeGraphConstants.TOOLTIP_PADDING.toPx()
    val tooltipWidth = textLayoutResult.size.width + padding * 2
    val tooltipHeight = textLayoutResult.size.height + padding * 2

    var tooltipX = x + TimeGraphConstants.TOOLTIP_OFFSET.toPx()
    if (tooltipX + tooltipWidth > this.size.width) tooltipX = x - tooltipWidth - TimeGraphConstants.TOOLTIP_OFFSET.toPx()
    val tooltipY = (this.size.height - seconds * scaleY - tooltipHeight).coerceIn(0f, this.size.height - tooltipHeight)

    drawRoundRect(
        color = Color.Black.copy(alpha = TimeGraphConstants.ALPHA_TOOLTIP),
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(TimeGraphConstants.TOOLTIP_CORNER_RADIUS.toPx()),
    )
    drawText(textLayoutResult, color = Color.White, topLeft = Offset(tooltipX + padding, tooltipY + padding))
}
