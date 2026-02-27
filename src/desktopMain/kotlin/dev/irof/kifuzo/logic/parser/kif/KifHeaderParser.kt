package dev.irof.kifuzo.logic.parser.kif

import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuHeader
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.csa.handleCsaMetadataLine
import dev.irof.kifuzo.logic.parser.csa.handleCsaMochigomaLine
import dev.irof.kifuzo.logic.parser.csa.parseCsaBoardLine
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants

object KifHeaderParser {
    fun isMetadata(l: String) = l.startsWith("先手：") || l.startsWith("対局者：") || l.startsWith("後手：") || l.startsWith("開始日時：") || l.startsWith("棋戦：") || l.startsWith("場所：")
    fun isBoardLine(l: String) = l.startsWith("|") && l.count { it == '|' } >= 2
    fun isMochigomaLine(l: String) = Regex("""^[上下先後]手(の)?持駒：""").containsMatchIn(l)
    fun isMoveLine(l: String) = Regex("""^\s*\d+\s+.*""").matches(l)

    fun handleMetadataLine(hp: HeaderParser, line: String) {
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

    fun handleMochigomaLine(hp: HeaderParser, line: String) {
        hp.prepareNonStandardBoard()
        val pieces = Piece.parseMochigoma(line.substringAfter("："))
        if (line.startsWith("先手") || line.startsWith("下手")) hp.senteMochi.addAll(pieces) else hp.goteMochi.addAll(pieces)
    }

    fun handleBoardLine(hp: HeaderParser, line: String) {
        hp.prepareNonStandardBoard()
        parseKifBoardLine(hp, line)
    }
}

internal fun parseKifBoardLine(hp: HeaderParser, line: String) {
    if (hp.boardY >= ShogiConstants.BOARD_SIZE) return
    val content = line.substringAfter("|").substringBeforeLast("|")
    val cells = extractKifBoardCells(content)

    for (x in 0 until ShogiConstants.BOARD_SIZE) {
        if (x < cells.size) {
            updateBoardCell(hp, x, cells[x])
        }
    }
    hp.boardY++
}

private fun extractKifBoardCells(content: String): List<String> {
    val s = content.replace(" ", "").replace("　", "")
    val cells = mutableListOf<String>()
    var i = 0
    while (i < s.length && cells.size < ShogiConstants.BOARD_SIZE) {
        if (s[i] == 'v') {
            if (i + 1 < s.length) {
                cells.add(s.substring(i, i + 2))
                i += 2
            } else {
                i++
            }
        } else {
            cells.add(s.substring(i, i + 1))
            i++
        }
    }
    return cells
}

private fun updateBoardCell(hp: HeaderParser, x: Int, pStr: String) {
    val piece = Piece.findPieceBySymbol(pStr.replace("v", "").replace("・", "").trim())
    hp.currentCells[hp.boardY][x] = piece?.let {
        BoardPiece(it, if (pStr.contains('v')) PieceColor.White else PieceColor.Black)
    }
}

/**
 * 一文字の駒名（例: "歩", "王", "竜"）を解析して Piece を返します。
 */
fun Piece.Companion.findPieceBySymbol(symbol: String): Piece? {
    val s = symbol.trim()
    if (s.isEmpty()) return null
    val found = Piece.entries.find {
        it.symbol == s ||
            (s == "王" && it == Piece.OU) ||
            (s == "玉" && it == Piece.OU) ||
            (s == "竜" && it == Piece.RY) ||
            (s == "龍" && it == Piece.RY) ||
            (s == "馬" && it == Piece.UM)
    }
    return found
}

/**
 * 持ち駒文字列（例: "飛二 角 銀三"）を解析して Piece のリストを返します。
 */
fun Piece.Companion.parseMochigoma(text: String): List<Piece> {
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
