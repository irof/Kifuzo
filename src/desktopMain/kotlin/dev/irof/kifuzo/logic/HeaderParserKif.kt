package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants

internal fun handleMetadataLine(hp: HeaderParser, line: String) {
    when {
        line.startsWith("後手：") -> hp.goteName = line.substringAfter("：").trim()
        line.startsWith("先手：") || line.startsWith("対局者：") -> hp.senteName = line.substringAfter("：").trim()
        line.startsWith("開始日時：") -> hp.startTime = line.substringAfter("：").trim()
        line.startsWith("棋戦：") -> hp.event = line.substringAfter("：").trim()
        else -> handleCsaMetadataLine(hp, line)
    }
}

internal fun handleMochigomaLine(hp: HeaderParser, line: String) {
    hp.isStandardStart = false
    if (line.startsWith("P+") || line.startsWith("P-")) {
        handleCsaMochigomaLine(hp, line)
    } else {
        val pieces = Piece.parseMochigoma(line.substringAfter("："))
        if (line.startsWith("先手") || line.startsWith("下手")) hp.senteMochi.addAll(pieces) else hp.goteMochi.addAll(pieces)
    }
}

internal fun handleBoardLine(hp: HeaderParser, line: String) {
    hp.prepareNonStandardBoard()
    if (line.startsWith("|")) {
        parseKifBoardLine(hp, line)
    } else {
        parseCsaBoardLine(hp, line)
    }
}

private fun parseKifBoardLine(hp: HeaderParser, line: String) {
    if (hp.boardY >= ShogiConstants.BOARD_SIZE) return
    val content = line.substringAfter("|").substringBeforeLast("|")
    val cells = splitKifBoardCells(content)
    for (x in 0 until ShogiConstants.BOARD_SIZE) {
        if (x < cells.size) {
            val pStr = cells[x]
            Piece.findPieceBySymbol(pStr.replace("v", "").replace("・", "").trim())?.let {
                hp.currentCells[hp.boardY][x] = BoardPiece(it, if (pStr.contains('v')) PieceColor.White else PieceColor.Black)
            }
        }
    }
    hp.boardY++
}

private fun splitKifBoardCells(content: String): List<String> = if (content.contains("|")) {
    content.split("|")
} else {
    (0 until ShogiConstants.BOARD_SIZE).map { idx ->
        val start = idx * 2
        if (start + 2 <= content.length) content.substring(start, start + 2) else ""
    }
}
