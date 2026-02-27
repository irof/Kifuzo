package dev.irof.kifuzo.logic.parser
import dev.irof.kifuzo.logic.io.*
import dev.irof.kifuzo.logic.service.*
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants

internal fun handleMetadataLine(hp: HeaderParser, line: String) {
    when {
        line.startsWith("後手：") -> hp.goteName = line.substringAfter("：").trim()
        line.startsWith("先手：") || line.startsWith("対局者：") -> hp.senteName = line.substringAfter("：").trim()
        line.startsWith("開始日時：") -> hp.startTime = line.substringAfter("：").trim()
        line.startsWith("棋戦：") -> {
            val event = line.substringAfter("：").trim()
            if (hp.event.isEmpty()) {
                hp.event = event
            } else if (!hp.event.contains(event)) {
                hp.event = "$event (${hp.event})"
            }
        }
        line.startsWith("場所：") -> {
            val place = line.substringAfter("：").trim()
            if (hp.event.isEmpty()) {
                hp.event = place
            } else if (!hp.event.contains(place)) {
                hp.event += " ($place)"
            }
        }
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

/**
 * 一文字の駒名（例: "歩", "王", "竜"）を解析して Piece を返します。
 */
internal fun Piece.Companion.findPieceBySymbol(symbol: String): Piece? {
    val s = symbol.trim()
    if (s.isEmpty()) return null
    return Piece.entries.find {
        it.symbol == s ||
            (s == "王" && it == Piece.OU) ||
            (s == "玉" && it == Piece.OU) ||
            (s == "竜" && it == Piece.RY) ||
            (s == "龍" && it == Piece.RY) ||
            (s == "馬" && it == Piece.UM)
    }
}

/**
 * 持ち駒文字列（例: "飛二 角 銀三"）を解析して Piece のリストを返します。
 */
internal fun Piece.Companion.parseMochigoma(text: String): List<Piece> {
    val t = text.trim()
    if (t == "なし" || t.isEmpty()) return emptyList()
    val list = mutableListOf<Piece>()
    val kanjiDigits = "一二三四五六七八九"
    t.split(Regex("""[\s　]+""")).forEach { part ->
        if (part.isEmpty()) return@forEach
        val pieceName = part.substring(0, 1)
        val countStr = part.substring(1)
        val count = if (countStr.isEmpty()) {
            1
        } else {
            val idx = kanjiDigits.indexOf(countStr)
            if (idx != -1) idx + 1 else countStr.toIntOrNull() ?: 1
        }
        val piece = findPieceBySymbol(pieceName)
        if (piece != null) repeat(count) { list.add(piece) }
    }
    return list
}
