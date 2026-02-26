package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square

/**
 * 棋譜のパース中に局面を構築し、KifuSession を生成するためのビルダー。
 */
class KifuSessionBuilder {
    var senteName: String = "先手"
    var goteName: String = "後手"
    var startTime: String = ""
    var event: String = ""
    private var isStandardStart: Boolean = true
    private var firstContactStep: Int = -1
    private var startingStep: Int = 0

    private var initialSnapshot: BoardSnapshot = BoardSnapshot(BoardSnapshot.getInitialCells())
    private val moves = mutableListOf<Move>()

    var lastEvaluation: Evaluation
        get() = moves.lastOrNull()?.evaluation ?: Evaluation.Unknown
        set(value) {
            if (moves.isNotEmpty()) {
                val lastIdx = moves.size - 1
                moves[lastIdx] = moves[lastIdx].copy(evaluation = value)
            }
        }

    /**
     * 初期状態を設定します。
     */
    fun setup(
        senteName: String = "先手",
        goteName: String = "後手",
        startTime: String = "",
        event: String = "",
        initialCells: List<List<BoardPiece?>> = BoardSnapshot.getInitialCells(),
        senteMochi: List<Piece> = emptyList(),
        goteMochi: List<Piece> = emptyList(),
        isStandardStart: Boolean = true,
        startingStep: Int = 0,
        snapshot: BoardSnapshot? = null,
    ) {
        this.senteName = senteName
        this.goteName = goteName
        this.startTime = startTime
        this.event = event
        this.isStandardStart = isStandardStart
        this.startingStep = if (snapshot != null) startingStep else 0

        initialSnapshot = snapshot ?: BoardSnapshot(
            cells = initialCells,
            senteMochigoma = senteMochi,
            goteMochigoma = goteMochi,
        )
        moves.clear()
    }

    /**
     * 指定された手数の局面を取得します。
     */
    fun getSnapshotAt(step: Int): BoardSnapshot? = when {
        step == startingStep -> initialSnapshot
        step > startingStep && step <= startingStep + moves.size -> moves[step - startingStep - 1].resultSnapshot
        else -> null
    }

    val historySize: Int get() = moves.size + 1
    val currentStartingStep: Int get() = startingStep

    /**
     * 指し手を適用します。
     */
    fun applyAction(
        from: Square? = null,
        to: Square? = null,
        piece: Piece? = null,
        isPromote: Boolean = false,
        consumptionSeconds: Int?,
        moveText: String,
        resultText: String? = null,
    ) {
        val currentSnapshot = moves.lastOrNull()?.resultSnapshot ?: initialSnapshot
        val currentStep = startingStep + moves.size + 1

        val move = if (resultText != null) {
            Move.createResult(currentStep, resultText, currentSnapshot)
        } else {
            val targetTo = to ?: throw IllegalArgumentException("toが必要です")
            if (from == null) {
                Move.createDrop(currentStep, piece ?: throw IllegalArgumentException("pieceが必要です"), targetTo, moveText, currentSnapshot, consumptionSeconds)
            } else {
                if (firstContactStep == -1 && currentSnapshot.at(targetTo) != null) firstContactStep = currentStep
                Move.createMove(currentStep, from, targetTo, isPromote, moveText, currentSnapshot, consumptionSeconds)
            }
        }
        moves.add(move)
    }

    /**
     * 盤面の状態（初期局面）を更新します（CSAなどで手動で初期盤面を作る用）。
     */
    fun updateInitialState(
        y: Int? = null,
        row: List<BoardPiece?>? = null,
        mochigomaColor: PieceColor? = null,
        mochigomaPiece: Piece? = null,
    ) {
        this.isStandardStart = false
        var cells = initialSnapshot.cells
        var senteMochi = initialSnapshot.senteMochigoma
        var goteMochi = initialSnapshot.goteMochigoma

        if (y != null && row != null) {
            val newCells = cells.map { it.toMutableList() }.toMutableList()
            newCells[y] = row.toMutableList()
            cells = newCells.map { it.toList() }
        }
        if (mochigomaColor != null && mochigomaPiece != null) {
            if (mochigomaColor == PieceColor.Black) senteMochi = senteMochi + listOf(mochigomaPiece) else goteMochi = goteMochi + listOf(mochigomaPiece)
        }

        initialSnapshot = initialSnapshot.copy(cells = cells, senteMochigoma = senteMochi, goteMochigoma = goteMochi)
    }

    /**
     * 変化手順を追加します。
     */
    fun addVariation(atStep: Int, variationMoves: List<Move>) {
        val index = atStep - startingStep
        if (index in 1..moves.size) {
            val target = moves[index - 1]
            moves[index - 1] = target.copy(variations = target.variations + listOf(variationMoves))
        }
    }

    /**
     * KifuSession を構築します。
     */
    fun build(): KifuSession {
        val finalIsStandardStart = isStandardStart || initialSnapshot.isStandardInitial()
        val initialStep = if (!finalIsStandardStart) {
            0
        } else if (firstContactStep != -1) {
            firstContactStep
        } else {
            moves.size
        }
        return KifuSession(
            initialSnapshot = initialSnapshot, moves = moves.toList(), initialStep = initialStep,
            senteName = senteName, goteName = goteName, startTime = startTime, event = event,
            firstContactStep = firstContactStep, isStandardStart = finalIsStandardStart,
        )
    }
}
