package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.logic.parser.csa.CsaHeaderParser
import dev.irof.kifuzo.logic.parser.kif.KifHeaderParser
import dev.irof.kifuzo.logic.parser.kif.parseMove
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.ShogiConstants
import java.nio.file.Path

data class KifuHeader(
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

/**
 * 棋譜のヘッダー部分を解析するクラス。
 */
class HeaderParser {
    var senteName = "先手"
    var goteName = "後手"
    var startTime = ""
    var event = ""
    var currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
    var senteMochi = mutableListOf<Piece>()
    var goteMochi = mutableListOf<Piece>()
    var isStandardStart = true
    var moveStartIndex = -1
    var boardY = 0

    fun processLine(line: String, index: Int): Boolean = when {
        KifHeaderParser.isMetadata(line) -> {
            KifHeaderParser.handleMetadataLine(this, line)
            false
        }
        CsaHeaderParser.isMetadata(line) -> {
            CsaHeaderParser.handleMetadataLine(this, line)
            false
        }
        line == "PI" -> {
            resetToStandard()
            false
        }
        KifHeaderParser.isBoardLine(line) -> {
            KifHeaderParser.handleBoardLine(this, line)
            false
        }
        CsaHeaderParser.isBoardLine(line) -> {
            CsaHeaderParser.handleBoardLine(this, line)
            false
        }
        KifHeaderParser.isMochigomaLine(line) -> {
            KifHeaderParser.handleMochigomaLine(this, line)
            false
        }
        CsaHeaderParser.isMochigomaLine(line) -> {
            CsaHeaderParser.handleMochigomaLine(this, line)
            false
        }
        KifHeaderParser.isMoveLine(line) || CsaHeaderParser.isMoveLine(line) -> {
            if (parseMove(line, null) != null || line.startsWith("+") || line.startsWith("-")) {
                moveStartIndex = index
                true
            } else {
                false
            }
        }
        else -> false
    }

    private fun resetToStandard() {
        isStandardStart = true
        currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
        senteMochi.clear()
        goteMochi.clear()
    }

    fun prepareNonStandardBoard() {
        // cellsが平手の初期配置のままであれば、空の盤面に置き換える
        if (isStandardStart) {
            isStandardStart = false
            currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<BoardPiece>(ShogiConstants.BOARD_SIZE).toMutableList() }.toMutableList()
        }
    }

    fun build(): KifuHeader {
        // boardY > 0 は KIFの盤面図(|)が1行以上現れたことを示す
        if (boardY in 1 until ShogiConstants.BOARD_SIZE) {
            throw KifuParseException("盤面図が不完全です（${boardY}行しか見つかりませんでした）。")
        }
        return KifuHeader(
            senteName = senteName, goteName = goteName, startTime = startTime, event = event,
            initialCells = currentCells.map { it.toList() }, senteMochi = senteMochi.toList(), goteMochi = goteMochi.toList(),
            isStandardStart = isStandardStart, moveStartIndex = moveStartIndex,
        )
    }
}

fun parseHeader(lines: List<String>): KifuHeader {
    val parser = HeaderParser()
    for ((i, line) in lines.withIndex()) {
        if (parser.processLine(line, i)) break
    }
    return parser.build()
}

class KifuParseException(
    message: String,
    val lineNumber: Int? = null,
    val lineContent: String? = null,
    val path: Path? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
