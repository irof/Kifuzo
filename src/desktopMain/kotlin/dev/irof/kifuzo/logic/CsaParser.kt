package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square
import java.nio.file.Path

/**
 * CSA形式の棋譜を解析して ShogiBoardState を更新するクラス。
 */
fun parseCsa(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseCsa(lines, state)
}

fun parseCsa(lines: List<String>, state: ShogiBoardState) {
    var senteName = "先手"
    var goteName = "後手"
    val history = mutableListOf<BoardSnapshot>()
    var firstContactStep = -1

    // 初期盤面（平手）
    val currentCells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
    val senteMochi = mutableListOf<Piece>()
    val goteMochi = mutableListOf<Piece>()

    history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), "開始局面", evaluation = 0))

    var moveCount = 1
    for (i in lines.indices) {
        val line = lines[i].trim()
        when {
            line.startsWith("N+") -> senteName = line.substring(2)
            line.startsWith("N-") -> goteName = line.substring(2)
            line.startsWith("+") || line.startsWith("-") -> {
                if (line.length < 7) continue
                val isSente = line.startsWith("+")
                val fromX = line[1] - '0'
                val fromY = line[2] - '0'
                val toX = line[3] - '0'
                val toY = line[4] - '0'
                val pieceCsa = line.substring(5, 7)
                val isPromoteMarker = line.endsWith("+")

                val targetPiece = Piece.entries.find { it.name == pieceCsa } ?: Piece.FU
                val turnColor = if (isSente) PieceColor.Black else PieceColor.White

                val lastSnapshot = history.last()

                val moveText = if (fromX == 0) {
                    currentCells[toY - 1][ShogiConstants.BOARD_SIZE - toX] = targetPiece to turnColor
                    if (isSente) senteMochi.remove(targetPiece) else goteMochi.remove(targetPiece)
                    "${targetPiece.symbol}打"
                } else {
                    val movingPiece = currentCells[fromY - 1][ShogiConstants.BOARD_SIZE - fromX]?.first ?: targetPiece
                    val isActuallyPromoted = (!movingPiece.isPromoted() && targetPiece.isPromoted()) || isPromoteMarker

                    // 駒を取る処理
                    val captured = currentCells[toY - 1][ShogiConstants.BOARD_SIZE - toX]
                    if (captured != null) {
                        if (isSente) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase())
                        if (firstContactStep == -1) firstContactStep = history.size
                    }

                    currentCells[fromY - 1][ShogiConstants.BOARD_SIZE - fromX] = null
                    currentCells[toY - 1][ShogiConstants.BOARD_SIZE - toX] = targetPiece to turnColor

                    if (isActuallyPromoted) movingPiece.symbol + "成" else movingPiece.symbol
                }

                val seconds = if (i + 1 < lines.size && lines[i + 1].trim().startsWith("T")) {
                    lines[i + 1].trim().substring(1).toIntOrNull()
                } else {
                    null
                }

                history.add(
                    BoardSnapshot(
                        cells = currentCells.map { it.toList() },
                        senteMochigoma = senteMochi.toList(),
                        goteMochigoma = goteMochi.toList(),
                        lastMoveText = "$moveCount $moveText",
                        lastFrom = if (fromX == 0) null else Square(fromX, fromY),
                        lastTo = Square(toX, toY),
                        consumptionSeconds = seconds,
                        evaluation = lastSnapshot.evaluation, // CSAには標準で評価値がないため継承
                    ),
                )
                moveCount++
            }
        }
    }

    state.updateSession(
        KifuSession(
            history = history,
            initialStep = history.size - 1,
            senteName = senteName,
            goteName = goteName,
            firstContactStep = firstContactStep,
            isStandardStart = true, // 簡易化のため平手のみ対応
        ),
    )
}
