package dev.irof.kfv.models

data class BoardSnapshot(
    val cells: List<List<Pair<Piece, PieceColor>?>>,
    val senteMochigoma: List<Piece> = emptyList(),
    val goteMochigoma: List<Piece> = emptyList(),
    val lastMoveText: String = "",
    val lastFrom: Square? = null,
    val lastTo: Square? = null,
) {
    companion object {
        /**
         * 標準の平手初期配置を取得します。
         */
        fun getInitialCells(): List<List<Pair<Piece, PieceColor>?>> {
            val cells = Array(9) { arrayOfNulls<Pair<Piece, PieceColor>>(9) }
            cells[0][0] = Piece.KY to PieceColor.White
            cells[0][1] = Piece.KE to PieceColor.White
            cells[0][2] = Piece.GI to PieceColor.White
            cells[0][3] = Piece.KI to PieceColor.White
            cells[0][4] = Piece.OU to PieceColor.White
            cells[0][5] = Piece.KI to PieceColor.White
            cells[0][6] = Piece.GI to PieceColor.White
            cells[0][7] = Piece.KE to PieceColor.White
            cells[0][8] = Piece.KY to PieceColor.White
            cells[1][1] = Piece.HI to PieceColor.White
            cells[1][7] = Piece.KA to PieceColor.White
            for (i in 0..8) cells[2][i] = Piece.FU to PieceColor.White
            cells[8][0] = Piece.KY to PieceColor.Black
            cells[8][1] = Piece.KE to PieceColor.Black
            cells[8][2] = Piece.GI to PieceColor.Black
            cells[8][3] = Piece.KI to PieceColor.Black
            cells[8][4] = Piece.OU to PieceColor.Black
            cells[8][5] = Piece.KI to PieceColor.Black
            cells[8][6] = Piece.GI to PieceColor.Black
            cells[8][7] = Piece.KE to PieceColor.Black
            cells[8][8] = Piece.KY to PieceColor.Black
            cells[7][1] = Piece.KA to PieceColor.Black
            cells[7][7] = Piece.HI to PieceColor.Black
            for (i in 0..8) cells[6][i] = Piece.FU to PieceColor.Black
            return cells.map { it.toList() }
        }
    }
}
