package dev.irof.kifuzo.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions

object GraphCommonConstants {
    const val ALPHA_GRID_LIGHT = 0.2f
    const val ALPHA_GRID_DARK = 0.5f
    const val ALPHA_TOOLTIP = 0.7f
    const val ALPHA_LABEL = 0.8f

    val LABEL_FONT_SIZE = 8.sp
    val LABEL_OFFSET_X = 4.dp

    val TOOLTIP_PADDING = 4.dp
    val TOOLTIP_OFFSET = 8.dp
    val TOOLTIP_CORNER_RADIUS = 4.dp

    const val LINE_WIDTH_NORMAL = 1.5f
    const val LINE_WIDTH_THIN = 1f
    const val LINE_WIDTH_INDICATOR = 2f
}

enum class ScalerMode {
    CENTER_ZERO, // 中央が0 (評価値グラフ用)
    BOTTOM_ZERO, // 下端が0 (所要時間グラフ用)
}

/**
 * 非線形スケールを計算するクラス。
 */
class NonLinearScaler(
    private val maxActualValue: Float,
    private val height: Float,
    private val threshold: Float,
    private val compression: Float,
    private val mode: ScalerMode,
    private val isFlipped: Boolean = false,
) {
    private val displayMax = calculateScaledValue(maxActualValue)

    fun getScaledY(value: Float): Float {
        val clampedValue = value.coerceIn(-maxActualValue, maxActualValue)
        val scaledValue = calculateScaledValue(clampedValue)
        return when (mode) {
            ScalerMode.CENTER_ZERO -> {
                val centerY = height / 2f
                val finalScaledValue = if (isFlipped) -scaledValue else scaledValue
                centerY - (finalScaledValue / displayMax * centerY)
            }
            ScalerMode.BOTTOM_ZERO -> {
                height - (scaledValue / displayMax * height)
            }
        }
    }

    private fun calculateScaledValue(value: Float): Float {
        val absVal = kotlin.math.abs(value)
        val sign = if (value >= 0) 1f else -1f
        return if (absVal <= threshold) {
            value
        } else {
            sign * (threshold + (absVal - threshold) * compression)
        }
    }
}

/**
 * グラフ上のホバーツールチップを描画します。
 */
@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawGraphTooltip(
    x: Float,
    y: Float,
    label: String,
    textMeasurer: TextMeasurer,
) {
    val textLayoutResult: TextLayoutResult = textMeasurer.measure(
        text = AnnotatedString(label),
        style = TextStyle(color = ShogiColors.Tooltip.Content, fontSize = ShogiDimensions.Text.Caption),
    )

    val padding = GraphCommonConstants.TOOLTIP_PADDING.toPx()
    val tooltipWidth = textLayoutResult.size.width + padding * 2
    val tooltipHeight = textLayoutResult.size.height + padding * 2

    var tooltipX = x + GraphCommonConstants.TOOLTIP_OFFSET.toPx()
    if (tooltipX + tooltipWidth > size.width) {
        tooltipX = x - tooltipWidth - GraphCommonConstants.TOOLTIP_OFFSET.toPx()
    }
    val tooltipY = (y - tooltipHeight).coerceIn(0f, size.height - tooltipHeight)

    drawRoundRect(
        color = ShogiColors.Tooltip.Background.copy(alpha = GraphCommonConstants.ALPHA_TOOLTIP),
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(tooltipWidth, tooltipHeight),
        cornerRadius = CornerRadius(GraphCommonConstants.TOOLTIP_CORNER_RADIUS.toPx()),
    )
    drawText(
        textLayoutResult,
        color = ShogiColors.Tooltip.Content,
        topLeft = Offset(tooltipX + padding, tooltipY + padding),
    )
}
