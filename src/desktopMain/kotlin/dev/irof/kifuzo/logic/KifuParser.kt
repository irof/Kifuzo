package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.models.Square
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.text.Charsets

private val logger = KotlinLogging.logger {}

fun scanKifuInfo(path: Path): KifuInfo = try {
    val lines = readLinesWithEncoding(path)
    scanKifuInfo(lines).copy(path = path)
} catch (e: IOException) {
    logger.error(e) { "IO error scanning header for ${path.name}" }
    KifuInfo(path, isError = true)
}

fun scanKifuInfo(lines: List<String>): KifuInfo {
    var sente = ""
    var gote = ""
    var senkei = ""
    var startTime = ""
    for (line in lines) {
        val trimmed = line.trim()
        // KIF format
        if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) sente = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("後手：")) gote = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("戦型：")) senkei = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("開始日時：")) startTime = trimmed.substringAfter("：").trim()

        // CSA format
        if (trimmed.startsWith("N+")) sente = trimmed.substring(2).trim()
        if (trimmed.startsWith("N-")) gote = trimmed.substring(2).trim()
        if (trimmed.startsWith("\$START_TIME:")) startTime = trimmed.substringAfter(":").trim()

        if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) break
        if (trimmed.startsWith("+") || trimmed.startsWith("-")) break
    }
    return KifuInfo(java.nio.file.Paths.get(""), sente, gote, senkei, startTime)
}

fun parseKifu(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseKifu(lines, state)
}

fun parseKifu(lines: List<String>, state: ShogiBoardState) {
    val header = parseHeader(lines)
    val parserState = ParserState(header)

    if (header.moveStartIndex != -1) {
        var isVariationSection = false
        for (i in header.moveStartIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("&")) continue
            if (line.startsWith("変化：") || line.startsWith("変化:")) {
                isVariationSection = true
            }
            if (isVariationSection) continue

            if (line.startsWith("*")) {
                parserState.extractEvaluation(line)
                continue
            }

            val parsedMove = parseMove(line, parserState.lastTo) ?: continue
            try {
                parserState.applyMove(parsedMove, line)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                throw KifuParseException("${i + 1}行目: ${e.message}\n(内容: $line)", e)
            }
        }
    }

    val initialStep = parserState.calculateInitialStep(header.isStandardStart)
    state.updateSession(
        KifuSession(
            history = parserState.history,
            initialStep = initialStep,
            senteName = header.senteName,
            goteName = header.goteName,
            firstContactStep = parserState.firstContactStep,
            isStandardStart = header.isStandardStart,
        ),
    )
}

private class ParserState(header: KifuHeader) {
    val currentCells = Array(ShogiConstants.BOARD_SIZE) { y -> header.initialCells[y].toTypedArray() }
    val senteMochi = header.senteMochi.toMutableList()
    val goteMochi = header.goteMochi.toMutableList()
    var firstContactStep = -1
    var lastTo: Square? = null
    val history = mutableListOf<BoardSnapshot>().apply {
        add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), "開始局面"))
    }

    fun extractEvaluation(line: String) {
        if (history.isEmpty()) return
        val lastIdx = history.size - 1
        val currentEval = history[lastIdx].evaluation
        val isCurrentMate = currentEval != null && (kotlin.math.abs(currentEval) >= ShogiConstants.MATE_SCORE_THRESHOLD)

        if (line.contains("#詰み=先手勝ち")) {
            history[lastIdx] = history[lastIdx].copy(evaluation = ShogiConstants.WIN_SCORE)
        } else if (line.contains("#詰み=後手勝ち")) {
            history[lastIdx] = history[lastIdx].copy(evaluation = ShogiConstants.LOSE_SCORE)
        } else if (!isCurrentMate) {
            val evalMatch = Regex("""\*#評価値=([+-]?\d+)""").find(line)
                ?: Regex("""\* ([+-]?\d+)""").find(line)
            evalMatch?.groupValues?.get(1)?.toIntOrNull()?.let { eval ->
                history[lastIdx] = history[lastIdx].copy(evaluation = eval)
            }
        }
    }

    fun applyMove(parsedMove: KifuParsedMove, line: String) {
        val turnColor = if (parsedMove.moveNum % 2 != 0) PieceColor.Black else PieceColor.White
        when (parsedMove) {
            is KifuParsedMove.Move -> {
                val toSquare = parsedMove.to
                val fromSquare = parsedMove.from
                val captured = currentCells[toSquare.yIndex][toSquare.xIndex]
                if (captured != null) {
                    if (turnColor == PieceColor.Black) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase())
                    if (firstContactStep == -1) firstContactStep = history.size
                }
                val current = currentCells[fromSquare.yIndex][fromSquare.xIndex] ?: throw KifuParseException("移動元($fromSquare)に駒がありません。")
                val piece = if (parsedMove.isPromote) current.first.promote() else current.first
                currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                currentCells[fromSquare.yIndex][fromSquare.xIndex] = null
                history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = fromSquare, lastTo = toSquare, consumptionSeconds = parsedMove.consumptionSeconds, evaluation = history.last().evaluation))
                lastTo = toSquare
            }
            is KifuParsedMove.Drop -> {
                val toSquare = parsedMove.to
                val piece = parsedMove.piece
                if (turnColor == PieceColor.Black) senteMochi.remove(piece) else goteMochi.remove(piece)
                currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = null, lastTo = toSquare, consumptionSeconds = parsedMove.consumptionSeconds, evaluation = history.last().evaluation))
                lastTo = toSquare
            }
            is KifuParsedMove.Result -> {
                val evaluation = if (turnColor == PieceColor.Black) ShogiConstants.LOSE_SCORE else ShogiConstants.WIN_SCORE
                history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), line, evaluation = evaluation))
            }
        }
    }

    fun calculateInitialStep(isStandardStart: Boolean): Int = when {
        !isStandardStart -> 0
        firstContactStep != -1 -> firstContactStep
        history.isNotEmpty() -> history.size - 1
        else -> 0
    }
}

private data class KifuHeader(
    val senteName: String,
    val goteName: String,
    val initialCells: List<List<Pair<Piece, PieceColor>?>>,
    val senteMochi: List<Piece>,
    val goteMochi: List<Piece>,
    val isStandardStart: Boolean,
    val moveStartIndex: Int,
)

private fun parseHeader(lines: List<String>): KifuHeader {
    val parser = HeaderParser()
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (parser.processLine(line, i)) break
    }
    return parser.build()
}

private class HeaderParser {
    private var senteName = "先手"
    private var goteName = "後手"
    private val currentCells = Array(ShogiConstants.BOARD_SIZE) { arrayOfNulls<Pair<Piece, PieceColor>>(ShogiConstants.BOARD_SIZE) }
    private val senteMochi = mutableListOf<Piece>()
    private val goteMochi = mutableListOf<Piece>()
    private var isStandardStart = true
    private var moveStartIndex = -1
    private var boardY = 0

    fun processLine(line: String, index: Int): Boolean = when {
        line.startsWith("先手：") || line.startsWith("対局者：") || line.startsWith("後手：") -> {
            handlePlayerName(line)
            false
        }
        line.startsWith("|") && line.count { it == '|' } >= 2 -> {
            isStandardStart = false
            parseBoardLine(line)
            false
        }
        line.startsWith("先手持駒：") || line.startsWith("下手持駒：") ||
            line.startsWith("後手持駒：") || line.startsWith("上手持駒：") -> {
            isStandardStart = false
            handleMochigomaLine(line)
            false
        }
        Regex("""^\s*\d+\s+.*""").matches(line) -> {
            if (parseMove(line, null) != null) {
                moveStartIndex = index
                true
            } else {
                false
            }
        }
        else -> false
    }

    private fun handlePlayerName(line: String) {
        if (line.startsWith("後手：")) {
            goteName = line.substringAfter("：").trim()
        } else {
            senteName = line.substringAfter("：").trim()
        }
    }

    private fun handleMochigomaLine(line: String) {
        val pieces = Piece.parseMochigoma(line.substringAfter("："))
        if (line.startsWith("先手") || line.startsWith("下手")) {
            senteMochi.addAll(pieces)
        } else {
            goteMochi.addAll(pieces)
        }
    }

    private fun parseBoardLine(line: String) {
        if (boardY >= ShogiConstants.BOARD_SIZE) return
        val content = line.substringAfter("|").substringBeforeLast("|")
        val cells = splitBoardCells(content)

        for (x in 0 until ShogiConstants.BOARD_SIZE) {
            if (x >= cells.size) break
            val pStr = cells[x]
            val piece = Piece.findPieceBySymbol(pStr.replace("v", "").replace("・", "").trim())
            if (piece != null) {
                val color = if (pStr.contains('v')) PieceColor.White else PieceColor.Black
                currentCells[boardY][x] = piece to color
            }
        }
        boardY++
    }

    private fun splitBoardCells(content: String): List<String> = if (content.contains("|")) {
        content.split("|")
    } else {
        (0 until ShogiConstants.BOARD_SIZE).map { idx ->
            val start = idx * 2
            if (start + 2 <= content.length) content.substring(start, start + 2) else ""
        }
    }

    fun build(): KifuHeader {
        val finalCells = if (isStandardStart) {
            BoardSnapshot.getInitialCells()
        } else {
            if (boardY < ShogiConstants.BOARD_SIZE) throw KifuParseException("盤面図が不完全です（${boardY}行しか見つかりませんでした）。")
            val pieceCount = currentCells.sumOf { row -> row.count { it != null } }
            if (pieceCount == 0) throw KifuParseException("盤面図から駒を読み取れませんでした。")
            currentCells.map { it.toList() }
        }

        return KifuHeader(
            senteName = senteName,
            goteName = goteName,
            initialCells = finalCells,
            senteMochi = senteMochi,
            goteMochi = goteMochi,
            isStandardStart = isStandardStart,
            moveStartIndex = moveStartIndex,
        )
    }
}

private val moveRegex = Regex("""^\s*(?<moveNum>\d+)\s+(?<toPos>[^\s(]{2}|同\s*)(?<pieceName>[^\s(]+)\((?<fromPos>[1-9]{2})\)\s*(?:\(\s*(?<timeMin>\d+)\s*:\s*(?<timeSec>\d+)\s*/.*?\))?.*""")
private val dropRegex = Regex("""^\s*(?<moveNum>\d+)\s+(?<toPos>[^\s(]{2})(?<pieceName>[^\s(]+?)打\s*(?:\(\s*(?<timeMin>\d+)\s*:\s*(?<timeSec>\d+)\s*/.*?\))?.*""")
private val resultRegex = Regex("""^\s*(\d+)\s+(${dev.irof.kifuzo.models.GameResult.ALL_KEYWORDS.joinToString("|")}).*""")

private sealed class KifuParsedMove {
    abstract val moveNum: Int
    abstract val consumptionSeconds: Int?

    data class Move(
        override val moveNum: Int,
        val to: Square,
        val from: Square,
        val isPromote: Boolean,
        override val consumptionSeconds: Int? = null,
    ) : KifuParsedMove()

    data class Drop(
        override val moveNum: Int,
        val to: Square,
        val piece: Piece,
        override val consumptionSeconds: Int? = null,
    ) : KifuParsedMove()

    data class Result(
        override val moveNum: Int,
        val result: String,
    ) : KifuParsedMove() {
        override val consumptionSeconds: Int? = null
    }
}

private fun parseMove(line: String, lastTo: Square?): KifuParsedMove? {
    if (!Regex("""^\s*\d+\s+.*""").matches(line)) return null

    return parseNormalMove(line, lastTo)
        ?: parseDropMove(line)
        ?: parseResultMove(line)
}

private fun parseNormalMove(line: String, lastTo: Square?): KifuParsedMove.Move? {
    val match = moveRegex.find(line) ?: return null
    val moveNum = match.groups["moveNum"]?.value?.toIntOrNull() ?: return null
    val toPosStr = match.groups["toPos"]?.value?.trim() ?: ""
    val pieceName = match.groups["pieceName"]?.value?.trim() ?: ""
    val fromPosStr = match.groups["fromPos"]?.value ?: ""

    val consumptionSeconds = parseTime(match)

    val isPromote = pieceName.contains("成") || pieceName in listOf("竜", "馬", "龍", "圭", "杏", "全")
    val toSquare = if (toPosStr.startsWith("同")) {
        lastTo ?: throw KifuParseException("同の移動先が不明です")
    } else {
        Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
    }
    val fromSquare = Square(fromPosStr[0] - '0', fromPosStr[1] - '0')

    return KifuParsedMove.Move(moveNum, toSquare, fromSquare, isPromote, consumptionSeconds)
}

private fun parseDropMove(line: String): KifuParsedMove.Drop? {
    val match = dropRegex.find(line) ?: return null
    val moveNum = match.groups["moveNum"]?.value?.toIntOrNull() ?: return null
    val toPosStr = match.groups["toPos"]?.value ?: ""
    val pieceSym = match.groups["pieceName"]?.value?.substring(0, 1) ?: ""

    val consumptionSeconds = parseTime(match)

    val toSquare = Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
    val piece = Piece.entries.find {
        it.symbol == pieceSym ||
            (pieceSym == "王" && it == Piece.OU) ||
            (pieceSym == "玉" && it == Piece.OU) ||
            (pieceSym == "竜" && it == Piece.RY) ||
            (pieceSym == "龍" && it == Piece.RY) ||
            (pieceSym == "馬" && it == Piece.UM)
    } ?: throw KifuParseException("不明な駒種: $pieceSym")

    return KifuParsedMove.Drop(moveNum, toSquare, piece, consumptionSeconds)
}

private fun parseTime(match: MatchResult): Int? {
    val mins = match.groups["timeMin"]?.value?.trim()?.toIntOrNull() ?: return null
    val secs = match.groups["timeSec"]?.value?.trim()?.toIntOrNull() ?: return null
    return mins * ShogiConstants.SECONDS_IN_MINUTE + secs
}

private fun parseResultMove(line: String): KifuParsedMove.Result? {
    val match = resultRegex.find(line) ?: return null
    val moveNum = match.groupValues[1].toInt()
    val result = match.groupValues[2]
    return KifuParsedMove.Result(moveNum, result)
}

private fun decodeX(c: Char): Int {
    val idx = "１２３４５６７８９123456789".indexOf(c)
    return if (idx == -1) -1 else (idx % ShogiConstants.BOARD_SIZE) + 1
}

private fun decodeY(c: Char): Int {
    val idx = "一二三四五六七八九１２３４５６７８９1234567８９".indexOf(c)
    return if (idx == -1) -1 else (idx % ShogiConstants.BOARD_SIZE) + 1
}

fun updateKifuSenkei(path: Path, senkei: String) {
    if (senkei.isEmpty()) return
    val lines = readLinesWithEncoding(path).toMutableList()
    var senkeiLineIndex = -1
    var headerEndIndex = 0
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.startsWith("戦型：")) {
            senkeiLineIndex = i
            break
        }
        if (Regex("""^\s*\d+\s+.*""").matches(line)) {
            headerEndIndex = i
            break
        }
    }
    if (senkeiLineIndex != -1) {
        lines[senkeiLineIndex] = "戦型：$senkei"
    } else {
        lines.add(headerEndIndex, "戦型：$senkei")
    }
    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}

fun updateKifuResult(path: Path, result: String) {
    if (result.isEmpty()) return
    val lines = readLinesWithEncoding(path).toMutableList()

    // 最後の「指し手」（終局行以外）の番号を探す
    var lastActualMoveNum = 0
    for (line in lines) {
        val match = Regex("""^\s*(\d+)\s+.*""").find(line)
        if (match != null) {
            if (!dev.irof.kifuzo.models.GameResult.isResultLine(line)) {
                val num = match.groupValues[1].toIntOrNull() ?: 0
                if (num > lastActualMoveNum) lastActualMoveNum = num
            }
        }
    }

    val nextMoveNum = lastActualMoveNum + 1
    val resultLine = String.format(java.util.Locale.US, "%4d %s", nextMoveNum, result)

    // すでに終局行があるかチェック
    val existingResultIndex = lines.indexOfLast { line ->
        dev.irof.kifuzo.models.GameResult.isResultLine(line)
    }

    if (existingResultIndex != -1) {
        lines[existingResultIndex] = resultLine
    } else {
        lines.add(resultLine)
    }
    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}

class KifuParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
