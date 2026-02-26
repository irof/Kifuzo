package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import kotlin.math.max

private object TimeGraphConstants {
    const val ALPHA_BAR = 0.6f
    const val ALPHA_HIGHLIGHT = 0.2f
    const val MIN_MAX_SECONDS = 60f // デフォルトの最低表示秒数

    const val GRID_SEC_5 = 5f
    const val GRID_SEC_10 = 10f
    const val GRID_SEC_20 = 20f
    const val GRID_SEC_SMALL = 60f
    const val GRID_SEC_MEDIUM = 300f
    const val GRID_SEC_LARGE = 1200f

    const val INTERVAL_SEC_1 = 1f
    const val INTERVAL_SEC_2 = 2f
    const val INTERVAL_SEC_5 = 5f
    const val INTERVAL_SEC_15 = 15f
    const val INTERVAL_SEC_60 = 60f
    const val INTERVAL_SEC_300 = 300f
    const val INTERVAL_SEC_600 = 600f

    const val SCALE_THRESHOLD = 60f // 1分を超えたら圧縮
    const val SCALE_COMPRESSION = 0.2f // 圧縮率

    const val AVG_LINE_DASH_ON = 10f
    const val AVG_LINE_DASH_OFF = 10f
    const val AVG_LINE_ALPHA = 0.8f
    const val PRECISION_THRESHOLD = 10.0
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

    val senteTimes = times.filterIndexed { i, t -> i % 2 != 0 && t != null }.mapNotNull { it }
    val goteTimes = times.filterIndexed { i, t -> i > 0 && i % 2 == 0 && t != null }.mapNotNull { it }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
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
                val maxSeconds = calculateMaxSeconds(times)
                val scaler = NonLinearScaler(
                    maxActualValue = maxSeconds,
                    height = size.height,
                    threshold = TimeGraphConstants.SCALE_THRESHOLD,
                    compression = TimeGraphConstants.SCALE_COMPRESSION,
                    mode = ScalerMode.BOTTOM_ZERO,
                )

                drawTimeGridLines(maxSeconds, scaler, textMeasurer)
                drawTimeCurrentHighlight(currentStep, totalSteps, stepWidth)
                drawTimeBars(times, stepWidth, scaler)
                drawTimeAverageLines(senteTimes, goteTimes, scaler)
                drawTimeHoverIndicator(hoverStep, times, stepWidth, scaler, textMeasurer)
            }
        }

        AverageLabelsRow(
            senteAvg = senteTimes.takeIf { it.isNotEmpty() }?.average(),
            goteAvg = goteTimes.takeIf { it.isNotEmpty() }?.average(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeAverageLines(
    senteTimes: List<Int>,
    goteTimes: List<Int>,
    scaler: NonLinearScaler,
) {
    if (senteTimes.isNotEmpty()) {
        drawSingleAverageLine(senteTimes.average().toFloat(), ShogiColors.EvalPositive, scaler)
    }
    if (goteTimes.isNotEmpty()) {
        drawSingleAverageLine(goteTimes.average().toFloat(), ShogiColors.EvalNegative, scaler)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSingleAverageLine(
    average: Float,
    baseColor: Color,
    scaler: NonLinearScaler,
) {
    val y = scaler.getScaledY(average)
    if (y < 0 || y > size.height) return

    val color = baseColor.copy(alpha = TimeGraphConstants.AVG_LINE_ALPHA)
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = GraphCommonConstants.LINE_WIDTH_THIN,
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(TimeGraphConstants.AVG_LINE_DASH_ON, TimeGraphConstants.AVG_LINE_DASH_OFF),
            0f,
        ),
    )
}

@Composable
private fun AverageLabelsRow(senteAvg: Double?, goteAvg: Double?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        senteAvg?.let {
            AverageLabel("▲", it, ShogiColors.EvalPositive)
            Spacer(Modifier.width(ShogiDimensions.PaddingLarge))
        }
        goteAvg?.let {
            AverageLabel("△", it, ShogiColors.EvalNegative)
        }
    }
}

@Composable
private fun AverageLabel(prefix: String, value: Double, color: Color) {
    val formattedValue = if (value < TimeGraphConstants.PRECISION_THRESHOLD) {
        String.format(java.util.Locale.US, "%.1fs", value)
    } else {
        "${value.toInt()}s"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$prefix avg:",
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = formattedValue,
            style = MaterialTheme.typography.caption,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = color.copy(alpha = TimeGraphConstants.AVG_LINE_ALPHA),
        )
    }
}

private fun calculateMaxSeconds(times: List<Int?>): Float {
    val maxSecondsActual = (times.filterNotNull().maxOrNull() ?: 0).toFloat()
    return when {
        maxSecondsActual < TimeGraphConstants.GRID_SEC_5 -> TimeGraphConstants.GRID_SEC_5
        maxSecondsActual < TimeGraphConstants.GRID_SEC_10 -> TimeGraphConstants.GRID_SEC_10
        maxSecondsActual < TimeGraphConstants.GRID_SEC_20 -> TimeGraphConstants.GRID_SEC_20
        else -> max(TimeGraphConstants.MIN_MAX_SECONDS, maxSecondsActual)
    }
}

@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeHoverIndicator(
    hoverStep: Int?,
    times: List<Int?>,
    stepWidth: Float,
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val step = hoverStep ?: return
    val seconds = times.getOrNull(step) ?: return
    val timeStr = "${seconds / ShogiConstants.SECONDS_IN_MINUTE}:${(seconds % ShogiConstants.SECONDS_IN_MINUTE).toString().padStart(2, '0')}"
    drawGraphTooltip(
        x = step * stepWidth,
        y = scaler.getScaledY(seconds.toFloat()),
        label = "${step}手目: $timeStr",
        textMeasurer = textMeasurer,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeGridLines(
    maxSeconds: Float,
    scaler: NonLinearScaler,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val gridInterval = when {
        maxSeconds <= TimeGraphConstants.GRID_SEC_5 -> TimeGraphConstants.INTERVAL_SEC_1
        maxSeconds <= TimeGraphConstants.GRID_SEC_10 -> TimeGraphConstants.INTERVAL_SEC_2
        maxSeconds <= TimeGraphConstants.GRID_SEC_20 -> TimeGraphConstants.INTERVAL_SEC_5
        maxSeconds <= TimeGraphConstants.GRID_SEC_SMALL -> TimeGraphConstants.INTERVAL_SEC_15
        maxSeconds <= TimeGraphConstants.GRID_SEC_MEDIUM -> TimeGraphConstants.INTERVAL_SEC_60
        maxSeconds <= TimeGraphConstants.GRID_SEC_LARGE -> TimeGraphConstants.INTERVAL_SEC_300
        else -> TimeGraphConstants.INTERVAL_SEC_600
    }
    var gridSeconds = gridInterval
    while (gridSeconds <= maxSeconds) {
        val y = scaler.getScaledY(gridSeconds)
        if (y < 0) break

        drawLine(
            color = Color.Gray.copy(alpha = GraphCommonConstants.ALPHA_GRID_LIGHT),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = GraphCommonConstants.LINE_WIDTH_THIN,
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
            style = TextStyle(color = Color.Gray.copy(alpha = GraphCommonConstants.ALPHA_LABEL), fontSize = GraphCommonConstants.LABEL_FONT_SIZE),
        )
        drawText(
            textLayoutResult,
            topLeft = Offset(GraphCommonConstants.LABEL_OFFSET_X.toPx(), y - textLayoutResult.size.height),
        )

        gridSeconds += gridInterval
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeBars(
    times: List<Int?>,
    stepWidth: Float,
    scaler: NonLinearScaler,
) {
    for (i in times.indices) {
        val seconds = times[i]?.toFloat() ?: continue
        val y = scaler.getScaledY(seconds)
        val barHeight = size.height - y
        val isSente = i % 2 != 0
        val color = if (isSente) ShogiColors.EvalPositive else ShogiColors.EvalNegative

        drawRect(
            color = color.copy(alpha = TimeGraphConstants.ALPHA_BAR),
            topLeft = Offset(i * stepWidth, y),
            size = Size(stepWidth.coerceAtLeast(GraphCommonConstants.LINE_WIDTH_THIN), barHeight),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeCurrentHighlight(currentStep: Int, totalSteps: Int, stepWidth: Float) {
    if (totalSteps == 0) return
    val currentX = currentStep.coerceIn(0, totalSteps - 1) * stepWidth
    drawRect(
        color = ShogiColors.Primary.copy(alpha = 0.2f),
        topLeft = Offset(currentX, 0f),
        size = Size(stepWidth, size.height),
    )
}
