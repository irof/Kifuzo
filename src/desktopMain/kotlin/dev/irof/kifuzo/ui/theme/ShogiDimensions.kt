package dev.irof.kifuzo.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * デザインシステムに基づいた寸法定義。
 */
object ShogiDimensions {
    object Spacing {
        val ExtraSmall = 2.dp
        val Small = 4.dp
        val Medium = 8.dp
        val Large = 16.dp
    }

    object Corner {
        val Medium = 8.dp
    }

    object Icon {
        val ExtraSmall = 12.dp
        val Small = 16.dp
        val Medium = 24.dp
    }

    object Text {
        val Caption = 10.sp
        val Small = 11.sp
        val Body = 12.sp
        val Title = 18.sp
    }

    object Board {
        val CellMaxSize = 60.dp
        val LineThickness = 1.5.dp
        val Padding = 2.dp
        val CellBorderThickness = 0.5.dp
    }

    object Component {
        val ButtonHeight = 32.dp
        val MenuBarWidth = 48.dp
        val SidebarDefaultWidth = 280.dp
        val MoveListWidth = 280.dp
    }

    object Chart {
        val DefaultHeight = 240.dp
        val DualHeight = 160.dp
    }
}
