package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.logic.parser.csa.CsaHeaderParser
import dev.irof.kifuzo.logic.parser.kif.KifHeaderParser
import dev.irof.kifuzo.logic.parser.kif.parseMove
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.ShogiConstants
import java.nio.file.Path

enum class KifuFormat { KIF, CSA }

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
    var senteName = "Sente"
    var goteName = "Gote"
    var startTime = ""
    var event = ""
    var currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
    var senteMochi = mutableListOf<Piece>()
    var goteMochi = mutableListOf<Piece>()
    var isStandardStart = true
    var moveStartIndex = -1
    var boardY = 0
    private var detectedFormat: KifuFormat? = null

    fun processLine(line: String, index: Int): Boolean {
        if (line.isBlank()) return false

        val format = when {
            isKifLine(line) && isCsaLine(line) -> detectedFormat ?: KifuFormat.KIF
            isKifLine(line) -> KifuFormat.KIF
            isCsaLine(line) -> KifuFormat.CSA
            else -> null
        }

        return format?.let { processLineAs(line, index, it) } ?: false
    }

    private fun isKifLine(line: String) = KifHeaderParser.isMetadata(line) || KifHeaderParser.isBoardLine(line) || KifHeaderParser.isMochigomaLine(line) || KifHeaderParser.isMoveLine(line)

    private fun isCsaLine(line: String) = CsaHeaderParser.isMetadata(line) || CsaHeaderParser.isBoardLine(line) || CsaHeaderParser.isMochigomaLine(line) || CsaHeaderParser.isMoveLine(line)

    private fun processLineAs(line: String, index: Int, format: KifuFormat): Boolean {
        if (detectedFormat != null && detectedFormat != format) {
            throw KifuParseException("棋譜形式が混在しています: ${detectedFormat}の中に${format}の行があります", lineNumber = index + 1, lineContent = line)
        }

        if (detectedFormat == null && format == KifuFormat.KIF) {
            if (senteName == "Sente") senteName = "先手"
            if (goteName == "Gote") goteName = "後手"
        }

        detectedFormat = format

        return when (format) {
            KifuFormat.KIF -> processKifLine(line, index)
            KifuFormat.CSA -> processCsaLine(line, index)
        }
    }

    private fun processKifLine(line: String, index: Int): Boolean = when {
        KifHeaderParser.isMetadata(line) -> {
            KifHeaderParser.handleMetadataLine(this, line)
            false
        }
        KifHeaderParser.isBoardLine(line) -> {
            KifHeaderParser.handleBoardLine(this, line)
            false
        }
        KifHeaderParser.isMochigomaLine(line) -> {
            KifHeaderParser.handleMochigomaLine(this, line)
            false
        }
        KifHeaderParser.isMoveLine(line) -> {
            if (parseMove(line, null) != null) {
                moveStartIndex = index
                true
            } else {
                false
            }
        }
        else -> false
    }

    private fun processCsaLine(line: String, index: Int): Boolean = when {
        CsaHeaderParser.isMetadata(line) -> {
            CsaHeaderParser.handleMetadataLine(this, line)
            false
        }
        CsaHeaderParser.isBoardLine(line) -> {
            CsaHeaderParser.handleBoardLine(this, line)
            false
        }
        CsaHeaderParser.isMochigomaLine(line) -> {
            CsaHeaderParser.handleMochigomaLine(this, line)
            false
        }
        CsaHeaderParser.isMoveLine(line) -> {
            moveStartIndex = index
            true
        }
        else -> false
    }

    fun resetToStandard() {
        isStandardStart = true
        currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
        senteMochi.clear()
        goteMochi.clear()
    }

    fun prepareNonStandardBoard() {
        if (isStandardStart) {
            isStandardStart = false
            currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<BoardPiece>(ShogiConstants.BOARD_SIZE).toMutableList() }.toMutableList()
        }
    }

    fun build(): KifuHeader {
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
        try {
            if (parser.processLine(line, i)) break
        } catch (e: KifuParseException) {
            throw e
        } catch (cause: Exception) {
            throw KifuParseException(cause.message ?: "パースエラー", lineNumber = i + 1, lineContent = line, cause = cause)
        }
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
