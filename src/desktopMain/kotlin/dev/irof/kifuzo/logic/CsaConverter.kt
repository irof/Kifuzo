package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import java.nio.file.Path
import java.util.*
import kotlin.io.path.nameWithoutExtension
import kotlin.text.Charsets

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
            line.startsWith("N+") -> kifLines.add("先手：" + line.substring(2))
            line.startsWith("N-") -> kifLines.add("後手：" + line.substring(2))
            line.startsWith("\$START_TIME:") -> kifLines.add("開始日時：" + line.substring(12))
            line.startsWith("%") -> processResultLine(line, context.moveCount)?.let { kifLines.add(it) }
            line.startsWith("+") || line.startsWith("-") -> {
                if (line.length >= 7) {
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

    fun processMove(line: String, seconds: Int): String {
        val detail = MoveDetail.parse(line)
        val targetPiece = findPiece(detail.pieceCsa)

        val pieceKifName = if (detail.fromX == 0) {
            updateBoardForDrop(detail.toX, detail.toY, targetPiece, detail.isSente)
            targetPiece.symbol + "打"
        } else {
            val movingPiece = currentCells[detail.fromY - 1][9 - detail.fromX]?.first ?: targetPiece
            val isActuallyPromoted = (!movingPiece.isPromoted() && targetPiece.isPromoted()) || detail.isPromoteMarker
            updateBoardForMove(detail.fromX, detail.fromY, detail.toX, detail.toY, targetPiece, detail.isSente)
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
            fun parse(line: String): MoveDetail = MoveDetail(
                isSente = line.startsWith("+"),
                fromX = line[1] - '0',
                fromY = line[2] - '0',
                toX = line[3] - '0',
                toY = line[4] - '0',
                pieceCsa = line.substring(5, 7),
                isPromoteMarker = line.endsWith("+"),
            )
        }
    }

    private fun findPiece(csa: String): Piece = Piece.entries.find { it.name == csa } ?: Piece.FU

    private fun updateBoardForDrop(tx: Int, ty: Int, piece: Piece, isSente: Boolean) {
        currentCells[ty - 1][9 - tx] = piece to (if (isSente) PieceColor.Black else PieceColor.White)
    }

    private fun updateBoardForMove(fx: Int, fy: Int, tx: Int, ty: Int, piece: Piece, isSente: Boolean) {
        currentCells[fy - 1][9 - fx] = null
        currentCells[ty - 1][9 - tx] = piece to (if (isSente) PieceColor.Black else PieceColor.White)
    }

    private fun formatTime(seconds: Int, totalSeconds: Int): String {
        val moveTime = String.format(Locale.US, "%2d:%02d", seconds / 60, seconds % 60)
        val totalTime = String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
        return "$moveTime/$totalTime"
    }
}
