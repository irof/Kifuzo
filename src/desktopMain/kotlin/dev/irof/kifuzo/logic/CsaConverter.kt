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
        val isSente = line.startsWith("+")
        val fx = line[1] - '0'
        val fy = line[2] - '0'
        val tx = line[3] - '0'
        val ty = line[4] - '0'
        val pieceCsa = line.substring(5, 7)
        val isPromoteMarker = line.endsWith("+")

        val targetPiece = findPiece(pieceCsa)
        val pieceKifName = if (fx == 0) {
            updateBoardForDrop(tx, ty, targetPiece, isSente)
            targetPiece.symbol + "打"
        } else {
            val movingPiece = currentCells[fy - 1][9 - fx]?.first ?: targetPiece
            val isActuallyPromoted = (!movingPiece.isPromoted() && targetPiece.isPromoted()) || isPromoteMarker
            updateBoardForMove(fx, fy, tx, ty, targetPiece, isSente)
            if (isActuallyPromoted) movingPiece.symbol + "成" else movingPiece.symbol
        }

        val toPosStr = if (tx == lastToX && ty == lastToY) "同　" else "${fullWidthDigits[tx - 1]}${kanjiDigits[ty - 1]}"
        val fromStr = if (fx == 0) "" else "($fx$fy)"

        if (isSente) totalSenteSeconds += seconds else totalGoteSeconds += seconds
        val timeStr = formatTime(seconds, if (isSente) totalSenteSeconds else totalGoteSeconds)

        lastToX = tx
        lastToY = ty
        return String.format(Locale.US, "%4d %s%s%-7s   ( %s)", moveCount++, toPosStr, pieceKifName, fromStr, timeStr)
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
