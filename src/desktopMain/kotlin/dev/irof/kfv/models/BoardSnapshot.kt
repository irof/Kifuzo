package dev.irof.kfv.models

data class BoardSnapshot(
    val cells: Array<Array<Pair<Piece, PieceColor>?>>,
    val senteMochigoma: List<Piece> = emptyList(),
    val goteMochigoma: List<Piece> = emptyList(),
    val lastMoveText: String = "",
    val lastFrom: Square? = null,
    val lastTo: Square? = null,
)
