@file:Suppress("MagicNumber")

package dev.irof.kifuzo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * デザインシステムに基づいた色定義。
 * セマンティックな命名（用途に基づいた名前）を採用しています。
 */
object ShogiColors {
    object Board {
        val Background = Color(0xFFF3C077)
        val Line = Color.Black
        val CellBorder = Color.Gray.copy(alpha = 0.5f)
        val HighlightLastTo = Color(0xFFFFD54F).copy(alpha = 0.7f)
        val HighlightLastFrom = Color(0xFFFFE082).copy(alpha = 0.5f)
    }

    object Piece {
        val Sente = Color.Black
        val Gote = Color.Black
        val Promoted = Color.Red
    }

    object Chart {
        val SenteAdvantage = Color(0xFFD32F2F) // 赤系
        val GoteAdvantage = Color(0xFF1976D2) // 青系
        val Line = Color.Black
        val CurrentStepMarker = Color(0xFF6200EE).copy(alpha = 0.15f)
        const val AVERAGE_LINE_ALPHA = 0.8f
    }

    object Panel {
        val Background = Color(0xFFEEEEEE)
        val MenuBarBackground = Color(0xFFE0E0E0)
        val Primary = Color(0xFF6200EE)
        const val SELECTED_BACKGROUND_ALPHA = 0.15f
    }

    object Tooltip {
        val Background = Color(0xFF333333)
        val Content = Color.White
    }
}
