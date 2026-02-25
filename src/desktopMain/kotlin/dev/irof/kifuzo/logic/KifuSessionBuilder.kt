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
    private var senteName: String = "先手"
    private var goteName: String = "後手"
    private var startTime: String = ""
    private var event: String = ""
    private var isStandardStart: Boolean = true
    private var firstContactStep: Int = -1
    private var startingStep: Int = 0

    private val currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<Pair<Piece, PieceColor>>(ShogiConstants.BOARD_SIZE) }
    private val senteMochi = mutableListOf<Piece>()
    private val goteMochi = mutableListOf<Piece>()
    private val history = mutableListOf<BoardSnapshot>()

    /**
     * 初期状態を設定します。
     */
    fun setup(
        senteName: String = "先手",
        goteName: String = "後手",
        initialCells: List<List<Pair<Piece, PieceColor>?>> = BoardSnapshot.getInitialCells(),
        senteMochi: List<Piece> = emptyList(),
        goteMochi: List<Piece> = emptyList(),
        isStandardStart: Boolean = true,
        startTime: String = "",
        event: String = "",
    ) {
        this.senteName = senteName
        this.goteName = goteName
        this.startTime = startTime
        this.event = event
        this.isStandardStart = isStandardStart
        this.startingStep = 0
        this.senteMochi.clear()
        this.senteMochi.addAll(senteMochi)
        this.goteMochi.clear()
        this.goteMochi.addAll(goteMochi)

        for (y in 0 until ShogiConstants.BOARD_SIZE) {
            for (x in 0 until ShogiConstants.BOARD_SIZE) {
                currentCells[y][x] = initialCells[y][x]
            }
        }

        history.clear()
        history.add(createSnapshot("開始局面"))
    }

    /**
     * 特定の局面から開始するように設定します（変化手順用）。
     */
    fun setupFromSnapshot(snapshot: BoardSnapshot, step: Int) {
        this.startingStep = step
        this.senteMochi.clear()
        this.senteMochi.addAll(snapshot.senteMochigoma)
        this.goteMochi.clear()
        this.goteMochi.addAll(snapshot.goteMochigoma)

        for (y in 0 until ShogiConstants.BOARD_SIZE) {
            for (x in 0 until ShogiConstants.BOARD_SIZE) {
                currentCells[y][x] = snapshot.cells[y][x]
            }
        }

        history.clear()
        // 分岐元局面を開始局面として追加。ただし、変化リストは空にしておく（無限再帰防止）。
        history.add(snapshot.copy(variations = emptyList(), lastMoveText = "分岐元"))
    }

    /**
     * メタデータを設定します。
     */
    fun setMetadata(sente: String, gote: String, start: String = "", ev: String = "") {
        this.senteName = sente
        this.goteName = gote
        this.startTime = start
        this.event = ev
    }

    /**
     * 指定された手数の局面を取得します。
     */
    fun getSnapshotAt(step: Int): BoardSnapshot? = history.getOrNull(step - startingStep)

    val historySize: Int get() = history.size
    val currentStartingStep: Int get() = startingStep

    /**
     * 通常の指し手を適用します。
     */
    fun applyMove(
        from: Square,
        to: Square,
        isPromote: Boolean,
        consumptionSeconds: Int?,
        moveText: String,
    ) {
        val turnColor = getTurnColor()
        val current = currentCells[from.yIndex][from.xIndex] ?: throw KifuParseException("移動元($from)に駒がありません。")

        // 駒を取る処理
        val captured = currentCells[to.yIndex][to.xIndex]
        if (captured != null) {
            if (turnColor == PieceColor.Black) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase())
            if (firstContactStep == -1) firstContactStep = history.size
        }

        // 盤面の更新
        val piece = if (isPromote) current.first.promote() else current.first
        currentCells[to.yIndex][to.xIndex] = piece to turnColor
        currentCells[from.yIndex][from.xIndex] = null

        history.add(createSnapshot(moveText, from, to, consumptionSeconds))
    }

    /**
     * 駒打ちを適用します。
     */
    fun applyDrop(
        piece: Piece,
        to: Square,
        consumptionSeconds: Int?,
        moveText: String,
    ) {
        val turnColor = getTurnColor()
        if (turnColor == PieceColor.Black) senteMochi.remove(piece) else goteMochi.remove(piece)

        currentCells[to.yIndex][to.xIndex] = piece to turnColor

        history.add(createSnapshot(moveText, null, to, consumptionSeconds))
    }

    /**
     * 終局結果を適用します。
     */
    fun applyResult(resultText: String) {
        val turnColor = getTurnColor()
        val evaluation = if (turnColor == PieceColor.Black) Evaluation.GoteWin else Evaluation.SenteWin
        history.add(createSnapshot(resultText, evaluation = evaluation))
    }

    /**
     * 最新の局面の評価値を更新します。
     */
    fun updateLastEvaluation(evaluation: Evaluation) {
        if (history.isEmpty()) return
        val lastIdx = history.size - 1
        history[lastIdx] = history[lastIdx].copy(evaluation = evaluation)
    }

    /**
     * 最新の局面の評価値を取得します。
     */
    fun getLastEvaluation(): Evaluation = history.lastOrNull()?.evaluation ?: Evaluation.Unknown

    /**
     * 盤面の特定行を更新します（初期局面の設定用）。
     */
    fun updateInitialBoardRow(y: Int, row: List<Pair<Piece, PieceColor>?>) {
        this.isStandardStart = false
        for (x in 0 until ShogiConstants.BOARD_SIZE) {
            currentCells[y][x] = row[x]
        }
        // 最初のスナップショット（開始局面）を更新
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
        val initialStep = if (firstContactStep != -1) firstContactStep else (history.size - 1)
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
