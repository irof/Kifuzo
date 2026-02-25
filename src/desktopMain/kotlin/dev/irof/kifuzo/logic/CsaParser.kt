package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardLayout
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square
import java.nio.file.Path

private const val CSA_MOVE_LINE_MIN_LENGTH = 7
private const val FROM_X_POS = 1
private const val FROM_Y_POS = 2
private const val TO_X_POS = 3
private const val TO_Y_POS = 4
private const val PIECE_START_POS = 5
private const val PIECE_END_POS = 7

/**
 * CSA形式の棋譜を解析して ShogiBoardState を更新するクラス。
 */
fun parseCsa(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseCsa(lines, state)
}

private class CsaParseContext {
    var senteName = "先手"
    var goteName = "後手"
    var startTime = ""
    var event = ""
    var moveCount = 1
    val builder = KifuSessionBuilder().apply { setup() }
}

fun parseCsa(lines: List<String>, state: ShogiBoardState) {
    val ctx = CsaParseContext()

    for (i in lines.indices) {
        val line = lines[i].trim()
        when {
            line.startsWith("N+") -> ctx.senteName = line.substring(2)
            line.startsWith("N-") -> ctx.goteName = line.substring(2)
            line.startsWith("\$START_TIME:") -> ctx.startTime = line.substringAfter(":").trim()
            line.startsWith("\$EVENT:") -> ctx.event = line.substringAfter(":").trim()
            line.startsWith("P") && line.length >= 2 && line[1].isDigit() -> handleCsaBoardLine(line, ctx.builder)
            line.startsWith("'") -> extractCsaEvaluation(line, ctx.builder)
            line.startsWith("+") || line.startsWith("-") -> {
                if (line.length >= CSA_MOVE_LINE_MIN_LENGTH) {
                    handleCsaMoveLine(line, i, lines, ctx)
                    ctx.moveCount++
                }
            }
        }
    }

    ctx.builder.setMetadata(sente = ctx.senteName, gote = ctx.goteName, start = ctx.startTime, ev = ctx.event)
    state.updateSession(ctx.builder.build())
}

private fun handleCsaBoardLine(line: String, builder: KifuSessionBuilder) {
    val rowIdx = line[1] - '1'
    if (rowIdx !in 0 until ShogiConstants.BOARD_SIZE) return

    val cells = mutableListOf<Pair<Piece, PieceColor>?>()
    for (i in 0 until ShogiConstants.BOARD_SIZE) {
        val start = 2 + i * 3
        if (start + 3 > line.length) {
            cells.add(null)
            continue
        }
        val pieceStr = line.substring(start, start + 3)
        val piece: Pair<Piece, PieceColor>? = when {
            pieceStr == " * " -> null
            pieceStr.startsWith("+") -> findPieceForCsa(pieceStr.substring(1)) to PieceColor.Black
            pieceStr.startsWith("-") -> findPieceForCsa(pieceStr.substring(1)) to PieceColor.White
            else -> null
        }
        cells.add(piece)
    }
    builder.updateInitialBoardRow(rowIdx, cells)
}

private fun findPieceForCsa(name: String): Piece = Piece.entries.find { it.name == name } ?: Piece.FU

private fun handleCsaMoveLine(line: String, index: Int, lines: List<String>, ctx: CsaParseContext) {
    if (line.length < CSA_MOVE_LINE_MIN_LENGTH) return

    val fromX = line[FROM_X_POS] - '0'
    val fromY = line[FROM_Y_POS] - '0'
    val toX = line[TO_X_POS] - '0'
    val toY = line[TO_Y_POS] - '0'
    val pieceCsa = line.substring(PIECE_START_POS, PIECE_END_POS)
    val targetPiece = Piece.entries.find { it.name == pieceCsa } ?: Piece.FU

    val seconds = if (index + 1 < lines.size && lines[index + 1].trim().startsWith("T")) {
        lines[index + 1].trim().substring(1).toIntOrNull()
    } else {
        null
    }

    val lastSession = ctx.builder.build()
    val lastTo = lastSession.history.lastOrNull()?.lastTo
    val destinationText = if (lastTo != null && lastTo.file == toX && lastTo.rank == toY) {
        "同　"
    } else {
        BoardLayout.toShogiNotation(toX, toY)
    }

    if (fromX == 0) {
        val moveText = destinationText + targetPiece.symbol + "打"
        ctx.builder.applyDrop(targetPiece, Square(toX, toY), seconds, "${ctx.moveCount} $moveText")
    } else {
        applyCsaNormalMove(fromX, fromY, toX, toY, targetPiece, destinationText, seconds, ctx)
    }
}

private fun applyCsaNormalMove(
    fromX: Int,
    fromY: Int,
    toX: Int,
    toY: Int,
    targetPiece: Piece,
    destinationText: String,
    seconds: Int?,
    ctx: CsaParseContext,
) {
    val fromSquare = Square(fromX, fromY)
    val currentSnapshot = ctx.builder.build().history.last()
    val fromPiece = currentSnapshot.cells[fromSquare.yIndex][fromSquare.xIndex]?.first
    val isPromote = fromPiece != null && !fromPiece.isPromoted() && targetPiece.isPromoted()

    val movePieceSymbol = if (isPromote) fromPiece.symbol else targetPiece.symbol
    val moveText = destinationText + movePieceSymbol + if (isPromote) "成" else ""
    ctx.builder.applyMove(fromSquare, Square(toX, toY), isPromote, seconds, "${ctx.moveCount} $moveText")
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
