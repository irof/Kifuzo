package dev.irof.kifuzo.logic.parser
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.BoardPiece
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.text.Charsets

private object CsaConstants {
    const val SENTE_NAME_PREFIX = "N+"
    const val GOTE_NAME_PREFIX = "N-"
    const val EVENT_PREFIX = "\$EVENT:"
    const val SITE_PREFIX = "\$SITE:"
    const val START_TIME_PREFIX = "\$START_TIME:"
    const val OPENING_PREFIX = "\$OPENING:"
    const val TIME_LIMIT_PREFIX = "\$TIME_LIMIT:"
    const val MOVE_MIN_LENGTH = 7
    const val COLOR_SYMBOL_POS = 1
    const val INITIAL_PIECES_START_INDEX = 2
    const val PIECE_ENTRY_LENGTH = 4
    const val PIECE_NAME_OFFSET = 2
    const val PIECE_NAME_LENGTH = 2
}

/**
 * CSA形式の棋譜ファイルを KIF形式に変換します。
 */
fun convertCsaToKifu(path: Path) {
    val lines = readLinesWithEncoding(path)
    val kifLines = convertCsaToKifuLines(lines)
    val kifuFile = (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    java.nio.file.Files.write(kifuFile, kifLines.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8))
}

/**
 * CSA形式の行リストを KIF形式の行リストに変換します。
 */
fun convertCsaToKifuLines(lines: List<String>): List<String> {
    val context = CsaConvertContext()
    val kifLines = mutableListOf("# KIF version=2.0 encoding=UTF-8")

    for (i in lines.indices) {
        val line = lines[i].trim()
        val converted = convertSingleCsaLine(line, i, lines, context)
        if (converted != null) {
            kifLines.addAll(converted.split("\n"))
        }
    }
    return kifLines
}

private fun convertSingleCsaLine(line: String, index: Int, lines: List<String>, context: CsaConvertContext): String? = when {
    isMetadataLine(line) -> processMetadataLine(line)
    line == "PI" -> {
        context.setupInitialPosition()
        null
    }
    line.startsWith("P") && line.length >= 2 && line[1].isDigit() -> {
        context.setupRow(line)
        null
    }
    line.startsWith("P+") || line.startsWith("P-") -> processInitialPieces(line)
    line.startsWith("%") -> processResultLine(line, context.moveCount)
    line.startsWith("+") || line.startsWith("-") -> processMoveLine(line, index, lines, context)
    else -> null
}

private fun isMetadataLine(line: String): Boolean = line.startsWith(CsaConstants.SENTE_NAME_PREFIX) ||
    line.startsWith(CsaConstants.GOTE_NAME_PREFIX) ||
    line.startsWith(CsaConstants.EVENT_PREFIX) ||
    line.startsWith(CsaConstants.SITE_PREFIX) ||
    line.startsWith(CsaConstants.START_TIME_PREFIX) ||
    line.startsWith(CsaConstants.OPENING_PREFIX) ||
    line.startsWith(CsaConstants.TIME_LIMIT_PREFIX)

private fun processMetadataLine(line: String): String? = when {
    line.startsWith(CsaConstants.SENTE_NAME_PREFIX) -> "先手：" + line.substring(CsaConstants.SENTE_NAME_PREFIX.length)
    line.startsWith(CsaConstants.GOTE_NAME_PREFIX) -> "後手：" + line.substring(CsaConstants.GOTE_NAME_PREFIX.length)
    line.startsWith(CsaConstants.EVENT_PREFIX) -> "棋戦：" + line.substring(CsaConstants.EVENT_PREFIX.length)
    line.startsWith(CsaConstants.SITE_PREFIX) -> "場所：" + line.substring(CsaConstants.SITE_PREFIX.length)
    line.startsWith(CsaConstants.START_TIME_PREFIX) -> "開始日時：" + line.substring(CsaConstants.START_TIME_PREFIX.length)
    line.startsWith(CsaConstants.OPENING_PREFIX) -> "戦型：" + line.substring(CsaConstants.OPENING_PREFIX.length)
    line.startsWith(CsaConstants.TIME_LIMIT_PREFIX) -> "持ち時間：" + line.substring(CsaConstants.TIME_LIMIT_PREFIX.length)
    else -> null
}

private fun processInitialPieces(line: String): String {
    val color = if (line[CsaConstants.COLOR_SYMBOL_POS] == '+') "先手" else "後手"
    val pieces = mutableListOf<String>()
    var i = CsaConstants.INITIAL_PIECES_START_INDEX
    while (i + CsaConstants.PIECE_ENTRY_LENGTH <= line.length) {
        val pieceCsa = line.substring(i + CsaConstants.PIECE_NAME_OFFSET, i + CsaConstants.PIECE_NAME_OFFSET + CsaConstants.PIECE_NAME_LENGTH)
        if (pieceCsa != "AL") {
            val piece = Piece.entries.find { it.name == pieceCsa }
            if (piece != null) pieces.add(piece.symbol)
        }
        i += CsaConstants.PIECE_ENTRY_LENGTH
    }
    return if (pieces.isEmpty()) "${color}持駒：なし" else "${color}持駒：" + pieces.joinToString("　")
}

private fun processMoveLine(line: String, index: Int, lines: List<String>, context: CsaConvertContext): String? {
    if (line.length < CsaConstants.MOVE_MIN_LENGTH) return null
    val seconds = if (index + 1 < lines.size && lines[index + 1].trim().startsWith("T")) {
        lines[index + 1].trim().substring(1).toIntOrNull() ?: 0
    } else {
        0
    }
    return context.processMove(line, seconds)
}

private fun processResultLine(line: String, moveCount: Int): String? {
    val (kifMove, summary) = when (line) {
        "%TORYO" -> "投了" to "投了"
        "%CHUDAN" -> "中断" to "中断"
        "%TIME_UP" -> "切れ負け" to "タイムアップ"
        "%SENNICHITE" -> "千日手" to "千日手"
        "%KACHI" -> "入玉勝ち" to "入玉勝ち"
        "%HIKIWAKE" -> "持将棋" to "持将棋"
        else -> return null
    }
    val numberedMove = String.format(java.util.Locale.US, "%4d %s", moveCount, kifMove)
    val summaryLine = "まで${moveCount - 1}手で$summary"
    return "$numberedMove\n$summaryLine"
}

private class CsaConvertContext {
    var moveCount = 1
    private var lastToX = -1
    private var lastToY = -1
    private var totalSenteSeconds = 0
    private var totalGoteSeconds = 0
    private var currentSnapshot = BoardSnapshot(BoardSnapshot.getInitialCells())

    private val fullWidthDigits = "１２３４５６７８９"
    private val kanjiDigits = "一二三四五六七八九"

    companion object {
        private const val CSA_ROW_INDEX_POS = 1
        private const val CSA_ROW_START_OFFSET = 2
        private const val CSA_PIECE_WIDTH = 3
        private const val CSA_EMPTY_PIECE = " * "
    }

    fun setupInitialPosition() {
        currentSnapshot = BoardSnapshot(BoardSnapshot.getInitialCells())
    }

    fun setupRow(line: String) {
        val rowIdx = line[CSA_ROW_INDEX_POS] - '1'
        if (rowIdx !in 0..ShogiConstants.MAX_INDEX) return

        val newCells = currentSnapshot.cells.map { it.toMutableList() }.toMutableList()
        val row = newCells[rowIdx]

        for (i in 0..ShogiConstants.MAX_INDEX) {
            val start = CSA_ROW_START_OFFSET + i * CSA_PIECE_WIDTH
            if (start + CSA_PIECE_WIDTH > line.length) break
            val pieceStr = line.substring(start, start + CSA_PIECE_WIDTH)
            row[i] = when {
                pieceStr == CSA_EMPTY_PIECE -> null
                pieceStr.startsWith("+") -> BoardPiece(findPiece(pieceStr.substring(1)), PieceColor.Black)
                pieceStr.startsWith("-") -> BoardPiece(findPiece(pieceStr.substring(1)), PieceColor.White)
                else -> null
            }
        }
        currentSnapshot = currentSnapshot.copy(cells = newCells.map { it.toList() })
    }

    fun processMove(line: String, seconds: Int): String {
        val detail = MoveDetail.parse(line)
        val targetPiece = findPiece(detail.pieceCsa)
        val toSquare = Square(detail.toX, detail.toY)
        val turnColor = if (detail.isSente) PieceColor.Black else PieceColor.White

        val pieceKifName = if (detail.fromX == 0) {
            currentSnapshot = currentSnapshot.applyDrop(targetPiece, toSquare, turnColor)
            targetPiece.symbol + "打"
        } else {
            val fromSquare = Square(detail.fromX, detail.fromY)
            val movingPiece = currentSnapshot.at(fromSquare)?.piece ?: targetPiece.toBase()
            val isActuallyPromoted = (!movingPiece.isPromoted() && targetPiece.isPromoted()) || detail.isPromoteMarker
            currentSnapshot = currentSnapshot.applyMove(fromSquare, toSquare, isActuallyPromoted, turnColor, fallbackPiece = targetPiece)
            if (isActuallyPromoted) movingPiece.symbol + "成" else movingPiece.symbol
        }

        val toPosStr = formatToPos(detail.toX, detail.toY)
        val fromStr = if (detail.fromX == 0) "" else "(${detail.fromX}${detail.fromY})"

        updateConsumptionTime(detail.isSente, seconds)
        val timeStr = formatTime(seconds, if (detail.isSente) totalSenteSeconds else totalGoteSeconds)

        val moveText = String.format(java.util.Locale.US, "%4d %s%s%s%s", moveCount++, toPosStr, pieceKifName, fromStr, timeStr)
        lastToX = detail.toX
        lastToY = detail.toY
        return moveText
    }

    private fun updateConsumptionTime(isSente: Boolean, seconds: Int) {
        if (isSente) totalSenteSeconds += seconds else totalGoteSeconds += seconds
    }

    private fun formatToPos(toX: Int, toY: Int): String = if (toX == lastToX && toY == lastToY) {
        "同　"
    } else {
        fullWidthDigits[toX - 1].toString() + kanjiDigits[toY - 1]
    }

    private fun formatTime(seconds: Int, totalSeconds: Int): String {
        val m = seconds / ShogiConstants.SECONDS_IN_MINUTE
        val s = seconds % ShogiConstants.SECONDS_IN_MINUTE
        val tm = totalSeconds / ShogiConstants.SECONDS_IN_MINUTE
        val ts = totalSeconds % ShogiConstants.SECONDS_IN_MINUTE
        val totalHours = tm / ShogiConstants.MINUTES_IN_HOUR
        val totalMinutes = tm % ShogiConstants.MINUTES_IN_HOUR
        return String.format(java.util.Locale.US, "   (%2d:%02d/%02d:%02d:%02d)", m, s, totalHours, totalMinutes, ts)
    }

    private fun findPiece(csaName: String): Piece = Piece.entries.find { it.name == csaName } ?: Piece.FU
}

private data class MoveDetail(
    val isSente: Boolean,
    val fromX: Int,
    val fromY: Int,
    val toX: Int,
    val toY: Int,
    val pieceCsa: String,
    val isPromoteMarker: Boolean,
) {
    companion object {
        private const val FROM_X_POS = 1
        private const val FROM_Y_POS = 2
        private const val TO_X_POS = 3
        private const val TO_Y_POS = 4
        private const val PIECE_START_POS = 5
        private const val PIECE_END_POS = 7
        private const val PROMOTE_MARKER_MIN_LENGTH = 8

        fun parse(line: String): MoveDetail = MoveDetail(
            isSente = line.startsWith("+"),
            fromX = line[FROM_X_POS] - '0',
            fromY = line[FROM_Y_POS] - '0',
            toX = line[TO_X_POS] - '0',
            toY = line[TO_Y_POS] - '0',
            pieceCsa = line.substring(PIECE_START_POS, PIECE_END_POS),
            isPromoteMarker = line.length >= PROMOTE_MARKER_MIN_LENGTH,
        )
    }
}
