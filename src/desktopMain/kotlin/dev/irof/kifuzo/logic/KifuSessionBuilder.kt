package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuSession
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

    private val currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<Pair<Piece, PieceColor>>(ShogiConstants.BOARD_SIZE) }
    private val senteMochi = mutableListOf<Piece>()
    private val goteMochi = mutableListOf<Piece>()
    private val history = mutableListOf<BoardSnapshot>()

    var lastEvaluation: Evaluation
        get() = history.lastOrNull()?.evaluation ?: Evaluation.Unknown
        set(value) {
            if (history.isNotEmpty()) {
                val lastIdx = history.size - 1
                history[lastIdx] = history[lastIdx].copy(evaluation = value)
            }
        }

    /**
     * 初期状態を設定します。snapshotが指定された場合はその局面から（変化手順用）、
     * 指定されない場合は初期盤面から開始します。
     */
    fun setup(
        senteName: String = "先手",
        goteName: String = "後手",
        startTime: String = "",
        event: String = "",
        initialCells: List<List<Pair<Piece, PieceColor>?>> = BoardSnapshot.getInitialCells(),
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

        history.clear()
        val initialMoveText = if (snapshot != null) "分岐元" else "開始局面"
        history.add(createSnapshot(initialMoveText).copy(variations = emptyList()))
    }

    /**
     * 指定された手数の局面を取得します。
     */
    fun getSnapshotAt(step: Int): BoardSnapshot? = history.getOrNull(step - startingStep)

    val historySize: Int get() = history.size
    val currentStartingStep: Int get() = startingStep

    /**
     * 指し手を適用します。dropPieceが指定された場合は駒打ち、そうでない場合は移動として処理します。
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
        history.add(createSnapshot(resultText, evaluation = evaluation))
    }

    private fun applyDropAction(piece: Piece?, to: Square, consumptionSeconds: Int?, moveText: String) {
        val dropPiece = piece ?: throw IllegalArgumentException("駒打ちには駒の指定が必要です。")
        val turnColor = getTurnColor()
        if (turnColor == PieceColor.Black) senteMochi.remove(dropPiece) else goteMochi.remove(dropPiece)
        currentCells[to.yIndex][to.xIndex] = dropPiece to turnColor
        history.add(createSnapshot(moveText, null, to, consumptionSeconds))
    }

    private fun applyMoveAction(from: Square, to: Square, isPromote: Boolean, consumptionSeconds: Int?, moveText: String) {
        val turnColor = getTurnColor()
        val current = currentCells[from.yIndex][from.xIndex] ?: throw KifuParseException("移動元($from)に駒がありません。")
        val captured = currentCells[to.yIndex][to.xIndex]
        if (captured != null) {
            if (turnColor == PieceColor.Black) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase())
            if (firstContactStep == -1) firstContactStep = history.size
        }
        val movingPiece = if (isPromote) current.first.promote() else current.first
        currentCells[to.yIndex][to.xIndex] = movingPiece to turnColor
        currentCells[from.yIndex][from.xIndex] = null
        history.add(createSnapshot(moveText, from, to, consumptionSeconds))
    }

    /**
     * 盤面の状態（初期局面）を更新します。
     */
    fun updateInitialState(
        y: Int? = null,
        row: List<Pair<Piece, PieceColor>?>? = null,
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
        if (history.isNotEmpty()) {
            history[0] = createSnapshot("開始局面")
        }
    }

    /**
     * 指定された手数に変化手順を追加します。
     */
    fun addVariation(atStep: Int, variationHistory: List<BoardSnapshot>) {
        val index = atStep - startingStep
        if (index in history.indices) {
            val target = history[index]
            history[index] = target.copy(variations = target.variations + listOf(variationHistory))
        }
    }

    /**
     * 構築された KifuSession を返します。
     */
    fun build(): KifuSession {
        val initialStep = when {
            !isStandardStart -> 0
            firstContactStep != -1 -> firstContactStep
            else -> history.size - 1
        }
        return KifuSession(
            history = history.toList(),
            initialStep = initialStep,
            senteName = senteName,
            goteName = goteName,
            startTime = startTime,
            event = event,
            firstContactStep = firstContactStep,
            isStandardStart = isStandardStart,
        )
    }

    private fun getTurnColor(): PieceColor = if ((startingStep + history.size) % 2 != 0) PieceColor.Black else PieceColor.White

    private fun createSnapshot(
        moveText: String,
        lastFrom: Square? = null,
        lastTo: Square? = null,
        consumptionSeconds: Int? = null,
        evaluation: Evaluation = Evaluation.Unknown,
    ): BoardSnapshot = BoardSnapshot(
        cells = currentCells.map { it.toList() },
        senteMochigoma = senteMochi.toList(),
        goteMochigoma = goteMochi.toList(),
        lastMoveText = moveText,
        lastFrom = lastFrom,
        lastTo = lastTo,
        consumptionSeconds = consumptionSeconds,
        evaluation = evaluation,
    )
}
