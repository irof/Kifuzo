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
    val kifLines = mutableListOf<String>()
    kifLines.add("# KIF version=2.0 encoding=UTF-8")

    // 盤面管理用
    val currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()

    val csaToPiece = mapOf(
        "FU" to Piece.FU, "KY" to Piece.KY, "KE" to Piece.KE, "GI" to Piece.GI, "KI" to Piece.KI,
        "KA" to Piece.KA, "HI" to Piece.HI, "OU" to Piece.OU,
        "TO" to Piece.TO, "NY" to Piece.NY, "NK" to Piece.NK, "NG" to Piece.NG, "UM" to Piece.UM, "RY" to Piece.RY,
    )

    val fullWidthDigits = "１２３４５６７８９"
    val kanjiDigits = "一二三四五六七八九"

    var moveCount = 1
    var lastToX = -1
    var lastToY = -1
    var totalSenteSeconds = 0
    var totalGoteSeconds = 0

    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.startsWith("N+")) {
            kifLines.add("先手：" + line.substring(2))
            continue
        }
        if (line.startsWith("N-")) {
            kifLines.add("後手：" + line.substring(2))
            continue
        }
        if (line.startsWith("\$START_TIME:")) {
            kifLines.add("開始日時：" + line.substring(12))
            continue
        }

        if (line.startsWith("%")) {
            val ending = when (line) {
                "%TORYO" -> "投了"
                "%CHUDAN" -> "中断"
                "%TIME_UP" -> "タイムアップ"
                "%SENNICHITE" -> "千日手"
                "%KACHI" -> "入玉勝ち"
                "%HIKIWAKE" -> "持将棋"
                else -> null
            }
            if (ending != null) kifLines.add("まで${moveCount - 1}手で$ending")
            continue
        }

        if ((line.startsWith("+") || line.startsWith("-")) && line.length >= 7) {
            val isSente = line.startsWith("+")
            val fx = line[1] - '0'
            val fy = line[2] - '0'
            val tx = line[3] - '0'
            val ty = line[4] - '0'
            val pieceCsa = line.substring(5, 7)
            val isPromoteMarker = line.endsWith("+")

            val targetPiece = csaToPiece[pieceCsa] ?: Piece.FU

            val pieceKifName: String
            if (fx == 0) {
                // 打つ場合
                pieceKifName = targetPiece.symbol + "打"
                // 盤面更新 (打つ場合も盤面に反映させないと後の移動で駒が特定できない)
                val tyIdx = ty - 1
                val txIdx = 9 - tx
                currentCells[tyIdx][txIdx] = targetPiece to (if (isSente) PieceColor.Black else PieceColor.White)
            } else {
                // 盤上の移動
                val fyIdx = fy - 1
                val fxIdx = 9 - fx
                val tyIdx = ty - 1
                val txIdx = 9 - tx

                val movingPiece = currentCells[fyIdx][fxIdx]?.first ?: targetPiece

                // 「成」を付ける条件:
                // 1. 移動前の駒が成り駒でない && 移動後の駒が成り駒
                // 2. あるいは明示的に末尾に + がある場合(一部の変換ツール用)
                val isActuallyPromoted = (!movingPiece.isPromoted() && targetPiece.isPromoted()) || isPromoteMarker

                pieceKifName = if (isActuallyPromoted) {
                    movingPiece.symbol + "成"
                } else {
                    movingPiece.symbol
                }

                // 盤面更新
                currentCells[fyIdx][fxIdx] = null
                currentCells[tyIdx][txIdx] = targetPiece to (if (isSente) PieceColor.Black else PieceColor.White)
            }

            val toPosStr = if (tx == lastToX && ty == lastToY) "同　" else "${fullWidthDigits[tx - 1]}${kanjiDigits[ty - 1]}"
            val fromStr = if (fx == 0) "" else "($fx$fy)"

            var seconds = 0
            if (i + 1 < lines.size && lines[i + 1].trim().startsWith("T")) {
                seconds = lines[i + 1].trim().substring(1).toIntOrNull() ?: 0
            }

            if (isSente) totalSenteSeconds += seconds else totalGoteSeconds += seconds
            val moveTimeStr = String.format(Locale.US, "%2d:%02d", seconds / 60, seconds % 60)
            val totalSec = if (isSente) totalSenteSeconds else totalGoteSeconds
            val totalTimeStr = String.format(Locale.US, "%02d:%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)

            kifLines.add(String.format(Locale.US, "%4d %s%s%-7s   ( %s/%s)", moveCount++, toPosStr, pieceKifName, fromStr, moveTimeStr, totalTimeStr))
            lastToX = tx
            lastToY = ty
        }
    }
    return kifLines
}
