package dev.irof.kifuzo.models

/**
 * 一つの棋譜ファイルの解析結果全体を保持するクラス
 */
data class KifuSession(
    val history: List<BoardSnapshot> = emptyList(),
    val initialStep: Int = 0,
    val senteName: String = "先手",
    val goteName: String = "後手",
    val startTime: String = "",
    val event: String = "",
    val firstContactStep: Int = -1,
    val isStandardStart: Boolean = true,
) {
    // 最後に有効なインデックス
    val maxStep: Int get() = if (history.isEmpty()) 0 else history.size - 1

    /**
     * 手数が範囲内にあることを保証します。
     */
    fun coerceStep(step: Int): Int = step.coerceIn(0, maxStep)
}
