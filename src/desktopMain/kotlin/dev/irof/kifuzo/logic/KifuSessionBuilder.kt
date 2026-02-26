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
 * 棋譜のパース中に盤面状態を管理し、KifuSession を構築するためのビルダー。
 */
class KifuSessionBuilder {
    var senteName: String = "先手"
    var goteName: String = "後手"
    var startTime: String = ""
    var event: String = ""
    private var isStandardStart: Boolean = true
    private var firstContactStep: Int = -1
    private var startingStep: Int = 0

    private val currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<BoardPiece>(ShogiConstants.BOARD_SIZE) }
    private val senteMochi = mutableListOf<Piece>()
    private val goteMochi = mutableListOf<Piece>()

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
        this.senteMochi.clear()
        this.senteMochi.addAll(snapshot?.senteMochigoma ?: senteMochi)
        this.goteMochi.clear()
        this.goteMochi.addAll(snapshot?.goteMochigoma ?: goteMochi)

        val cellsToUse = snapshot?.cells ?: initialCells
        for (y in 0 until ShogiConstants.BOARD_SIZE) {
            for (x in 0 until ShogiConstants.BOARD_SIZE) {
                currentCells[y][x] = cellsToUse[y][x]
            }
        }

        initialSnapshot = snapshot ?: createSnapshot()
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
        if (resultText != null) {
            applyResultAction(resultText)
        } else {
            val targetTo = to ?: throw IllegalArgumentException("移動先(to)の指定が必要です。")
            if (from == null) {
                applyDropAction(piece, targetTo, consumptionSeconds, moveText)
            } else {
                applyMoveAction(from, targetTo, isPromote, consumptionSeconds, moveText)
            }
        }
    }

    private fun applyResultAction(resultText: String) {
        val evaluation = if (getTurnColor() == PieceColor.Black) Evaluation.GoteWin else Evaluation.SenteWin
        moves.add(
            Move(
                step = startingStep + moves.size + 1,
                moveText = resultText,
                resultSnapshot = createSnapshot(),
                evaluation = evaluation,
            ),
        )
    }

    private fun applyDropAction(piece: Piece?, to: Square, consumptionSeconds: Int?, moveText: String) {
        val dropPiece = piece ?: throw IllegalArgumentException("駒打ちには駒の指定が必要です。")
        val turnColor = getTurnColor()
        if (turnColor == PieceColor.Black) senteMochi.remove(dropPiece) else goteMochi.remove(dropPiece)
        currentCells[to.yIndex][to.xIndex] = BoardPiece(dropPiece, turnColor)
        moves.add(
            Move(
                step = startingStep + moves.size + 1,
                moveText = moveText,
                resultSnapshot = createSnapshot(null, to),
                consumptionSeconds = consumptionSeconds,
            ),
        )
    }

    private fun applyMoveAction(from: Square, to: Square, isPromote: Boolean, consumptionSeconds: Int?, moveText: String) {
        val turnColor = getTurnColor()
        val current = currentCells[from.yIndex][from.xIndex] ?: throw KifuParseException("移動元($from)に駒がありません。")
        val captured = currentCells[to.yIndex][to.xIndex]
        if (captured != null) {
            if (turnColor == PieceColor.Black) senteMochi.add(captured.piece.toBase()) else goteMochi.add(captured.piece.toBase())
            if (firstContactStep == -1) firstContactStep = startingStep + moves.size + 1
        }
        val movingPiece = if (isPromote) current.piece.promote() else current.piece
        currentCells[to.yIndex][to.xIndex] = BoardPiece(movingPiece, turnColor)
        currentCells[from.yIndex][from.xIndex] = null
        moves.add(
            Move(
                step = startingStep + moves.size + 1,
                moveText = moveText,
                resultSnapshot = createSnapshot(from, to),
                consumptionSeconds = consumptionSeconds,
            ),
        )
    }

    /**
     * 盤面の状態（初期局面）を更新します。
     */
    fun updateInitialState(
        y: Int? = null,
        row: List<BoardPiece?>? = null,
        mochigomaColor: PieceColor? = null,
        mochigomaPiece: Piece? = null,
    ) {
        this.isStandardStart = false
        if (y != null && row != null) {
            for (x in 0 until ShogiConstants.BOARD_SIZE) {
                currentCells[y][x] = row[x]
            }
        }
        if (mochigomaColor != null && mochigomaPiece != null) {
            if (mochigomaColor == PieceColor.Black) senteMochi.add(mochigomaPiece) else goteMochi.add(mochigomaPiece)
        }
        initialSnapshot = createSnapshot()
    }

    /**
     * 指定された手数に変化手順を追加します。
     */
    fun addVariation(atStep: Int, variationMoves: List<Move>) {
        val index = atStep - startingStep
        if (index == 0) {
            // 開始局面からの分岐
            initialSnapshot = initialSnapshot.copy() // Not exactly right, but variations are usually on Moves
        } else if (index in 1..moves.size) {
            val target = moves[index - 1]
            moves[index - 1] = target.copy(variations = target.variations + listOf(variationMoves))
        }
    }

    /**
     * 構築された KifuSession を返します。
     */
    fun build(): KifuSession {
        val finalIsStandardStart = isStandardStart || initialSnapshot.isStandardInitial()
        val initialStep = when {
            !finalIsStandardStart -> 0
            firstContactStep != -1 -> firstContactStep
            else -> moves.size
        }
        return KifuSession(
            initialSnapshot = initialSnapshot,
            moves = moves.toList(),
            initialStep = initialStep,
            senteName = senteName,
            goteName = goteName,
            startTime = startTime,
            event = event,
            firstContactStep = firstContactStep,
            isStandardStart = finalIsStandardStart,
        )
    }

    private fun getTurnColor(): PieceColor = if ((startingStep + moves.size + 1) % 2 != 0) PieceColor.Black else PieceColor.White

    private fun createSnapshot(lastFrom: Square? = null, lastTo: Square? = null): BoardSnapshot = BoardSnapshot(
        cells = currentCells.map { it.toList() },
        senteMochigoma = senteMochi.toList(),
        goteMochigoma = goteMochi.toList(),
        lastFrom = lastFrom,
        lastTo = lastTo,
    )
}
