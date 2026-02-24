@file:Suppress("MagicNumber")

package dev.irof.kifuzo.ui.theme

import androidx.compose.ui.graphics.Color

object ShogiColors {
    val BoardBackground = Color(0xFFF3C077)
    val BoardLine = Color.Black
    val CellBorder = Color.Gray.copy(alpha = 0.5f)

    val HighlightLastTo = Color(0xFFFFD54F).copy(alpha = 0.7f)
    val HighlightLastFrom = Color(0xFFFFE082).copy(alpha = 0.5f)

    val PieceSente = Color.Black
    val PieceGote = Color.Black
    val PiecePromoted = Color.Red

    val TurnHighlight = Color.Yellow.copy(alpha = 0.3f)
    val PanelBackground = Color(0xFFEEEEEE)
    val MenuBarBackground = Color(0xFFE0E0E0)

    val Primary = Color(0xFF6200EE)
    val Success = Color(0xFF4CAF50)
    val Info = Color(0xFF2196F3)

    // 評価値グラフ用
    val EvalPositive = Color(0xFFD32F2F) // 先手有利（赤系）
    val EvalNegative = Color(0xFF1976D2) // 後手有利（青系）
    val EvalLine = Color(0xFF000000) // グラフの線（黒）
    val EvalCurrentPos = Color.Black // 現在の手数位置

    val TooltipBackground = Color(0xFF333333)
}
