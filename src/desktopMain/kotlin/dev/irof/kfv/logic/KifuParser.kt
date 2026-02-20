package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.text.Charsets

fun scanKifuInfo(path: Path): KifuInfo = try {
    val lines = readLinesWithEncoding(path)
    scanKifuInfo(lines).copy(path = path)
} catch (e: Exception) {
    System.err.println("Failed to scan header for ${path.name}: ${e.message}")
    KifuInfo(path, "", "", "")
}

fun scanKifuInfo(lines: List<String>): KifuInfo {
    var sente = ""
    var gote = ""
    var senkei = ""
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) sente = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("後手：")) gote = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("戦型：")) senkei = trimmed.substringAfter("：").trim()
        if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) break
    }
    return KifuInfo(java.nio.file.Paths.get(""), sente, gote, senkei)
}

fun parseKifu(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    parseKifu(lines, state)
}

fun parseKifu(lines: List<String>, state: ShogiBoardState) {
    val header = parseHeader(lines)

    var currentCells = header.initialCells
    val senteMochi = header.senteMochi.toMutableList()
    val goteMochi = header.goteMochi.toMutableList()
    var firstContactStep = -1

    val history = mutableListOf<BoardSnapshot>()
    history.add(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), "開始局面"))

    if (header.moveStartIndex != -1) {
        val moveRegex = Regex("""^\s*(\d+)\s+([^\s(]{2}|同\s*)([^\s(]+)\(([1-9]{2})\).*""")
        val dropRegex = Regex("""^\s*(\d+)\s+([^\s(]{2})([^\s(]+?)打.*""")
        var lastTo: Square? = null
        var isVariationSection = false

        fun decodeX(c: Char): Int {
            val idx = "１２３４５６７８９123456789".indexOf(c)
            return if (idx == -1) -1 else (idx % 9) + 1
        }
        fun decodeY(c: Char): Int {
            val idx = "一二三四五六七八九１２３４５６７８９1234567８９".indexOf(c)
            return if (idx == -1) -1 else (idx % 9) + 1
        }

        for (i in header.moveStartIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("*") || line.startsWith("#") || line.startsWith("&")) continue
            if (line.startsWith("変化：") || line.startsWith("変化:")) {
                isVariationSection = true
            }
            if (isVariationSection) continue
            if (!Regex("""^\s*\d+\s+.*""").matches(line)) continue

            try {
                val moveMatch = moveRegex.find(line)
                if (moveMatch != null) {
                    val moveNum = moveMatch.groupValues[1].toInt()
                    val turnColor = if (moveNum % 2 != 0) PieceColor.Black else PieceColor.White
                    val toPosStr = moveMatch.groupValues[2].trim()
                    val pieceName = moveMatch.groupValues[3].trim()
                    val fromPosStr = moveMatch.groupValues[4]
                    val isPromote = pieceName.contains("成") || pieceName == "竜" || pieceName == "馬" || pieceName == "龍" || pieceName == "圭" || pieceName == "杏" || pieceName == "全"
                    val toSquare = if (toPosStr.startsWith("同")) lastTo ?: throw Exception("同の移動先が不明です") else Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
                    val fromSquare = Square(fromPosStr[0] - '0', fromPosStr[1] - '0')
                    val captured = currentCells[toSquare.yIndex][toSquare.xIndex]
                    if (captured != null) {
                        if (turnColor == PieceColor.Black) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase())
                        if (firstContactStep == -1) firstContactStep = history.size
                    }
                    val current = currentCells[fromSquare.yIndex][fromSquare.xIndex] ?: throw Exception("移動元($fromSquare)に駒がありません。")
                    val piece = if (isPromote) {
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
                    history.add(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = fromSquare, lastTo = toSquare))
                    lastTo = toSquare
                    continue
                }
                val dropMatch = dropRegex.find(line)
                if (dropMatch != null) {
                    val moveNum = dropMatch.groupValues[1].toInt()
                    val turnColor = if (moveNum % 2 != 0) PieceColor.Black else PieceColor.White
                    val toPosStr = dropMatch.groupValues[2]
                    val pieceSym = dropMatch.groupValues[3].substring(0, 1)
                    val toSquare = Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
                    val piece = Piece.entries.find { it.symbol == pieceSym || (pieceSym == "王" && it == Piece.OU) || (pieceSym == "玉" && it == Piece.OU) || (pieceSym == "竜" && it == Piece.RY) || (pieceSym == "龍" && it == Piece.RY) || (pieceSym == "馬" && it == Piece.UM) } ?: throw Exception("不明な駒種: $pieceSym")
                    if (turnColor == PieceColor.Black) senteMochi.remove(piece) else goteMochi.remove(piece)
                    currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                    history.add(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = null, lastTo = toSquare))
                    lastTo = toSquare
                    continue
                }
            } catch (e: Exception) {
                throw Exception("${i + 1}行目: ${e.message}\n(内容: $line)")
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
    val initialCells: Array<Array<Pair<Piece, PieceColor>?>>,
    val senteMochi: List<Piece>,
    val goteMochi: List<Piece>,
    val isStandardStart: Boolean,
    val moveStartIndex: Int,
)

private fun parseHeader(lines: List<String>): KifuHeader {
    var senteName = "先手"
    var goteName = "後手"
    var currentCells = Array(9) { arrayOfNulls<Pair<Piece, PieceColor>>(9) }
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
        if (trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2) {
            isStandardStart = false
            val content = trimmed.substringAfter("|").substringBeforeLast("|")
            val cells = content.split("|")
            for (x in 0..8) {
                if (x >= cells.size) break
                val pStr = cells[x].trim()
                if (pStr.isEmpty()) continue

                val color = if (pStr.startsWith('v')) PieceColor.White else PieceColor.Black
                val pieceName = (if (pStr.startsWith('v')) pStr.substring(1) else pStr).trim()

                if (pieceName.isNotEmpty() && pieceName != "・" && pieceName != "　") {
                    val piece = pieceSymbolMap[pieceName] ?: if (pieceName == "王") {
                        Piece.OU
                    } else if (pieceName == "玉") {
                        Piece.OU
                    } else if (pieceName == "竜") {
                        Piece.RY
                    } else if (pieceName == "馬") {
                        Piece.UM
                    } else {
                        null
                    }
                    if (piece != null) currentCells[boardY][x] = piece to color
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
        if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) {
            moveStartIndex = i
            break
        }
    }

    if (isStandardStart) currentCells = BoardSnapshot.getInitialCells()

    return KifuHeader(
        senteName = senteName,
        goteName = goteName,
        initialCells = currentCells,
        senteMochi = senteMochi,
        goteMochi = goteMochi,
        isStandardStart = isStandardStart,
        moveStartIndex = moveStartIndex,
    )
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
