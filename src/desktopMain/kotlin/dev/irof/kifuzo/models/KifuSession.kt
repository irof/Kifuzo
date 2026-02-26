package dev.irof.kifuzo.models

/**
 * 一つの棋譜ファイルの解析結果全体を保持するクラス
 */
data class KifuSession(
    val initialSnapshot: BoardSnapshot = BoardSnapshot(BoardSnapshot.getInitialCells()),
    val moves: List<Move> = emptyList(),
    val initialStep: Int = 0,
    val senteName: String = "先手",
    val goteName: String = "後手",
    val startTime: String = "",
    val event: String = "",
    val firstContactStep: Int = -1,
    val isStandardStart: Boolean = true,
) {
    /**
     * 指定された手数の局面を取得します。
     * 0手目は initialSnapshot、それ以降は moves[step-1].resultSnapshot を返します。
     */
    fun getSnapshotAt(step: Int): BoardSnapshot = if (step <= 0 || moves.isEmpty()) {
        initialSnapshot
    } else {
        moves[coerceStep(step) - 1].resultSnapshot
    }

    /**
     * 全局面のリスト（0手目を含む）を返します。
     */
    val history: List<BoardSnapshot> get() = listOf(initialSnapshot) + moves.map { it.resultSnapshot }

    // 最後に有効なインデックス
    val maxStep: Int get() = moves.size

    /**
     * 手数が範囲内にあることを保証します。
     */
    fun coerceStep(step: Int): Int = step.coerceIn(0, maxStep)
}
