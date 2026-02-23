package dev.irof.kifuzo.logic

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
    val builder = KifuSessionBuilder().apply { setup() }

    var moveCount = 1
    for (i in lines.indices) {
        val line = lines[i].trim()
        when {
            line.startsWith("N+") -> senteName = line.substring(2)
            line.startsWith("N-") -> goteName = line.substring(2)
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
                val isPromoteMarker = line.endsWith("+")

                val targetPiece = Piece.entries.find { it.name == pieceCsa } ?: Piece.FU

                val seconds = if (i + 1 < lines.size && lines[i + 1].trim().startsWith("T")) {
                    lines[i + 1].trim().substring(1).toIntOrNull()
                } else {
                    null
                }

                if (fromX == 0) {
                    val moveText = "${targetPiece.symbol}打"
                    builder.applyDrop(targetPiece, Square(toX, toY), seconds, "$moveCount $moveText")
                } else {
                    // CSAでは、移動後の駒(targetPiece)が成駒である場合に、それが今回の手で成ったのか、元々成っていたのかを判断する必要がある。
                    // ここでは簡易的に、移動前の駒が成駒でなく、移動後の駒が成駒である場合に「成」と表示する。
                    val fromSquare = Square(fromX, fromY)
                    // builderの内部状態を覗くのは良くないので、将来的にbuilder側で判断させるべきだが、
                    // 現状はKifuParserと同様に外部でテキストを構築して渡す。
                    val moveText = targetPiece.symbol + if (isPromoteMarker) "成" else ""
                    builder.applyMove(fromSquare, Square(toX, toY), isPromoteMarker, seconds, "$moveCount $moveText")
                }
                moveCount++
            }
        }
    }

    builder.setPlayers(sente = senteName, gote = goteName)
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
