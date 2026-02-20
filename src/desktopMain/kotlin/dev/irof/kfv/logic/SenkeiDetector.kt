package dev.irof.kfv.logic

import dev.irof.kfv.models.BoardSnapshot
import dev.irof.kfv.models.Piece

/**
 * 棋譜の履歴から戦型を判定します。
 */
fun detectSenkei(history: List<BoardSnapshot>): String {
    if (history.size < 10) return ""
    val checkStep = if (history.size > 25) 25 else history.size - 1
    val snapshot = history[checkStep]
    val cells = snapshot.cells
    var senteRookX = -1
    var goteRookX = -1
    for (y in 0..8) {
        for (x in 0..8) {
            val cell = cells[y][x] ?: continue
            if (cell.first == Piece.HI || cell.first == Piece.RY) {
                if (cell.second) senteRookX = 8 - x + 1
                else goteRookX = x + 1
            }
        }
    }
    fun getFuriType(rookX: Int): String? = when (rookX) {
        3 -> "袖飛車"
        4 -> "右四間飛車"
        5 -> "中飛車"
        6 -> "四間飛車"
        7 -> "三間飛車"
        8 -> "向かい飛車"
        else -> null
    }
    val senteSenkei = getFuriType(senteRookX)
    val goteSenkei = getFuriType(goteRookX)
    return when {
        senteSenkei != null && goteSenkei != null -> if (senteSenkei == goteSenkei) "相$senteSenkei" else "相振り飛車"
        senteSenkei != null -> senteSenkei
        goteSenkei != null -> goteSenkei
        else -> if (cells[7][2]?.first == Piece.KI && cells[7][3]?.first == Piece.GI) "矢倉" else "居飛車"
    }
}
