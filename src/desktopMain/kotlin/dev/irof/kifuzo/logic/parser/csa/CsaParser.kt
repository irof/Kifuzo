package dev.irof.kifuzo.logic.parser.csa
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuHeader
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.Evaluation
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

private class CsaParseContext(header: KifuHeader) {
    var moveCount = 1
    val builder = KifuSessionBuilder().apply {
        setup(
            senteName = header.senteName,
            goteName = header.goteName,
            startTime = header.startTime,
            event = header.event,
            initialCells = header.initialCells,
            senteMochi = header.senteMochi,
            goteMochi = header.goteMochi,
            isStandardStart = header.isStandardStart,
        )
    }
}

/**
 * CSA形式の行リストを解析します。
 */
fun parseCsa(lines: List<String>, state: ShogiBoardState, warningMessage: String? = null) {
    val header = parseHeader(lines)
    val ctx = CsaParseContext(header)
    ctx.builder.warningMessage = warningMessage

    for (i in lines.indices) {
        val line = lines[i].trim()
        when {
            line.startsWith("+") || line.startsWith("-") -> {
                if (line.length >= CSA_MOVE_LINE_MIN_LENGTH) {
                    handleCsaMoveLine(line, i, lines, ctx)
                    ctx.moveCount++
                }
            }
            line.startsWith("%") -> {
                handleCsaResultLine(line, ctx)
            }
            line.startsWith("'") -> {
                extractCsaEvaluation(line, ctx.builder)
            }
        }
    }

    state.updateSession(ctx.builder.build())
}

private fun handleCsaResultLine(line: String, ctx: CsaParseContext) {
    val resultText = when (line) {
        "%TORYO" -> "投了"
        "%TSUMI" -> "詰み"
        "%CHUDAN" -> "中断"
        "%SENNICHITE" -> "千日手"
        "%TIME_UP" -> "切れ負け"
        "%JISHOGI" -> "持将棋"
        "%KACHI" -> "入玉勝ち"
        "%HIKIWAKE" -> "引き分け"
        else -> line.substring(1) // 未知のコードは % を除いてそのまま
    }
    ctx.builder.applyAction(consumptionSeconds = null, moveText = "", resultText = "${ctx.moveCount} $resultText")
}

private fun isValidCsaMove(line: String): Boolean = line.length >= CSA_MOVE_LINE_MIN_LENGTH &&
    line[FROM_X_POS].isDigit() && line[FROM_Y_POS].isDigit() &&
    line[TO_X_POS].isDigit() && line[TO_Y_POS].isDigit()

private fun handleCsaMoveLine(line: String, index: Int, lines: List<String>, ctx: CsaParseContext) {
    if (!isValidCsaMove(line)) {
        throw KifuParseException("${index + 1}行目: CSA形式の指し手として正しくありません: $line", lineNumber = index + 1, lineContent = line)
    }

    val fromX = line[FROM_X_POS] - '0'
    val fromY = line[FROM_Y_POS] - '0'
    val toX = line[TO_X_POS] - '0'
    val toY = line[TO_Y_POS] - '0'
    val pieceCsa = line.substring(PIECE_START_POS, PIECE_END_POS)
    val targetPiece = dev.irof.kifuzo.models.Piece.entries.find { it.name == pieceCsa } ?: dev.irof.kifuzo.models.Piece.FU

    val seconds = if (index + 1 < lines.size && lines[index + 1].trim().startsWith("T")) {
        lines[index + 1].trim().substring(1).toIntOrNull()
    } else {
        null
    }

    val destinationText = getCsaDestinationText(ctx, toX, toY)
    try {
        if (fromX == 0) {
            ctx.builder.applyAction(to = Square(toX, toY), piece = targetPiece, consumptionSeconds = seconds, moveText = "${ctx.moveCount} $destinationText${targetPiece.symbol}打")
        } else {
            applyCsaNormalMove(fromX, fromY, toX, toY, targetPiece, destinationText, seconds, ctx)
        }
    } catch (cause: Exception) {
        throw KifuParseException("${index + 1}行目: ${cause.message}", lineNumber = index + 1, lineContent = line, cause = cause)
    }
}

private fun getCsaDestinationText(ctx: CsaParseContext, toX: Int, toY: Int): String {
    val lastTo = ctx.builder.build().moves.lastOrNull()?.resultSnapshot?.lastTo
    return if (lastTo != null && lastTo.file == toX && lastTo.rank == toY) "同　" else dev.irof.kifuzo.models.BoardLayout.toShogiNotation(toX, toY)
}

private fun applyCsaNormalMove(
    fromX: Int,
    fromY: Int,
    toX: Int,
    toY: Int,
    targetPiece: dev.irof.kifuzo.models.Piece,
    destinationText: String,
    seconds: Int?,
    ctx: CsaParseContext,
) {
    val fromSquare = Square(fromX, fromY)

    val currentSnapshot = ctx.builder.build().getSnapshotAt(ctx.moveCount - 1) ?: ctx.builder.build().initialSnapshot
    val fromPiece = currentSnapshot.cells[fromSquare.yIndex][fromSquare.xIndex]?.piece
    val isPromote = fromPiece != null && !fromPiece.isPromoted() && targetPiece.isPromoted()

    val movePieceSymbol = if (isPromote) fromPiece?.symbol ?: "" else targetPiece.symbol
    val moveText = "${ctx.moveCount} $destinationText$movePieceSymbol${if (isPromote) "成" else ""}"
    ctx.builder.applyAction(from = fromSquare, to = Square(toX, toY), isPromote = isPromote, consumptionSeconds = seconds, moveText = moveText)
}

private fun extractCsaEvaluation(line: String, builder: KifuSessionBuilder) {
    val currentEval = builder.lastEvaluation.orNull()
    if (line.contains("#詰み=先手勝ち")) {
        builder.lastEvaluation = Evaluation.SenteWin
    } else if (line.contains("#詰み=後手勝ち")) {
        builder.lastEvaluation = Evaluation.GoteWin
    } else if (currentEval == null || kotlin.math.abs(currentEval) < ShogiConstants.MATE_SCORE_THRESHOLD) {
        val evalMatch = Regex("""'\*#評価値=([+-]?\d+)""").find(line)
        evalMatch?.groupValues?.get(1)?.toIntOrNull()?.let { eval ->
            val evaluation = when {
                eval >= ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.SenteWin
                eval <= -ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.GoteWin
                else -> Evaluation.Score(eval)
            }
            builder.lastEvaluation = evaluation
        }
    }
}
