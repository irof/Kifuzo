package dev.irof.kifuzo.logic.parser
import dev.irof.kifuzo.logic.io.*
import dev.irof.kifuzo.logic.service.*
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

/**
 * 棋譜のヘッダー部分を解析するクラス。
 */
internal class HeaderParser {
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
        isMetadata(line) -> {
            handleMetadataLine(this, line)
            false
        }
        line == "PI" -> {
            resetToStandard()
            false
        }
        isBoardLine(line) -> {
            handleBoardLine(this, line)
            false
        }
        isMochigomaLine(line) -> {
            handleMochigomaLine(this, line)
            false
        }
        isMoveLine(line) -> {
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

    private fun isMetadata(l: String) = l.startsWith("先手：") || l.startsWith("対局者：") || l.startsWith("後手：") || l.startsWith("開始日時：") || l.startsWith("棋戦：") || l.startsWith("場所：") || l.startsWith("N+") || l.startsWith("N-") || l.startsWith("\$START_TIME:") || l.startsWith("\$EVENT:")
    private fun isBoardLine(l: String) = (l.startsWith("|") && l.count { it == '|' } >= 2) || (l.startsWith("P") && l.length >= 2 && l[1].isDigit())
    private fun isMochigomaLine(l: String) = l.startsWith("P+") || l.startsWith("P-") || Regex("""^[上下先後]手(の)?持駒：""").containsMatchIn(l)
    private fun isMoveLine(l: String) = Regex("""^\s*\d+\s+.*""").matches(l) || (l.length >= 2 && (l.startsWith("+") || l.startsWith("-")) && l[1].isDigit())

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
