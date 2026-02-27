package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants

private object HeaderCsaConstants {
    const val START_INDEX = 2
    const val ENTRY_LENGTH = 4
    const val NAME_OFFSET = 2
    const val NAME_LENGTH = 2
    const val PIECE_WIDTH = 3
}

internal fun handleCsaMetadataLine(hp: HeaderParser, line: String) {
    when {
        line.startsWith("N+") -> hp.senteName = line.substring(2).trim()
        line.startsWith("N-") -> hp.goteName = line.substring(2).trim()
        line.startsWith("\$START_TIME:") -> hp.startTime = line.substringAfter(":").trim()
        line.startsWith("\$EVENT:") -> hp.event = line.substringAfter(":").trim()
    }
}

internal fun handleCsaMochigomaLine(hp: HeaderParser, line: String) {
    val color = if (line[1] == '+') PieceColor.Black else PieceColor.White
    var i = HeaderCsaConstants.START_INDEX
    while (i + HeaderCsaConstants.ENTRY_LENGTH <= line.length) {
        val pieceStr = line.substring(i + HeaderCsaConstants.NAME_OFFSET, i + HeaderCsaConstants.NAME_OFFSET + HeaderCsaConstants.NAME_LENGTH)
        addCsaMochigoma(hp, pieceStr, color)
        i += HeaderCsaConstants.ENTRY_LENGTH
    }
}

private fun addCsaMochigoma(hp: HeaderParser, pieceStr: String, color: PieceColor) {
    if (pieceStr == "AL") return
    val piece = Piece.entries.find { it.name == pieceStr } ?: return
    if (color == PieceColor.Black) hp.senteMochi.add(piece) else hp.goteMochi.add(piece)
}

internal fun parseCsaBoardLine(hp: HeaderParser, line: String) {
    val rowIdx = line[1] - '1'
    if (rowIdx !in 0 until ShogiConstants.BOARD_SIZE) return
    for (i in 0 until ShogiConstants.BOARD_SIZE) {
        val start = HeaderCsaConstants.START_INDEX + i * HeaderCsaConstants.PIECE_WIDTH
        if (start + HeaderCsaConstants.PIECE_WIDTH > line.length) break
        val pStr = line.substring(start, start + HeaderCsaConstants.PIECE_WIDTH)
        hp.currentCells[rowIdx][i] = when {
            pStr == " * " -> null
            pStr.startsWith("+") -> BoardPiece(Piece.entries.find { it.name == pStr.substring(1) } ?: Piece.FU, PieceColor.Black)
            pStr.startsWith("-") -> BoardPiece(Piece.entries.find { it.name == pStr.substring(1) } ?: Piece.FU, PieceColor.White)
            else -> null
        }
    }
}
