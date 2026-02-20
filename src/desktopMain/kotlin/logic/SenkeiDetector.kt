package logic

import models.BoardSnapshot
import models.Piece

/**
 * 棋譜の履歴から戦型を判定します。
 */
fun detectSenkei(history: List<BoardSnapshot>): String {
    if (history.size < 10) return ""

    // 20手目くらいまでの局面を見て判定
    val checkStep = if (history.size > 25) 25 else history.size - 1
    val snapshot = history[checkStep]
    val cells = snapshot.cells

    // 飛車の位置を確認 (y, x)
    var senteRookX = -1
    var goteRookX = -1

    for (y in 0..8) {
        for (x in 0..8) {
            val cell = cells[y][x] ?: continue
            if (cell.first == Piece.HI || cell.first == Piece.RY) {
                if (cell.second) senteRookX = 8 - x + 1 // 先手 (1-9筋)
                else goteRookX = 8 - x + 1 // 後手 (1-9筋)
            }
        }
    }

    // 振り飛車の判定
    val senteFuri = when (senteRookX) {
        5 -> "中飛車"
        6 -> "四間飛車"
        7 -> "三間飛車"
        8 -> "向かい飛車"
        else -> null
    }

    val goteFuri = when (goteRookX) {
        5 -> "中飛車"
        4 -> "四間飛車"
        3 -> "三間飛車"
        2 -> "向かい飛車"
        else -> null
    }

    return when {
        senteFuri != null && goteFuri != null -> "相振り飛車"
        senteFuri != null -> senteFuri
        goteFuri != null -> goteFuri
        else -> {
            // 居飛車系の簡易判定
            // 矢倉の判定 (先手 78金, 68銀, 76歩あたり)
            if (cells[7][2]?.first == Piece.KI && cells[7][3]?.first == Piece.GI) {
                "矢倉"
            } else {
                "居飛車"
            }
        }
    }
}
