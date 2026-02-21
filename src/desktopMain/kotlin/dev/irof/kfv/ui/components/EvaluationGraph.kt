package dev.irof.kfv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irof.kfv.ui.theme.ShogiColors

/**
 * 評価値の推移をグラフ表示するコンポーネント
 */
@Composable
fun EvaluationGraph(
    evaluations: List<Int?>,
    currentStep: Int,
    modifier: Modifier = Modifier.height(60.dp).width(280.dp),
) {
    if (evaluations.isEmpty() || evaluations.all { it == null }) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxEval = 2000f // 評価値の表示上限/下限（スケーリング基準）

        // 背景（0ライン）
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f,
        )

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

        // 折れ線の描画
        if (evaluations.size > 1) {
            val stepWidth = width / (evaluations.size - 1)
            var lastPoint: Offset? = null

            for (i in evaluations.indices) {
                val eval = evaluations[i] ?: continue
                val x = i * stepWidth
                // 評価値を画面高さに合わせて変換（上を先手プラス、下を後手マイナス）
                // 2000以上は端に固定
                val y = centerY - (eval.coerceIn(-2000, 2000) / maxEval * centerY)

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
            val currentX = currentStep.coerceIn(0, evaluations.size - 1) * (width / (evaluations.size - 1).coerceAtLeast(1))
            drawLine(
                color = ShogiColors.EvalCurrentPos,
                start = Offset(currentX, 0f),
                end = Offset(currentX, height),
                strokeWidth = 2f,
            )
        }
    }
}
