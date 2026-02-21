package dev.irof.kfv.ui.components

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
import dev.irof.kfv.ui.theme.ShogiColors

/**
 * 評価値の推移をグラフ表示するコンポーネント
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class)
@Composable
fun EvaluationGraph(
    evaluations: List<Int?>,
    currentStep: Int,
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

                        // クリック（離した瞬間）の判定
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
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxEval = 10000f // 評価値の表示上限/下限（スケーリング基準）

        // 領域の色分け
        drawRect(
            color = ShogiColors.EvalPositive,
            topLeft = Offset(0f, 0f),
            size = Size(width, centerY),
        )
        drawRect(
            color = ShogiColors.EvalNegative,
            topLeft = Offset(0f, centerY),
            size = Size(width, height - centerY),
        )

        // 横線（1000ごと）
        for (i in -10..10) {
            if (i == 0) continue
            val y = centerY - (i * 1000f / maxEval * centerY)
            if (y in 0f..height) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                )
            }
        }

        // 背景（0ライン）
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f,
        )

        val stepCount = evaluations.size
        val stepWidth = if (stepCount > 1) width / (stepCount - 1) else width

        // 折れ線の描画
        if (stepCount > 1) {
            var lastPoint: Offset? = null

            for (i in evaluations.indices) {
                val eval = evaluations[i] ?: continue
                val x = i * stepWidth
                // 評価値を画面高さに合わせて変換（上を先手プラス、下を後手マイナス）
                // 10000以上は端に固定
                val y = centerY - (eval.coerceIn(-10000, 10000) / maxEval * centerY)

                val currentPoint = Offset(x, y)
                if (lastPoint != null) {
                    drawLine(
                        color = ShogiColors.EvalLine,
                        start = lastPoint,
                        end = currentPoint,
                        strokeWidth = 2f,
                    )
                }
                lastPoint = currentPoint
            }
        }

        // 現在の手数位置を示す縦棒
        if (evaluations.isNotEmpty()) {
            val currentX = currentStep.coerceIn(0, evaluations.size - 1) * stepWidth
            drawLine(
                color = ShogiColors.EvalCurrentPos,
                start = Offset(currentX, 0f),
                end = Offset(currentX, height),
                strokeWidth = 2f,
            )
        }

        // 外枠
        drawRect(
            color = Color.Gray,
            topLeft = Offset(0f, 0f),
            size = size,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
        )

        // ホバー時のツールチップ描画
        hoverX?.let { hx ->
            val stepIndex = (hx / stepWidth).toInt().coerceIn(0, evaluations.size - 1)
            val eval = evaluations[stepIndex]
            val x = stepIndex * stepWidth

            // ガイド線
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f,
            )

            if (eval != null) {
                val sign = if (eval > 0) "+" else ""
                val text = "${stepIndex}手目: $sign$eval"
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = TextStyle(color = Color.White, fontSize = 10.sp),
                )

                val padding = 4.dp.toPx()
                val tooltipWidth = textLayoutResult.size.width + padding * 2
                val tooltipHeight = textLayoutResult.size.height + padding * 2

                var tooltipX = x + 8.dp.toPx()
                if (tooltipX + tooltipWidth > width) {
                    tooltipX = x - tooltipWidth - 8.dp.toPx()
                }

                val tooltipY = (centerY - (eval.coerceIn(-10000, 10000) / maxEval * centerY) - tooltipHeight / 2).coerceIn(0f, height - tooltipHeight)

                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.7f),
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Fill,
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(tooltipX + padding, tooltipY + padding),
                )
            }
        }
    }
}
