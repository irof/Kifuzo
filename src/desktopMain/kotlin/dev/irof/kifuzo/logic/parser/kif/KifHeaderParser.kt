package dev.irof.kifuzo.logic.parser.kif

import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.csa.CsaHeaderParser
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants

object KifHeaderParser {
    fun isMetadata(l: String) = l.startsWith("先手：") || l.startsWith("対局者：") || l.startsWith("後手：") || l.startsWith("開始日時：") || l.startsWith("棋戦：") || l.startsWith("場所：") ||
        l.startsWith("先手:") || l.startsWith("対局者:") || l.startsWith("後手:") || l.startsWith("開始日時:") || l.startsWith("棋戦:") || l.startsWith("場所:")

    fun isBoardLine(l: String) = l.startsWith("|") && l.count { it == '|' } >= 2
    fun isMochigomaLine(l: String) = Regex("""^[上下先後]手((の)?(手)?)?持駒：""").containsMatchIn(l)
    fun isMoveLine(l: String) = Regex("""^\s*\d+\s+.*""").matches(l)

    fun handleMetadataLine(hp: HeaderParser, line: String) {
        val content = if (line.contains("：")) line.substringAfter("：").trim() else line.substringAfter(":").trim()
        when {
            line.startsWith("後手") -> hp.goteName = content
            line.startsWith("先手") || line.startsWith("対局者") -> hp.senteName = content
            line.startsWith("開始日時") -> hp.startTime = content
            line.startsWith("棋戦") -> {
                if (hp.event.isEmpty()) {
                    hp.event = content
                } else if (!hp.event.contains(content)) {
                    hp.event = "$content (${hp.event})"
                }
            }
            line.startsWith("場所") -> {
                if (hp.event.isEmpty()) {
                    hp.event = content
                } else if (!hp.event.contains(content)) {
                    hp.event += " ($content)"
                }
            }
            else -> CsaHeaderParser.handleMetadataLine(hp, line)
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
    // KIF形式の盤面図は、1マス2文字分の固定幅で構成される（計18文字）。
    // 例: "| ・ ・ ・ ・ ・ ・ ・ ・ 龍|"
    // 例: "| ・v歩 ・v龍v歩 ・v銀v歩 ・|"
    val cells = mutableListOf<String>()
    var i = 0
    // 1マス2文字ずつ取り出す。
    while (i + 1 < content.length && cells.size < ShogiConstants.BOARD_SIZE) {
        cells.add(content.substring(i, i + 2).trim())
        i += 2
    }
    return cells
}

private fun updateBoardCell(hp: HeaderParser, x: Int, pStr: String) {
    val piece = Piece.findPieceBySymbol(pStr.replace("v", "").replace("・", "").trim())
    hp.currentCells[hp.boardY][x] = piece?.let {
        BoardPiece(it, if (pStr.contains('v')) PieceColor.White else PieceColor.Black)
    }
}

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
