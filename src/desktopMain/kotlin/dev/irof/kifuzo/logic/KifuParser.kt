package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * 棋譜ファイルの簡易情報をスキャンします。
 */
fun scanKifuInfo(path: Path): KifuInfo = try {
    val lines = readLinesWithEncoding(path)
    scanKifuInfo(lines).copy(path = path)
} catch (e: IOException) {
    logger.error(e) { "IO error scanning header for ${path.name}" }
    KifuInfo(path, isError = true)
}

/**
 * 行リストから棋譜の簡易情報をスキャンします。
 */
fun scanKifuInfo(lines: List<String>): KifuInfo {
    val header = parseHeader(lines)
    return KifuInfo(
        path = java.nio.file.Paths.get(""),
        senteName = header.senteName,
        goteName = header.goteName,
        startTime = header.startTime,
        event = header.event,
    )
}

/**
 * KIF形式の棋譜ファイルを解析します。
 */
fun parseKifu(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseKifu(lines, state)
}

/**
 * KIF形式の行リストを解析します。
 */
fun parseKifu(lines: List<String>, state: ShogiBoardState) {
    val header = parseHeader(lines)
    val mainParser = ParserState(header)

    if (header.moveStartIndex == -1) {
        state.updateSession(mainParser.buildSession())
        return
    }

    val allParsers = mutableListOf(mainParser)
    val childrenMap = mutableMapOf<ParserState, MutableList<Pair<Int, ParserState>>>()
    var currentParser = mainParser

    for (i in header.moveStartIndex until lines.size) {
        val line = lines[i].trim()
        if (line.isNotEmpty() && !line.startsWith("#") && !line.startsWith("&")) {
            val variationMatch = Regex("""変化：(\d+)手""").find(line)
            if (variationMatch != null) {
                currentParser = handleVariation(variationMatch, allParsers, mainParser, header, childrenMap) ?: currentParser
            } else if (line.startsWith("*")) {
                currentParser.extractEvaluation(line)
            } else {
                handleMoveLine(line, i, currentParser)
            }
        }
    }

    applyVariationsRecursive(mainParser, childrenMap)
    state.updateSession(mainParser.buildSession())
}

private fun handleVariation(
    match: MatchResult,
    allParsers: MutableList<ParserState>,
    mainParser: ParserState,
    header: KifuHeader,
    childrenMap: MutableMap<ParserState, MutableList<Pair<Int, ParserState>>>,
): ParserState? {
    val atStep = match.groupValues[1].toInt() - 1
    val parent = findParentParser(allParsers, mainParser, atStep) ?: return null
    return parent.getSnapshotAt(atStep)?.let { baseSnapshot ->
        val child = ParserState(header, baseSnapshot, atStep).apply { lastTo = baseSnapshot.lastTo }
        childrenMap.getOrPut(parent) { mutableListOf() }.add(atStep to child)
        allParsers.add(child)
        child
    }
}

private fun findParentParser(allParsers: List<ParserState>, mainParser: ParserState, atStep: Int): ParserState? {
    var parent = allParsers.reversed().find { atStep >= it.startingStep && atStep < it.startingStep + it.historySize }
    if (parent != null && parent != mainParser && atStep == parent.startingStep) {
        val sibling = parent
        parent = allParsers.reversed().find { it != sibling && atStep >= it.startingStep && atStep < it.startingStep + it.historySize } ?: sibling
    }
    return parent
}

private fun applyVariationsRecursive(parser: ParserState, childrenMap: Map<ParserState, List<Pair<Int, ParserState>>>) {
    val children = childrenMap[parser] ?: return
    for ((atStep, childParser) in children) {
        applyVariationsRecursive(childParser, childrenMap)
        parser.addVariation(atStep, childParser.buildMoves())
    }
}

private fun handleMoveLine(line: String, lineIndex: Int, parserState: ParserState) {
    val parsedMove = parseMove(line, parserState.lastTo) ?: return
    try {
        parserState.applyMove(parsedMove, line)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw KifuParseException(message = "${lineIndex + 1}行目: ${e.message}", lineNumber = lineIndex + 1, lineContent = line, cause = e)
    }
}

private class ParserState(private val header: KifuHeader, initialSnapshot: BoardSnapshot? = null, startingStep: Int = 0) {
    private val builder = KifuSessionBuilder().apply {
        setup(
            senteName = header.senteName, goteName = header.goteName, startTime = header.startTime, event = header.event,
            initialCells = header.initialCells, senteMochi = header.senteMochi, goteMochi = header.goteMochi,
            isStandardStart = header.isStandardStart, startingStep = startingStep, snapshot = initialSnapshot,
        )
    }
    var lastTo: dev.irof.kifuzo.models.Square? = null
    val historySize: Int get() = builder.historySize
    val startingStep: Int get() = builder.currentStartingStep

    fun extractEvaluation(line: String) {
        val currentEval = builder.lastEvaluation.orNull()
        if (line.contains("#詰み=先手勝ち")) {
            builder.lastEvaluation = Evaluation.SenteWin
        } else if (line.contains("#詰み=後手勝ち")) {
            builder.lastEvaluation = Evaluation.GoteWin
        } else if (currentEval == null || kotlin.math.abs(currentEval) < ShogiConstants.MATE_SCORE_THRESHOLD) {
            applyEvaluationScore(line)
        }
    }

    private fun applyEvaluationScore(line: String) {
        val evalMatch = Regex("""\*#評価値=([+-]?\d+)""").find(line) ?: Regex("""\* ([+-]?\d+)""").find(line)
        evalMatch?.groupValues?.get(1)?.toIntOrNull()?.let { eval ->
            val evaluation = when {
                eval >= ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.SenteWin
                eval <= -ShogiConstants.MATE_SCORE_THRESHOLD -> Evaluation.GoteWin
                else -> Evaluation.Score(eval)
            }
            builder.lastEvaluation = evaluation
        }
    }

    fun applyMove(parsedMove: KifuParsedMove, line: String) {
        when (parsedMove) {
            is KifuParsedMove.Move -> {
                builder.applyAction(from = parsedMove.from, to = parsedMove.to, isPromote = parsedMove.isPromote, consumptionSeconds = parsedMove.consumptionSeconds, moveText = line)
                lastTo = parsedMove.to
            }
            is KifuParsedMove.Drop -> {
                builder.applyAction(to = parsedMove.to, piece = parsedMove.piece, consumptionSeconds = parsedMove.consumptionSeconds, moveText = line)
                lastTo = parsedMove.to
            }
            is KifuParsedMove.Result -> builder.applyAction(consumptionSeconds = null, moveText = "", resultText = line)
        }
    }

    fun getSnapshotAt(step: Int): BoardSnapshot? = builder.getSnapshotAt(step)
    fun addVariation(atStep: Int, moves: List<Move>) = builder.addVariation(atStep, moves)
    fun buildMoves(): List<Move> = builder.build().moves
    fun buildSession(): KifuSession = builder.build()
}

internal fun parseHeader(lines: List<String>): KifuHeader {
    val parser = HeaderParser()
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (parser.processLine(line, i)) break
    }
    return parser.build()
}

/**
 * 棋譜解析中に発生する例外。
 */
class KifuParseException(
    message: String,
    val lineNumber: Int? = null,
    val lineContent: String? = null,
    val path: Path? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
