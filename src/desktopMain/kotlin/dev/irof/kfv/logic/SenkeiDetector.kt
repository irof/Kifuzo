package dev.irof.kfv.logic

import dev.irof.kfv.models.BoardSnapshot
import dev.irof.kfv.models.Piece
import dev.irof.kfv.models.PieceColor
import dev.irof.kfv.models.Square

/**
 * 棋譜の履歴から戦型を判定します。
 */
fun detectSenkei(history: List<BoardSnapshot>): String {
    if (history.size < 10) return ""

    // 序盤の駒組みを見る（25手目前後）
    val checkStep = if (history.size > 25) 25 else history.size - 1
    val snapshot = history[checkStep]
    val cells = snapshot.cells

    var senteRookFile = -1
    var goteRookFile = -1

    for (y in 0..8) {
        for (x in 0..8) {
            val cell = cells[y][x] ?: continue
            if (cell.first == Piece.HI || cell.first == Piece.RY) {
                val square = Square.fromIndex(x, y)
                if (cell.second == PieceColor.Black) {
                    senteRookFile = square.file
                } else {
                    goteRookFile = square.file
                }
            }
        }
    }

    // 先手の戦型判定
    val senteSenkei = when (senteRookFile) {
        8 -> "向かい飛車"
        7 -> "三間飛車"
        6 -> "四間飛車"
        5 -> "中飛車"
        4 -> "右四間飛車"
        3 -> "袖飛車"
        else -> null
    }

    // 後手の戦型判定（後手から見た筋で判定）
    // 後手の2筋は全体の8筋、4筋は全体の6筋...
    val goteSenkei = when (goteRookFile) {
        2 -> "向かい飛車"
        3 -> "三間飛車"
        4 -> "四間飛車"
        5 -> "中飛車"
        6 -> "右四間飛車"
        7 -> "袖飛車"
        else -> null
    }

    return when {
        senteSenkei != null && goteSenkei != null -> {
            if (senteSenkei == goteSenkei) "相$senteSenkei" else "相振り飛車"
        }
        senteSenkei != null -> senteSenkei
        goteSenkei != null -> goteSenkei
        else -> {
            // 居飛車系の判定
            // 矢倉の判定 (先手 78金[x=2,y=7], 68銀[x=3,y=7])
            if (cells[7][2]?.first == Piece.KI && cells[7][3]?.first == Piece.GI) {
                "矢倉"
            } else {
                "居飛車"
            }
        }
    }
}
