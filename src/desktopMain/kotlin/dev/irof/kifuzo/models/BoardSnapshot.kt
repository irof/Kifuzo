package dev.irof.kifuzo.models

/**
 * 盤面の特定の時点での状態（盤上の駒と両対局者の持駒）を表します。
 */
data class BoardSnapshot(
    val cells: List<List<BoardPiece?>>,
    val senteMochigoma: List<Piece> = emptyList(),
    val goteMochigoma: List<Piece> = emptyList(),
    val lastFrom: Square? = null,
    val lastTo: Square? = null,
) {
    /**
     * このスナップショットが標準の平手初期配置であるか判定します。
     */
    fun isStandardInitial(): Boolean = senteMochigoma.isEmpty() && goteMochigoma.isEmpty() && cells == getInitialCells()

    /**
     * 指定された座標の駒を取得します。
     */
    fun at(square: Square): BoardPiece? = cells[square.yIndex][square.xIndex]

    /**
     * 指定された座標の駒を取得します（file, rank 指定）。
     */
    fun at(file: Int, rank: Int): BoardPiece? = at(Square(file, rank))

    /**
     * 通常の指し手を適用し、新しい局面を返します。
     */
    fun applyMove(
        from: Square,
        to: Square,
        isPromote: Boolean,
        turnColor: PieceColor,
        fallbackPiece: Piece? = null,
    ): BoardSnapshot {
        val current = at(from)
        val captured = at(to)

        val newSenteMochi = senteMochigoma.toMutableList()
        val newGoteMochi = goteMochigoma.toMutableList()

        if (captured != null) {
            val mochiPiece = captured.piece.toBase()
            if (turnColor == PieceColor.Black) newSenteMochi.add(mochiPiece) else newGoteMochi.add(mochiPiece)
        }

        val basePiece = current?.piece ?: fallbackPiece ?: Piece.FU
        val movingPiece = if (isPromote) basePiece.promote() else basePiece
        val newCells = cells.map { it.toMutableList() }.toMutableList()
        newCells[from.yIndex][from.xIndex] = null
        newCells[to.yIndex][to.xIndex] = BoardPiece(movingPiece, turnColor)

        return copy(
            cells = newCells.map { it.toList() },
            senteMochigoma = newSenteMochi.toList(),
            goteMochigoma = newGoteMochi.toList(),
            lastFrom = from,
            lastTo = to,
        )
    }

    /**
     * 駒打ちを適用し、新しい局面を返します。
     */
    fun applyDrop(
        piece: Piece,
        to: Square,
        turnColor: PieceColor,
    ): BoardSnapshot {
        val newSenteMochi = senteMochigoma.toMutableList()
        val newGoteMochi = goteMochigoma.toMutableList()

        if (turnColor == PieceColor.Black) newSenteMochi.remove(piece) else newGoteMochi.remove(piece)

        val newCells = cells.map { it.toMutableList() }.toMutableList()
        newCells[to.yIndex][to.xIndex] = BoardPiece(piece, turnColor)

        return copy(
            cells = newCells.map { it.toList() },
            senteMochigoma = newSenteMochi.toList(),
            goteMochigoma = newGoteMochi.toList(),
            lastFrom = null,
            lastTo = to,
        )
    }

    companion object {
        /**
         * 標準の平手初期配置を取得します。
         */
        @Suppress("MagicNumber")
        fun getInitialCells(): List<List<BoardPiece?>> {
            val cells = Array(9) { arrayOfNulls<BoardPiece>(9) }
            cells[0][0] = BoardPiece(Piece.KY, PieceColor.White)
            cells[0][1] = BoardPiece(Piece.KE, PieceColor.White)
            cells[0][2] = BoardPiece(Piece.GI, PieceColor.White)
            cells[0][3] = BoardPiece(Piece.KI, PieceColor.White)
            cells[0][4] = BoardPiece(Piece.OU, PieceColor.White)
            cells[0][5] = BoardPiece(Piece.KI, PieceColor.White)
            cells[0][6] = BoardPiece(Piece.GI, PieceColor.White)
            cells[0][7] = BoardPiece(Piece.KE, PieceColor.White)
            cells[0][8] = BoardPiece(Piece.KY, PieceColor.White)
            cells[1][1] = BoardPiece(Piece.HI, PieceColor.White)
            cells[1][7] = BoardPiece(Piece.KA, PieceColor.White)
            for (i in 0..8) cells[2][i] = BoardPiece(Piece.FU, PieceColor.White)

            cells[8][0] = BoardPiece(Piece.KY, PieceColor.Black)
            cells[8][1] = BoardPiece(Piece.KE, PieceColor.Black)
            cells[8][2] = BoardPiece(Piece.GI, PieceColor.Black)
            cells[8][3] = BoardPiece(Piece.KI, PieceColor.Black)
            cells[8][4] = BoardPiece(Piece.OU, PieceColor.Black)
            cells[8][5] = BoardPiece(Piece.KI, PieceColor.Black)
            cells[8][6] = BoardPiece(Piece.GI, PieceColor.Black)
            cells[8][7] = BoardPiece(Piece.KE, PieceColor.Black)
            cells[8][8] = BoardPiece(Piece.KY, PieceColor.Black)
            cells[7][1] = BoardPiece(Piece.KA, PieceColor.Black)
            cells[7][7] = BoardPiece(Piece.HI, PieceColor.Black)
            for (i in 0..8) cells[6][i] = BoardPiece(Piece.FU, PieceColor.Black)

            return cells.map { it.toList() }
        }
    }
}
