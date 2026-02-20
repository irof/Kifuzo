package logic

import models.BoardSnapshot
import models.Piece

/**
 * 棋譜の履歴から戦型を判定します。
 */
fun detectSenkei(history: List<BoardSnapshot>): String {
    if (history.size < 10) return ""

    // 序盤の駒組みを見る（25手目前後）
    val checkStep = if (history.size > 25) 25 else history.size - 1
    val snapshot = history[checkStep]
    val cells = snapshot.cells

    // 飛車の位置を確認 (1-9筋)
    var senteRookX = -1
    var goteRookX = -1

    for (y in 0..8) {
        for (x in 0..8) {
            val cell = cells[y][x] ?: continue
            if (cell.first == Piece.HI || cell.first == Piece.RY) {
                if (cell.second) {
                    senteRookX = 8 - x + 1 // 先手視点の筋 (1-9)
                } else {
                    goteRookX = x + 1 // 後手視点の筋 (1-9)
                }
            }
        }
    }

    // 各陣営の戦型判定 (3:袖, 4:右四間, 5:中, 6:四間, 7:三間, 8:向かい)
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
        // 両者が振り飛車（または特殊な居飛車）の場合
        senteSenkei != null && goteSenkei != null -> {
            if (senteSenkei == goteSenkei) "相$senteSenkei" else "相振り飛車"
        }
        senteSenkei != null -> senteSenkei
        goteSenkei != null -> goteSenkei
        else -> {
            // どちらも飛車を振っていない場合は居飛車系の簡易判定
            if (cells[7][2]?.first == Piece.KI && cells[7][3]?.first == Piece.GI) {
                "矢倉"
            } else {
                "居飛車"
            }
        }
    }
}
