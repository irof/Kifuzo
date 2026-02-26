package dev.irof.kifuzo.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import dev.irof.kifuzo.ui.theme.ShogiColors

@OptIn(ExperimentalTextApi::class)
fun DrawScope.drawEvaluationGrid(
    scaler: NonLinearScaler,
    textMeasurer: TextMeasurer,
    gridRange: IntRange,
    gridStep: Int,
    threshold: Int,
) {
    for (i in gridRange) {
        if (i == 0) continue
        val evalValue = i * gridStep
        val y = scaler.getScaledY(evalValue.toFloat())
        if (y in 0f..size.height) {
            drawSingleGridLine(y, evalValue, threshold)
            drawSingleGridLabel(y, evalValue, threshold, textMeasurer)
        }
    }
    // Zero line
    drawLine(Color.Gray.copy(alpha = GraphCommonConstants.ALPHA_GRID_DARK), Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), GraphCommonConstants.LINE_WIDTH_THIN)
}

private fun DrawScope.drawSingleGridLine(y: Float, evalValue: Int, threshold: Int) {
    val isThreshold = kotlin.math.abs(evalValue) == threshold
    val alpha = if (isThreshold) GraphCommonConstants.ALPHA_GRID_DARK else GraphCommonConstants.ALPHA_GRID_LIGHT
    val strokeWidth = if (isThreshold) GraphCommonConstants.LINE_WIDTH_NORMAL else GraphCommonConstants.LINE_WIDTH_THIN

    drawLine(
        color = Color.Gray.copy(alpha = alpha),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth,
    )
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawSingleGridLabel(y: Float, evalValue: Int, threshold: Int, textMeasurer: TextMeasurer) {
    if (kotlin.math.abs(evalValue) <= threshold) {
        val sign = if (evalValue > 0) "+" else ""
        val textLayoutResult = textMeasurer.measure(
            text = AnnotatedString("$sign$evalValue"),
            style = TextStyle(color = Color.Gray.copy(alpha = GraphCommonConstants.ALPHA_LABEL), fontSize = GraphCommonConstants.LABEL_FONT_SIZE),
        )
        drawText(textLayoutResult, topLeft = Offset(GraphCommonConstants.LABEL_OFFSET_X.toPx(), y - textLayoutResult.size.height))
    }
}
