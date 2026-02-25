package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

fun scanKifuInfo(path: Path): KifuInfo = try {
    val lines = readLinesWithEncoding(path)
    scanKifuInfo(lines).copy(path = path)
} catch (e: IOException) {
    logger.error(e) { "IO error scanning header for ${path.name}" }
    KifuInfo(path, isError = true)
}

fun scanKifuInfo(lines: List<String>): KifuInfo {
    var info = KifuInfo(java.nio.file.Paths.get(""), "", "", "", "")
    for (line in lines) {
        val trimmed = line.trim()
        info = when {
            trimmed.startsWith("先手：") || trimmed.startsWith("対局者：") -> info.copy(senteName = trimmed.substringAfter("：").trim())
            trimmed.startsWith("後手：") -> info.copy(goteName = trimmed.substringAfter("：").trim())
            trimmed.startsWith("開始日時：") -> info.copy(startTime = trimmed.substringAfter("：").trim())
            trimmed.startsWith("棋戦：") -> info.copy(event = trimmed.substringAfter("：").trim())

            trimmed.startsWith("N+") -> info.copy(senteName = trimmed.substring(2).trim())
            trimmed.startsWith("N-") -> info.copy(goteName = trimmed.substring(2).trim())
            trimmed.startsWith("\$START_TIME:") -> info.copy(startTime = trimmed.substringAfter(":").trim())
            trimmed.startsWith("\$EVENT:") -> info.copy(event = trimmed.substringAfter(":").trim())

            Regex("""^\s*\d+\s+.*""").matches(trimmed) -> break
            trimmed.startsWith("+") || trimmed.startsWith("-") -> break
            else -> info
        }
    }
    return info
}

fun parseKifu(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseKifu(lines, state)
}

fun parseKifu(lines: List<String>, state: ShogiBoardState) {
    val header = parseHeader(lines)
    val mainParser = ParserState(header)

    val variations = mutableListOf<Pair<Int, ParserState>>()
    var currentParser = mainParser

    if (header.moveStartIndex != -1) {
        for (i in header.moveStartIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("&")) continue

            val variationMatch = Regex("""変化：(\d+)手""").find(line)
            if (variationMatch != null) {
                val atStep = variationMatch.groupValues[1].toInt() - 1
                val baseSnapshot = mainParser.getSnapshotAt(atStep)
                if (baseSnapshot != null) {
                    val varParser = ParserState(header, baseSnapshot)
                    variations.add(atStep to varParser)
                    currentParser = varParser
                }
                continue
            }

            if (line.startsWith("*")) {
                currentParser.extractEvaluation(line)
            } else {
                handleMoveLine(line, i, currentParser)
            }
        }
    }

    for ((atStep, varParser) in variations) {
        mainParser.addVariation(atStep, varParser.buildHistory())
    }

    state.updateSession(mainParser.buildSession())
}

private fun handleMoveLine(line: String, lineIndex: Int, parserState: ParserState) {
    val parsedMove = parseMove(line, parserState.lastTo) ?: return
    try {
        parserState.applyMove(parsedMove, line)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw KifuParseException("${lineIndex + 1}行目: ${e.message}\n(内容: $line)", e)
    }
}

private class ParserState(private val header: KifuHeader, initialSnapshot: BoardSnapshot? = null) {
    private val builder = KifuSessionBuilder().apply {
        if (initialSnapshot != null) {
            setupFromSnapshot(initialSnapshot)
        } else {
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
    var lastTo: dev.irof.kifuzo.models.Square? = null

    fun extractEvaluation(line: String) {
        val currentEval = builder.getLastEvaluation().orNull()
        val isCurrentMate = currentEval != null && (kotlin.math.abs(currentEval) >= ShogiConstants.MATE_SCORE_THRESHOLD)

        if (line.contains("#詰み=先手勝ち")) {
            builder.updateLastEvaluation(Evaluation.SenteWin)
        } else if (line.contains("#詰み=後手勝ち")) {
            builder.updateLastEvaluation(Evaluation.GoteWin)
        } else if (!isCurrentMate) {
            applyEvaluationScore(line)
        }
    }

    private fun applyEvaluationScore(line: String) {
        val evalMatch = Regex("""\*#評価値=([+-]?\d+)""").find(line)
            ?: Regex("""\* ([+-]?\d+)""").find(line)
        evalMatch?.groupValues?.get(1)?.toIntOrNull()?.let { eval ->
            val evaluation = when {
                eval >= ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.SenteWin
                eval <= -ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.GoteWin
                else -> Evaluation.Score(eval)
            }
            builder.updateLastEvaluation(evaluation)
        }
    }

    fun applyMove(parsedMove: KifuParsedMove, line: String) {
        when (parsedMove) {
            is KifuParsedMove.Move -> {
                builder.applyMove(parsedMove.from, parsedMove.to, parsedMove.isPromote, parsedMove.consumptionSeconds, line)
                lastTo = parsedMove.to
            }
            is KifuParsedMove.Drop -> {
                builder.applyDrop(parsedMove.piece, parsedMove.to, parsedMove.consumptionSeconds, line)
                lastTo = parsedMove.to
            }
            is KifuParsedMove.Result -> {
                builder.applyResult(line)
            }
        }
    }

    fun getSnapshotAt(step: Int): BoardSnapshot? = builder.getSnapshotAt(step)

    fun addVariation(atStep: Int, history: List<BoardSnapshot>) = builder.addVariation(atStep, history)

    fun buildHistory(): List<BoardSnapshot> = builder.build().history

    fun buildSession(): KifuSession = builder.build()
}

private fun parseHeader(lines: List<String>): KifuHeader {
    val parser = HeaderParser()
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (parser.processLine(line, i)) break
    }
    return parser.build()
}

class KifuParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
