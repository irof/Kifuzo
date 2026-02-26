package dev.irof.kifuzo.models

import dev.irof.kifuzo.utils.AppStrings

object ShogiConstants {
    const val BOARD_SIZE = 9
    const val MAX_FILE = 9
    const val MAX_RANK = 9
    const val MAX_INDEX = 8

    const val SECONDS_IN_MINUTE = 60
    const val MINUTES_IN_HOUR = 60
    const val SECONDS_IN_HOUR = 3600

    const val MATE_SCORE_THRESHOLD = 30000
    const val WIN_SCORE = 31111
    const val LOSE_SCORE = -31111

    const val SENTE_CAMP_RANK_START = 7
    const val GOTE_CAMP_RANK_END = 3
}

sealed interface Evaluation {
    object Unknown : Evaluation
    data class Score(val value: Int) : Evaluation
    object SenteWin : Evaluation
    object GoteWin : Evaluation

    fun orNull(): Int? = when (this) {
        is Score -> value
        is SenteWin -> ShogiConstants.WIN_SCORE
        is GoteWin -> ShogiConstants.LOSE_SCORE
        is Unknown -> null
    }

    fun orZero(): Int = orNull() ?: 0

    fun isSignificant(): Boolean = when (this) {
        is SenteWin, is GoteWin -> true
        is Score -> value != 0
        is Unknown -> false
    }
}

enum class PieceColor {
    Black, // 先手 (▲)
    White, // 後手 (△)
    ;

    fun toSymbol(): String = if (this == Black) "▲" else "△"
}

/**
 * 盤面上の駒（種類と色）を表します。
 */
data class BoardPiece(val piece: Piece, val color: PieceColor)

/**
 * 棋譜の一手を表します。
 */
data class Move(
    val step: Int,
    val moveText: String,
    val resultSnapshot: BoardSnapshot,
    val evaluation: Evaluation = Evaluation.Unknown,
    val consumptionSeconds: Int? = null,
    val variations: List<List<Move>> = emptyList(),
) {
    companion object {
        /**
         * 通常の移動による指し手を生成します。
         */
        fun createMove(
            step: Int,
            from: Square,
            to: Square,
            isPromote: Boolean,
            moveText: String,
            prevSnapshot: BoardSnapshot,
            consumptionSeconds: Int? = null,
        ): Move {
            val turnColor = if (step % 2 != 0) PieceColor.Black else PieceColor.White
            val result = prevSnapshot.applyMove(from, to, isPromote, turnColor)
            return Move(step, moveText, result, consumptionSeconds = consumptionSeconds)
        }

        /**
         * 駒打ちによる指し手を生成します。
         */
        fun createDrop(
            step: Int,
            piece: Piece,
            to: Square,
            moveText: String,
            prevSnapshot: BoardSnapshot,
            consumptionSeconds: Int? = null,
        ): Move {
            val turnColor = if (step % 2 != 0) PieceColor.Black else PieceColor.White
            val result = prevSnapshot.applyDrop(piece, to, turnColor)
            return Move(step, moveText, result, consumptionSeconds = consumptionSeconds)
        }

        /**
         * 終局結果（投了など）による指し手を生成します。
         */
        fun createResult(
            step: Int,
            resultText: String,
            prevSnapshot: BoardSnapshot,
        ): Move {
            val turnColor = if (step % 2 != 0) PieceColor.Black else PieceColor.White
            val evaluation = if (turnColor == PieceColor.Black) Evaluation.GoteWin else Evaluation.SenteWin
            return Move(step, resultText, prevSnapshot.copy(), evaluation = evaluation)
        }
    }
}

data class Square(val file: Int, val rank: Int) {
    // file: 1-9 (筋), rank: 1-9 (段)

    init {
        require(file in 1..ShogiConstants.MAX_FILE && rank in 1..ShogiConstants.MAX_RANK) { "Invalid square: $file$rank" }
    }

    // 配列インデックスへの変換 (x: 0-8, y: 0-8)
    // 配列は [y][x] で、x=0 が 9筋, x=8 が 1筋 となっている（ShogiBoardViewの実装に合わせる）
    // 1筋(file=1) -> x=8, 9筋(file=9) -> x=0
    val xIndex: Int get() = ShogiConstants.BOARD_SIZE - file
    val yIndex: Int get() = rank - 1

    companion object {
        fun fromIndex(x: Int, y: Int): Square = Square(ShogiConstants.BOARD_SIZE - x, y + 1)
    }
}

object GameResult {
    /** 棋譜パースや判定に使用する、すべての終局キーワード */
    val ALL_KEYWORDS = listOf("投了", "詰み", "千日手", "持将棋", "中断", "不戦敗", "反則負け", "切れ負け", "タイムアップ", "入玉勝ち")

    /** UI の「終局手を追加」メニューに表示する、一般的な終局キーワード */
    val UI_SELECTIONS = listOf("投了", "詰み", "千日手", "持将棋", "中断")

    /** 指定されたテキストが終局行（指し手ではない）であるか判定します */
    fun isResultLine(line: String): Boolean = ALL_KEYWORDS.any { line.contains(it) } && !line.contains("▲") && !line.contains("△")

    fun isFinished(lastMoveText: String, evaluation: Evaluation): Boolean {
        val score = evaluation.orZero()
        val isMate = kotlin.math.abs(score) >= ShogiConstants.MATE_SCORE_THRESHOLD
        return isMate || ALL_KEYWORDS.any { lastMoveText.contains(it) }
    }
}

fun Move.toMoveLabel(): String {
    val colorSymbol = if (step % 2 != 0) "▲" else "△"
    val movePart = moveText.trim().split(Regex("""\s+""")).getOrNull(1)?.substringBefore("(") ?: moveText
    return "$colorSymbol$movePart"
}

object BoardLayout {
    fun getSujiLabels(): List<String> = listOf("９", "８", "７", "６", "５", "４", "３", "２", "１")

    fun getDanLabels(): List<String> = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")

    fun toShogiNotation(file: Int, rank: Int): String {
        val suji = getSujiLabels().reversed().getOrNull(file - 1) ?: ""
        val dan = getDanLabels().getOrNull(rank - 1) ?: ""
        return suji + dan
    }

    fun getRangeX(isFlipped: Boolean): IntProgression = if (isFlipped) (ShogiConstants.MAX_INDEX downTo 0) else (0..ShogiConstants.MAX_INDEX)

    fun getRangeY(isFlipped: Boolean): IntProgression = if (isFlipped) (ShogiConstants.MAX_INDEX downTo 0) else (0..ShogiConstants.MAX_INDEX)
}
