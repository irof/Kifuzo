package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardLayout
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.Piece
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
    var startTime = ""
    var event = ""
    val builder = KifuSessionBuilder().apply { setup() }

    var moveCount = 1
    for (i in lines.indices) {
        val line = lines[i].trim()
        when {
            line.startsWith("N+") -> senteName = line.substring(2)
            line.startsWith("N-") -> goteName = line.substring(2)
            line.startsWith("\$START_TIME:") -> startTime = line.substringAfter(":").trim()
            line.startsWith("\$EVENT:") -> event = line.substringAfter(":").trim()
            line.startsWith("'") -> {
                extractCsaEvaluation(line, builder)
            }
            line.startsWith("+") || line.startsWith("-") -> {
                if (line.length < 7) continue
                val fromX = line[1] - '0'
                val fromY = line[2] - '0'
                val toX = line[3] - '0'
                val toY = line[4] - '0'
                val pieceCsa = line.substring(5, 7)

                val targetPiece = Piece.entries.find { it.name == pieceCsa } ?: Piece.FU

                val seconds = if (i + 1 < lines.size && lines[i + 1].trim().startsWith("T")) {
                    lines[i + 1].trim().substring(1).toIntOrNull()
                } else {
                    null
                }

                val lastSession = builder.build()
                val lastTo = lastSession.history.lastOrNull()?.lastTo
                val destinationText = if (lastTo != null && lastTo.file == toX && lastTo.rank == toY) {
                    "同　"
                } else {
                    BoardLayout.toShogiNotation(toX, toY)
                }

                if (fromX == 0) {
                    val moveText = destinationText + targetPiece.symbol + "打"
                    builder.applyDrop(targetPiece, Square(toX, toY), seconds, "$moveCount $moveText")
                } else {
                    val fromSquare = Square(fromX, fromY)
                    val currentSnapshot = builder.build().history.last()
                    val fromPiece = currentSnapshot.cells[fromSquare.yIndex][fromSquare.xIndex]?.first
                    val isPromote = fromPiece != null && !fromPiece.isPromoted() && targetPiece.isPromoted()

                    val movePieceSymbol = if (isPromote) fromPiece.symbol else targetPiece.symbol
                    val moveText = destinationText + movePieceSymbol + if (isPromote) "成" else ""
                    builder.applyMove(fromSquare, Square(toX, toY), isPromote, seconds, "$moveCount $moveText")
                }
                moveCount++
            }
        }
    }

    builder.setMetadata(sente = senteName, gote = goteName, start = startTime, ev = event)
    state.updateSession(builder.build())
}

private fun extractCsaEvaluation(line: String, builder: KifuSessionBuilder) {
    val currentEval = builder.getLastEvaluation().orNull()
    val isCurrentMate = currentEval != null && (kotlin.math.abs(currentEval) >= ShogiConstants.MATE_SCORE_THRESHOLD)

    if (line.contains("#詰み=先手勝ち")) {
        builder.updateLastEvaluation(Evaluation.SenteWin)
    } else if (line.contains("#詰み=後手勝ち")) {
        builder.updateLastEvaluation(Evaluation.GoteWin)
    } else if (!isCurrentMate) {
        val evalMatch = Regex("""'\*#評価値=([+-]?\d+)""").find(line)
        evalMatch?.groupValues?.get(1)?.toIntOrNull()?.let { eval ->
            val evaluation = when {
                eval >= ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.SenteWin
                eval <= -ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.GoteWin
                else -> Evaluation.Score(eval)
            }
            builder.updateLastEvaluation(evaluation)
        }
    }
}
