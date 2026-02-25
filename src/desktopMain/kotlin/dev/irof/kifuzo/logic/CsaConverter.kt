package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants
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
            kifLines.add(converted)
        }
    }
    return kifLines
}

private fun convertSingleCsaLine(line: String, index: Int, lines: List<String>, context: CsaConvertContext): String? = when {
    line.startsWith(CsaConstants.SENTE_NAME_PREFIX) -> "先手：" + line.substring(CsaConstants.SENTE_NAME_PREFIX.length)
    line.startsWith(CsaConstants.GOTE_NAME_PREFIX) -> "後手：" + line.substring(CsaConstants.GOTE_NAME_PREFIX.length)
    line.startsWith(CsaConstants.EVENT_PREFIX) -> "棋戦：" + line.substring(CsaConstants.EVENT_PREFIX.length)
    line.startsWith(CsaConstants.SITE_PREFIX) -> "場所：" + line.substring(CsaConstants.SITE_PREFIX.length)
    line.startsWith(CsaConstants.START_TIME_PREFIX) -> "開始日時：" + line.substring(CsaConstants.START_TIME_PREFIX.length)
    line.startsWith(CsaConstants.OPENING_PREFIX) -> "戦型：" + line.substring(CsaConstants.OPENING_PREFIX.length)
    line.startsWith(CsaConstants.TIME_LIMIT_PREFIX) -> "持ち時間：" + line.substring(CsaConstants.TIME_LIMIT_PREFIX.length)
    line == "PI" -> {
        context.setupInitialPosition()
        null
    }
    line.startsWith("P") && line.length >= 2 && line[1].isDigit() -> {
        context.setupRow(line)
        null
    }
    line.startsWith("P+") || line.startsWith("P-") -> {
        val color = if (line[1] == '+') "先手" else "後手"
        val pieces = mutableListOf<String>()
        var i = 2
        while (i + 4 <= line.length) {
            val pieceCsa = line.substring(i + 2, i + 4)
            if (pieceCsa != "AL") {
                val piece = Piece.entries.find { it.name == pieceCsa }
                if (piece != null) pieces.add(piece.symbol)
            }
            i += 4
        }
        if (pieces.isEmpty()) "${color}持駒：なし" else "${color}持駒：" + pieces.joinToString("　")
    }
    line.startsWith("%") -> processResultLine(line, context.moveCount)
    line.startsWith("+") || line.startsWith("-") -> {
        if (line.length >= CsaConstants.MOVE_MIN_LENGTH) {
            val seconds = if (index + 1 < lines.size && lines[index + 1].trim().startsWith("T")) {
                lines[index + 1].trim().substring(1).toIntOrNull() ?: 0
            } else {
                0
            }
            context.processMove(line, seconds)
        } else {
            null
        }
    }
    else -> null
}

private fun processResultLine(line: String, moveCount: Int): String? {
    val ending = when (line) {
        "%TORYO" -> "投了"
        "%CHUDAN" -> "中断"
        "%TIME_UP" -> "タイムアップ"
        "%SENNICHITE" -> "千日手"
        "%KACHI" -> "入玉勝ち"
        "%HIKIWAKE" -> "持将棋"
        else -> null
    }
    return ending?.let { "まで${moveCount - 1}手で$it" }
}

private class CsaConvertContext {
    var moveCount = 1
    private var lastToX = -1
    private var lastToY = -1
    private var totalSenteSeconds = 0
    private var totalGoteSeconds = 0
    private val currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()

    private val fullWidthDigits = "１２３４５６７８９"
    private val kanjiDigits = "一二三四五六七八九"

    companion object {
        private const val CSA_ROW_INDEX_POS = 1
        private const val CSA_ROW_START_OFFSET = 2
        private const val CSA_PIECE_WIDTH = 3
        private const val CSA_EMPTY_PIECE = " * "
    }

    fun setupInitialPosition() {
        BoardSnapshot.getInitialCells().forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                currentCells[y][x] = cell
            }
        }
    }

    fun setupRow(line: String) {
        val rowIdx = line[CSA_ROW_INDEX_POS] - '1'
        if (rowIdx !in 0..ShogiConstants.MAX_INDEX) return
        for (i in 0..ShogiConstants.MAX_INDEX) {
            val start = CSA_ROW_START_OFFSET + i * CSA_PIECE_WIDTH
            if (start + CSA_PIECE_WIDTH > line.length) break
            val pieceStr = line.substring(start, start + CSA_PIECE_WIDTH)
            currentCells[rowIdx][i] = when {
                pieceStr == CSA_EMPTY_PIECE -> null
                pieceStr.startsWith("+") -> findPiece(pieceStr.substring(1)) to PieceColor.Black
                pieceStr.startsWith("-") -> findPiece(pieceStr.substring(1)) to PieceColor.White
                else -> null
            }
        }
    }

    fun processMove(line: String, seconds: Int): String {
        val detail = MoveDetail.parse(line)
        val targetPiece = findPiece(detail.pieceCsa)

        val pieceKifName = if (detail.fromX == 0) {
            updateBoardForDrop(detail.toX, detail.toY, targetPiece, detail.isSente)
            targetPiece.symbol + "打"
        } else {
            val movingPiece = currentCells[detail.fromY - 1][ShogiConstants.BOARD_SIZE - detail.fromX]?.first ?: targetPiece.toBase()
            val isActuallyPromoted = (!movingPiece.isPromoted() && targetPiece.isPromoted()) || detail.isPromoteMarker
            val pieceOnBoard = if (detail.isPromoteMarker) targetPiece.promote() else targetPiece
            updateBoardForMove(detail.fromX, detail.fromY, detail.toX, detail.toY, pieceOnBoard, detail.isSente)
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

    private fun updateBoardForDrop(toX: Int, toY: Int, piece: Piece, isSente: Boolean) {
        currentCells[toY - 1][ShogiConstants.BOARD_SIZE - toX] = piece to (if (isSente) PieceColor.Black else PieceColor.White)
    }

    private fun updateBoardForMove(fromX: Int, fromY: Int, toX: Int, toY: Int, piece: Piece, isSente: Boolean) {
        currentCells[fromY - 1][ShogiConstants.BOARD_SIZE - fromX] = null
        currentCells[toY - 1][ShogiConstants.BOARD_SIZE - toX] = piece to (if (isSente) PieceColor.Black else PieceColor.White)
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
        return String.format(java.util.Locale.US, "   (%2d:%02d/%02d:%02d:%02d)", m, s, tm / ShogiConstants.MINUTES_IN_HOUR, tm % ShogiConstants.MINUTES_IN_HOUR, ts)
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
        fun parse(line: String): MoveDetail = MoveDetail(
            isSente = line.startsWith("+"),
            fromX = line[1] - '0',
            fromY = line[2] - '0',
            toX = line[3] - '0',
            toY = line[4] - '0',
            pieceCsa = line.substring(5, 7),
            isPromoteMarker = line.length > 7,
        )
    }
}
