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

    // 10手目から35手目までの各局面をスキャンして、自陣に飛車がいる時の筋をカウントする
    val startStep = 5
    val endStep = minOf(35, history.size - 1)
    
    val senteFiles = mutableMapOf<Int, Int>()
    val goteFiles = mutableMapOf<Int, Int>()

    for (step in startStep..endStep) {
        val snapshot = history[step]
        val cells = snapshot.cells

        for (y in 0..8) {
            for (x in 0..8) {
                val cell = cells[y][x] ?: continue
                if (cell.first == Piece.HI || cell.first == Piece.RY) {
                    val square = Square.fromIndex(x, y)
                    if (cell.second == PieceColor.Black) {
                        // 先手自陣 (7-9段) にいる時のみカウント
                        if (square.rank >= 7) {
                            senteFiles[square.file] = (senteFiles[square.file] ?: 0) + 1
                        }
                    } else {
                        // 後手自陣 (1-3段) にいる時のみカウント
                        if (square.rank <= 3) {
                            goteFiles[square.file] = (goteFiles[square.file] ?: 0) + 1
                        }
                    }
                }
            }
        }
    }

    // 最も頻繁に滞在した筋を取得
    val senteRookFile = senteFiles.maxByOrNull { it.value }?.key ?: -1
    val goteRookFile = goteFiles.maxByOrNull { it.value }?.key ?: -1

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

    // 後手の戦型判定
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
            // 居飛車系の判定（25手目時点の形で簡易判定）
            val finalSnapshot = history[minOf(25, history.size - 1)]
            val cells = finalSnapshot.cells
            if (cells[7][2]?.first == Piece.KI && cells[7][3]?.first == Piece.GI) {
                "矢倉"
            } else {
                "居飛車"
            }
        }
    }
}
