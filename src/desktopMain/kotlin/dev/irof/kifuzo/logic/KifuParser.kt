package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.Square
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.text.Charsets

private val logger = KotlinLogging.logger {}

fun scanKifuInfo(path: Path): KifuInfo = try {
    val lines = readLinesWithEncoding(path)
    scanKifuInfo(lines).copy(path = path)
} catch (e: Exception) {
    // スキャン時のエラーは、ファイルリスト表示を止めないようログに記録し、
    // UI上でエラー状態であることがわかるように KifuInfo を構築して返します。
    logger.error(e) { "Failed to scan header for ${path.name}" }
    KifuInfo(path, isError = true)
}

fun scanKifuInfo(lines: List<String>): KifuInfo {
    var sente = ""
    var gote = ""
    var senkei = ""
    var startTime = ""
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) sente = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("後手：")) gote = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("戦型：")) senkei = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("開始日時：")) startTime = trimmed.substringAfter("：").trim()
        if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) break
    }
    return KifuInfo(java.nio.file.Paths.get(""), sente, gote, senkei, startTime)
}

fun parseKifu(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseKifu(lines, state)
}

fun parseKifu(lines: List<String>, state: ShogiBoardState) {
    val header = parseHeader(lines)

    val currentCells = Array(9) { y -> header.initialCells[y].toTypedArray() }
    val senteMochi = header.senteMochi.toMutableList()
    val goteMochi = header.goteMochi.toMutableList()
    var firstContactStep = -1

    val history = mutableListOf<BoardSnapshot>()
    history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), "開始局面", evaluation = 0))

    if (header.moveStartIndex != -1) {
        var lastTo: Square? = null
        var isVariationSection = false

        for (i in header.moveStartIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("&")) continue
            if (line.startsWith("変化：") || line.startsWith("変化:")) {
                isVariationSection = true
            }
            if (isVariationSection) continue

            // コメント行の処理（評価値抽出）
            if (line.startsWith("*")) {
                if (history.isNotEmpty()) {
                    val lastIdx = history.size - 1
                    val currentEval = history[lastIdx].evaluation
                    val isCurrentMate = currentEval != null && (currentEval >= 30000 || currentEval <= -30000)

                    if (line.contains("#詰み=先手勝ち")) {
                        history[lastIdx] = history[lastIdx].copy(evaluation = 31111)
                    } else if (line.contains("#詰み=後手勝ち")) {
                        history[lastIdx] = history[lastIdx].copy(evaluation = -31111)
                    } else if (!isCurrentMate) {
                        // すでに詰みが検出されている場合は、通常の評価値で上書きしない
                        val evalMatch = Regex("""\*#評価値=([+-]?\d+)""").find(line)
                            ?: Regex("""\* ([+-]?\d+)""").find(line)
                        if (evalMatch != null) {
                            val eval = evalMatch.groupValues[1].toIntOrNull()
                            if (eval != null) {
                                history[lastIdx] = history[lastIdx].copy(evaluation = eval)
                            }
                        }
                    }
                }
                continue
            }

            val parsedMove = parseMove(line, lastTo) ?: continue

            try {
                when (parsedMove) {
                    is KifuParsedMove.Move -> {
                        val turnColor = if (parsedMove.moveNum % 2 != 0) PieceColor.Black else PieceColor.White
                        val toSquare = parsedMove.to
                        val fromSquare = parsedMove.from
                        val captured = currentCells[toSquare.yIndex][toSquare.xIndex]
                        if (captured != null) {
                            if (turnColor == PieceColor.Black) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase())
                            if (firstContactStep == -1) firstContactStep = history.size
                        }
                        val current = currentCells[fromSquare.yIndex][fromSquare.xIndex] ?: throw KifuParseException("移動元($fromSquare)に駒がありません。")
                        val piece = if (parsedMove.isPromote) {
                            when (current.first) {
                                Piece.FU -> Piece.TO
                                Piece.KY -> Piece.NY
                                Piece.KE -> Piece.NK
                                Piece.GI -> Piece.NG
                                Piece.KA -> Piece.UM
                                Piece.HI -> Piece.RY
                                else -> current.first
                            }
                        } else {
                            current.first
                        }
                        currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                        currentCells[fromSquare.yIndex][fromSquare.xIndex] = null
                        history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = fromSquare, lastTo = toSquare))
                        lastTo = toSquare
                    }
                    is KifuParsedMove.Drop -> {
                        val turnColor = if (parsedMove.moveNum % 2 != 0) PieceColor.Black else PieceColor.White
                        val toSquare = parsedMove.to
                        val piece = parsedMove.piece
                        if (turnColor == PieceColor.Black) senteMochi.remove(piece) else goteMochi.remove(piece)
                        currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                        history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = null, lastTo = toSquare))
                        lastTo = toSquare
                    }
                    is KifuParsedMove.Result -> {
                        // 投了などの終局。盤面は変えず、評価値を勝敗に合わせて設定する
                        val turnColor = if (parsedMove.moveNum % 2 != 0) PieceColor.Black else PieceColor.White
                        // 手番側が負け（投了など）なので、評価値は逆にする
                        val evaluation = if (turnColor == PieceColor.Black) -31111 else 31111
                        history.add(BoardSnapshot(currentCells.map { it.toList() }, senteMochi.toList(), goteMochi.toList(), line, evaluation = evaluation))
                    }
                }
            } catch (e: Exception) {
                throw KifuParseException("${i + 1}行目: ${e.message}\n(内容: $line)", e)
            }
        }
    }

    val initialStep = if (!header.isStandardStart) {
        0
    } else if (firstContactStep != -1) {
        firstContactStep
    } else if (history.isNotEmpty()) {
        history.size - 1
    } else {
        0
    }
    state.updateSession(
        KifuSession(
            history = history,
            initialStep = initialStep,
            senteName = header.senteName,
            goteName = header.goteName,
            firstContactStep = firstContactStep,
            isStandardStart = header.isStandardStart,
        ),
    )
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
    var senteName = "先手"
    var goteName = "後手"
    val currentCells = Array(9) { arrayOfNulls<Pair<Piece, PieceColor>>(9) }
    val senteMochi = mutableListOf<Piece>()
    val goteMochi = mutableListOf<Piece>()
    var isStandardStart = true
    var moveStartIndex = -1

    val pieceSymbolMap = Piece.entries.associateBy { it.symbol }
    var boardY = 0

    for (i in lines.indices) {
        val line = lines[i]
        val trimmed = line.trim()
        if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) senteName = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("後手：")) goteName = trimmed.substringAfter("：").trim()

        // 盤面行の解析
        if (trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2) {
            isStandardStart = false
            val content = trimmed.substringAfter("|").substringBeforeLast("|")

            // セパレータ '|' がある場合は分割、ない場合は2文字ずつの固定幅として扱う
            val cells = if (content.contains("|")) {
                content.split("|")
            } else {
                (0 until 9).map { idx ->
                    val start = idx * 2
                    if (start + 2 <= content.length) {
                        content.substring(start, start + 2)
                    } else if (start < content.length) {
                        content.substring(start)
                    } else {
                        ""
                    }
                }
            }

            for (x in 0..8) {
                if (x >= cells.size || boardY >= 9) break
                val pStr = cells[x]
                val isGote = pStr.contains('v')
                val pieceName = pStr.replace("v", "").replace("・", "").trim()

                val piece = Piece.findPieceBySymbol(pieceName)
                if (piece != null) {
                    val color = if (isGote) PieceColor.White else PieceColor.Black
                    currentCells[boardY][x] = piece to color
                }
            }
            boardY++
        }

        if (trimmed.startsWith("先手持駒：") || trimmed.startsWith("下手持駒：")) {
            isStandardStart = false
            senteMochi.addAll(Piece.parseMochigoma(trimmed.substringAfter("：")))
        }
        if (trimmed.startsWith("後手持駒：") || trimmed.startsWith("上手持駒：")) {
            isStandardStart = false
            goteMochi.addAll(Piece.parseMochigoma(trimmed.substringAfter("：")))
        }

        // 指し手開始行の判定
        if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) {
            // 盤面ヘッダの「 ９ ８ ７...」などを誤検知しないよう、実際に指し手としてパースできるか確認する
            if (parseMove(trimmed, null) != null) {
                moveStartIndex = i
                break
            }
        }
    }

    val finalCells = if (isStandardStart) {
        BoardSnapshot.getInitialCells()
    } else {
        if (boardY < 9) {
            throw KifuParseException("盤面図が不完全です（${boardY}行しか見つかりませんでした）。")
        }
        val pieceCount = currentCells.sumOf { row -> row.count { it != null } }
        if (pieceCount == 0) {
            throw KifuParseException("盤面図から駒を読み取れませんでした。盤面図の形式が解析に対応していない可能性があります。")
        }
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

private sealed class KifuParsedMove {
    abstract val moveNum: Int

    data class Move(
        override val moveNum: Int,
        val to: Square,
        val from: Square,
        val isPromote: Boolean,
    ) : KifuParsedMove()

    data class Drop(
        override val moveNum: Int,
        val to: Square,
        val piece: Piece,
    ) : KifuParsedMove()

    data class Result(
        override val moveNum: Int,
        val result: String,
    ) : KifuParsedMove()
}

private val moveRegex = Regex("""^\s*(\d+)\s+([^\s(]{2}|同\s*)([^\s(]+)\(([1-9]{2})\).*""")
private val dropRegex = Regex("""^\s*(\d+)\s+([^\s(]{2})([^\s(]+?)打.*""")
private val resultRegex = Regex("""^\s*(\d+)\s+(${dev.irof.kifuzo.models.GameResult.ALL_KEYWORDS.joinToString("|")}).*""")

private fun parseMove(line: String, lastTo: Square?): KifuParsedMove? {
    if (!Regex("""^\s*\d+\s+.*""").matches(line)) return null

    fun decodeX(c: Char): Int {
        val idx = "１２３４５６７８９123456789".indexOf(c)
        return if (idx == -1) -1 else (idx % 9) + 1
    }
    fun decodeY(c: Char): Int {
        val idx = "一二三四五六七八九１２３４５６７８９1234567８９".indexOf(c)
        return if (idx == -1) -1 else (idx % 9) + 1
    }

    val moveMatch = moveRegex.find(line)
    if (moveMatch != null) {
        val moveNum = moveMatch.groupValues[1].toInt()
        val toPosStr = moveMatch.groupValues[2].trim()
        val pieceName = moveMatch.groupValues[3].trim()
        val fromPosStr = moveMatch.groupValues[4]
        val isPromote = pieceName.contains("成") || pieceName == "竜" || pieceName == "馬" || pieceName == "龍" || pieceName == "圭" || pieceName == "杏" || pieceName == "全"
        val toSquare = if (toPosStr.startsWith("同")) lastTo ?: throw KifuParseException("同の移動先が不明です") else Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
        val fromSquare = Square(fromPosStr[0] - '0', fromPosStr[1] - '0')
        return KifuParsedMove.Move(moveNum, toSquare, fromSquare, isPromote)
    }

    val dropMatch = dropRegex.find(line)
    if (dropMatch != null) {
        val moveNum = dropMatch.groupValues[1].toInt()
        val toPosStr = dropMatch.groupValues[2]
        val pieceSym = dropMatch.groupValues[3].substring(0, 1)
        val toSquare = Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
        val piece = Piece.entries.find { it.symbol == pieceSym || (pieceSym == "王" && it == Piece.OU) || (pieceSym == "玉" && it == Piece.OU) || (pieceSym == "竜" && it == Piece.RY) || (pieceSym == "龍" && it == Piece.RY) || (pieceSym == "馬" && it == Piece.UM) } ?: throw KifuParseException("不明な駒種: $pieceSym")
        return KifuParsedMove.Drop(moveNum, toSquare, piece)
    }

    val resultMatch = resultRegex.find(line)
    if (resultMatch != null) {
        val moveNum = resultMatch.groupValues[1].toInt()
        val result = resultMatch.groupValues[2]
        return KifuParsedMove.Result(moveNum, result)
    }

    return null
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
