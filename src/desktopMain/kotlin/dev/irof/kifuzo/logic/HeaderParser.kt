package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants

internal data class KifuHeader(
    val senteName: String,
    val goteName: String,
    val startTime: String,
    val event: String,
    val initialCells: List<List<BoardPiece?>>,
    val senteMochi: List<Piece>,
    val goteMochi: List<Piece>,
    val isStandardStart: Boolean,
    val moveStartIndex: Int,
)

internal class HeaderParser {
    private var senteName = "先手"
    private var goteName = "後手"
    private var startTime = ""
    private var event = ""
    private val currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<BoardPiece>(ShogiConstants.BOARD_SIZE) }
    private val senteMochi = mutableListOf<Piece>()
    private val goteMochi = mutableListOf<Piece>()
    private var isStandardStart = true
    private var moveStartIndex = -1
    private var boardY = 0

    fun processLine(line: String, index: Int): Boolean = when {
        line.startsWith("先手：") || line.startsWith("対局者：") || line.startsWith("後手：") ||
            line.startsWith("開始日時：") || line.startsWith("棋戦：") -> {
            handleMetadataLine(line)
            false
        }
        line.startsWith("|") && line.count { it == '|' } >= 2 -> {
            isStandardStart = false
            parseBoardLine(line)
            false
        }
        Regex("""^[上下先後]手(の)?持駒：""").containsMatchIn(line) -> {
            isStandardStart = false
            handleMochigomaLine(line)
            false
        }
        Regex("""^\s*\d+\s+.*""").matches(line) -> {
            if (parseMove(line, null) != null) {
                moveStartIndex = index
                true
            } else {
                false
            }
        }
        else -> false
    }

    private fun handleMetadataLine(line: String) {
        when {
            line.startsWith("後手：") -> goteName = line.substringAfter("：").trim()
            line.startsWith("先手：") || line.startsWith("対局者：") -> senteName = line.substringAfter("：").trim()
            line.startsWith("開始日時：") -> startTime = line.substringAfter("：").trim()
            line.startsWith("棋戦：") -> event = line.substringAfter("：").trim()
        }
    }

    private fun handleMochigomaLine(line: String) {
        val pieces = Piece.parseMochigoma(line.substringAfter("："))
        if (line.startsWith("先手") || line.startsWith("下手")) {
            senteMochi.addAll(pieces)
        } else {
            goteMochi.addAll(pieces)
        }
    }

    private fun parseBoardLine(line: String) {
        if (boardY >= ShogiConstants.BOARD_SIZE) return
        val content = line.substringAfter("|").substringBeforeLast("|")
        val cells = splitBoardCells(content)

        for (x in 0 until ShogiConstants.BOARD_SIZE) {
            if (x >= cells.size) break
            val pStr = cells[x]
            val piece = Piece.findPieceBySymbol(pStr.replace("v", "").replace("・", "").trim())
            if (piece != null) {
                val color = if (pStr.contains('v')) PieceColor.White else PieceColor.Black
                currentCells[boardY][x] = BoardPiece(piece, color)
            }
        }
        boardY++
    }

    private fun splitBoardCells(content: String): List<String> = if (content.contains("|")) {
        content.split("|")
    } else {
        (0 until ShogiConstants.BOARD_SIZE).map { idx ->
            val start = idx * 2
            if (start + 2 <= content.length) content.substring(start, start + 2) else ""
        }
    }

    fun build(): KifuHeader {
        val finalCells = if (isStandardStart) {
            BoardSnapshot.getInitialCells()
        } else {
            if (boardY < ShogiConstants.BOARD_SIZE) throw KifuParseException("盤面図が不完全です（${boardY}行しか見つかりませんでした）。")
            val pieceCount = currentCells.sumOf { row -> row.count { it != null } }
            if (pieceCount == 0) throw KifuParseException("盤面図から駒を読み取れませんでした。")
            currentCells.map { it.toList() }
        }

        return KifuHeader(
            senteName = senteName,
            goteName = goteName,
            startTime = startTime,
            event = event,
            initialCells = finalCells,
            senteMochi = senteMochi,
            goteMochi = goteMochi,
            isStandardStart = isStandardStart,
            moveStartIndex = moveStartIndex,
        )
    }
}
