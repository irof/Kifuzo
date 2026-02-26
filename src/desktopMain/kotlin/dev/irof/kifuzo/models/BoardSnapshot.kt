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
