package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiConstants
import java.nio.file.Path
import java.util.*
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

fun convertCsaToKifu(path: Path) {
    val lines = readLinesWithEncoding(path)
    val kifLines = convertCsaToKifuLines(lines)
    val kifuFile = (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    java.nio.file.Files.write(kifuFile, kifLines.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8))
}

fun convertCsaToKifuLines(lines: List<String>): List<String> {
    val context = CsaConvertContext()
    val kifLines = mutableListOf("# KIF version=2.0 encoding=UTF-8")

    for (i in lines.indices) {
        val line = lines[i].trim()
        when {
            line.startsWith(CsaConstants.SENTE_NAME_PREFIX) -> kifLines.add("先手：" + line.substring(CsaConstants.SENTE_NAME_PREFIX.length))
            line.startsWith(CsaConstants.GOTE_NAME_PREFIX) -> kifLines.add("後手：" + line.substring(CsaConstants.GOTE_NAME_PREFIX.length))
            line.startsWith(CsaConstants.EVENT_PREFIX) -> kifLines.add("棋戦：" + line.substring(CsaConstants.EVENT_PREFIX.length))
            line.startsWith(CsaConstants.SITE_PREFIX) -> kifLines.add("場所：" + line.substring(CsaConstants.SITE_PREFIX.length))
            line.startsWith(CsaConstants.START_TIME_PREFIX) -> kifLines.add("開始日時：" + line.substring(CsaConstants.START_TIME_PREFIX.length))
            line.startsWith(CsaConstants.OPENING_PREFIX) -> kifLines.add("戦型：" + line.substring(CsaConstants.OPENING_PREFIX.length))
            line.startsWith(CsaConstants.TIME_LIMIT_PREFIX) -> kifLines.add("持ち時間：" + line.substring(CsaConstants.TIME_LIMIT_PREFIX.length))
            line == "PI" -> context.setupInitialPosition()
            line.startsWith("P") && line.length >= 2 && line[1].isDigit() -> context.setupRow(line)
            line.startsWith("%") -> processResultLine(line, context.moveCount)?.let { kifLines.add(it) }
            line.startsWith("+") || line.startsWith("-") -> {
                if (line.length >= CsaConstants.MOVE_MIN_LENGTH) {
                    val seconds = if (i + 1 < lines.size && lines[i + 1].trim().startsWith("T")) {
                        lines[i + 1].trim().substring(1).toIntOrNull() ?: 0
                    } else {
                        0
                    }
                    kifLines.add(context.processMove(line, seconds))
                }
            }
        }
    }
    return kifLines
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

    fun setupInitialPosition() {
        BoardSnapshot.getInitialCells().forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                currentCells[y][x] = cell
            }
        }
    }

    fun setupRow(line: String) {
        val rowIdx = line[1] - '1'
        if (rowIdx !in 0..8) return
        for (i in 0..8) {
            val start = 2 + i * 3
            if (start + 3 > line.length) break
            val pieceStr = line.substring(start, start + 3)
            currentCells[rowIdx][i] = when {
                pieceStr == " * " -> null
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

        lastToX = detail.toX
        lastToY = detail.toY
        return String.format(Locale.US, "%4d %s%s%-7s   ( %s)", moveCount++, toPosStr, pieceKifName, fromStr, timeStr)
    }

    private fun formatToPos(tx: Int, ty: Int): String = if (tx == lastToX && ty == lastToY) {
        "同　"
    } else {
        "${fullWidthDigits[tx - 1]}${kanjiDigits[ty - 1]}"
    }

    private fun updateConsumptionTime(isSente: Boolean, seconds: Int) {
        if (isSente) totalSenteSeconds += seconds else totalGoteSeconds += seconds
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
            private const val PIECE_START = 5
            private const val PIECE_END = 7

            fun parse(line: String): MoveDetail = MoveDetail(
                isSente = line.startsWith("+"),
                fromX = line[1] - '0',
                fromY = line[2] - '0',
                toX = line[3] - '0',
                toY = line[4] - '0',
                pieceCsa = line.substring(PIECE_START, PIECE_END),
                isPromoteMarker = line.endsWith("+"),
            )
        }
    }

    private fun findPiece(csa: String): Piece = Piece.entries.find { it.name == csa } ?: Piece.FU

    private fun updateBoardForDrop(tx: Int, ty: Int, piece: Piece, isSente: Boolean) {
        currentCells[ty - 1][ShogiConstants.BOARD_SIZE - tx] = piece to (if (isSente) PieceColor.Black else PieceColor.White)
    }

    private fun updateBoardForMove(fx: Int, fy: Int, tx: Int, ty: Int, piece: Piece, isSente: Boolean) {
        currentCells[fy - 1][ShogiConstants.BOARD_SIZE - fx] = null
        currentCells[ty - 1][ShogiConstants.BOARD_SIZE - tx] = piece to (if (isSente) PieceColor.Black else PieceColor.White)
    }

    private fun formatTime(seconds: Int, totalSeconds: Int): String {
        val moveTime = String.format(Locale.US, "%2d:%02d", seconds / ShogiConstants.SECONDS_IN_MINUTE, seconds % ShogiConstants.SECONDS_IN_MINUTE)
        val totalTime = String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / ShogiConstants.SECONDS_IN_HOUR, (totalSeconds % ShogiConstants.SECONDS_IN_HOUR) / ShogiConstants.SECONDS_IN_MINUTE, totalSeconds % ShogiConstants.SECONDS_IN_MINUTE)
        return "$moveTime/$totalTime"
    }
}
