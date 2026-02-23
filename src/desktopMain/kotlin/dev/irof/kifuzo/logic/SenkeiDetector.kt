package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square

private object SenkeiDetectionConstants {
    const val MIN_HISTORY_SIZE = 10
    const val SCAN_START_STEP = 5
    const val SCAN_END_STEP = 35
    const val YAGURA_CHECK_STEP = 25

    // 飛車の位置による戦型判定用の筋
    const val FILE_MUKAI_SENTE = 8
    const val FILE_SANKEN_SENTE = 7
    const val FILE_SHIKEN_SENTE = 6
    const val FILE_NAKABISHA_SENTE = 5
    const val FILE_MIGISHIKEN_SENTE = 4
    const val FILE_SODE_SENTE = 3

    const val FILE_MUKAI_GOTE = 2
    const val FILE_SANKEN_GOTE = 3
    const val FILE_SHIKEN_GOTE = 4
    const val FILE_NAKABISHA_GOTE = 5
    const val FILE_MIGISHIKEN_GOTE = 6
    const val FILE_SODE_GOTE = 7

    // 矢倉判定用
    const val YAGURA_RANK = 7
    const val YAGURA_FILE_GOLD = 2
    const val YAGURA_FILE_SILVER = 3
}

/**
 * 棋譜の履歴から戦型を判定します。
 */
fun detectSenkei(history: List<BoardSnapshot>): String {
    if (history.size < SenkeiDetectionConstants.MIN_HISTORY_SIZE) return ""

    // 10手目から35手目までの各局面をスキャンして、自陣に飛車がいる時の筋をカウントする
    val startStep = SenkeiDetectionConstants.SCAN_START_STEP
    val endStep = minOf(SenkeiDetectionConstants.SCAN_END_STEP, history.size - 1)

    val senteFiles = mutableMapOf<Int, Int>()
    val goteFiles = mutableMapOf<Int, Int>()

    for (step in startStep..endStep) {
        val snapshot = history[step]
        val cells = snapshot.cells

        for (y in 0 until ShogiConstants.BOARD_SIZE) {
            for (x in 0 until ShogiConstants.BOARD_SIZE) {
                val cell = cells[y][x] ?: continue
                if (cell.first == Piece.HI || cell.first == Piece.RY) {
                    val square = Square.fromIndex(x, y)
                    if (cell.second == PieceColor.Black) {
                        // 先手自陣 (7-9段) にいる時のみカウント
                        if (square.rank >= ShogiConstants.SENTE_CAMP_RANK_START) {
                            senteFiles[square.file] = (senteFiles[square.file] ?: 0) + 1
                        }
                    } else {
                        // 後手自陣 (1-3段) にいる時のみカウント
                        if (square.rank <= ShogiConstants.GOTE_CAMP_RANK_END) {
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
        SenkeiDetectionConstants.FILE_MUKAI_SENTE -> "向かい飛車"
        SenkeiDetectionConstants.FILE_SANKEN_SENTE -> "三間飛車"
        SenkeiDetectionConstants.FILE_SHIKEN_SENTE -> "四間飛車"
        SenkeiDetectionConstants.FILE_NAKABISHA_SENTE -> "中飛車"
        SenkeiDetectionConstants.FILE_MIGISHIKEN_SENTE -> "右四間飛車"
        SenkeiDetectionConstants.FILE_SODE_SENTE -> "袖飛車"
        else -> null
    }

    // 後手の戦型判定
    val goteSenkei = when (goteRookFile) {
        SenkeiDetectionConstants.FILE_MUKAI_GOTE -> "向かい飛車"
        SenkeiDetectionConstants.FILE_SANKEN_GOTE -> "三間飛車"
        SenkeiDetectionConstants.FILE_SHIKEN_GOTE -> "四間飛車"
        SenkeiDetectionConstants.FILE_NAKABISHA_GOTE -> "中飛車"
        SenkeiDetectionConstants.FILE_MIGISHIKEN_GOTE -> "右四間飛車"
        SenkeiDetectionConstants.FILE_SODE_GOTE -> "袖飛車"
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
            val finalSnapshot = history[minOf(SenkeiDetectionConstants.YAGURA_CHECK_STEP, history.size - 1)]
            val cells = finalSnapshot.cells
            // 矢倉の簡易判定 (7八金、7九銀 またはそれっぽい形)
            if (cells[SenkeiDetectionConstants.YAGURA_RANK][SenkeiDetectionConstants.YAGURA_FILE_GOLD]?.first == Piece.KI &&
                cells[SenkeiDetectionConstants.YAGURA_RANK][SenkeiDetectionConstants.YAGURA_FILE_SILVER]?.first == Piece.GI
            ) {
                "矢倉"
            } else {
                "居飛車"
            }
        }
    }
}
