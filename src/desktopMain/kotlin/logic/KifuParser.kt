package logic

import models.BoardSnapshot
import models.Piece
import models.ShogiBoardState
import java.io.File
import kotlin.Exception

fun getInitialCells(): Array<Array<Pair<Piece, Boolean>?>> {
    val cells = Array(9) { arrayOfNulls<Pair<Piece, Boolean>>(9) }
    cells[0][0] = Piece.KY to false; cells[0][1] = Piece.KE to false; cells[0][2] = Piece.GI to false
    cells[0][3] = Piece.KI to false; cells[0][4] = Piece.OU to false; cells[0][5] = Piece.KI to false
    cells[0][6] = Piece.GI to false; cells[0][7] = Piece.KE to false; cells[0][8] = Piece.KY to false
    cells[1][1] = Piece.HI to false; cells[1][7] = Piece.KA to false
    for (i in 0..8) cells[2][i] = Piece.FU to false
    cells[8][0] = Piece.KY to true; cells[8][1] = Piece.KE to true; cells[8][2] = Piece.GI to true
    cells[8][3] = Piece.KI to true; cells[8][4] = Piece.OU to true; cells[8][5] = Piece.KI to true
    cells[8][6] = Piece.GI to true; cells[8][7] = Piece.KE to true; cells[8][8] = Piece.KY to true
    cells[7][1] = Piece.KA to true; cells[7][7] = Piece.HI to true
    for (i in 0..8) cells[6][i] = Piece.FU to true
    return cells
}

fun parseKifu(file: File, state: ShogiBoardState) {
    val initialCells = getInitialCells()
    state.reset(initialCells)
    var currentCells = Array(9) { y -> Array(9) { x -> initialCells[y][x] } }
    var senteMochi = mutableListOf<Piece>()
    var goteMochi = mutableListOf<Piece>()
    val lines = file.readLines(Charsets.UTF_8)
    val moveRegex = Regex("""^\s*(\d+)\s+([^\s(]{2}|同\s*)([^\s(]+)\(([1-9]{2})\).*""")
    val dropRegex = Regex("""^\s*(\d+)\s+([^\s(]{2})([^\s(]+?)打.*""")
    var lastToX = -1; var lastToY = -1; var isVariationSection = false
    fun decodeX(c: Char): Int { val idx = "１２３４５６７８９123456789".indexOf(c); return if (idx == -1) -1 else (idx % 9) + 1 }
    fun decodeY(c: Char): Int { val idx = "一二三四五六七八九１２３４５６７８９123456789".indexOf(c); return if (idx == -1) -1 else (idx % 9) + 1 }
    lines.forEachIndexed { index, line ->
        val lineNum = index + 1
        try {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            if (trimmed.startsWith("先手：")) { state.senteName = trimmed.substring(3).trim(); return@forEachIndexed }
            if (trimmed.startsWith("後手：")) { state.goteName = trimmed.substring(3).trim(); return@forEachIndexed }
            if (trimmed.startsWith("*") || trimmed.startsWith("#") || trimmed.startsWith("&")) return@forEachIndexed
            if (trimmed.startsWith("変化：") || trimmed.startsWith("変化:")) { isVariationSection = true }
            if (isVariationSection) return@forEachIndexed
            if (!Regex("""^\s*\d+\s+.*""").matches(line)) return@forEachIndexed
            val moveMatch = moveRegex.find(line)
            if (moveMatch != null) {
                val moveNum = moveMatch.groupValues[1].toInt(); val isSente = moveNum % 2 != 0
                val toPosStr = moveMatch.groupValues[2].trim(); var pieceName = moveMatch.groupValues[3].trim(); val fromPosStr = moveMatch.groupValues[4]
                val isPromote = pieceName.endsWith("成") && pieceName.length > 1 && pieceName != "成銀" && pieceName != "成桂" && pieceName != "成香"
                val toX: Int; val toY: Int
                if (toPosStr.startsWith("同")) { toX = lastToX; toY = lastToY }
                else { val tx = decodeX(toPosStr[0]); val ty = decodeY(toPosStr[1]); if (tx == -1 || ty == -1) return@forEachIndexed; toX = 9 - tx; toY = ty - 1 }
                val fromX = 9 - (fromPosStr[0] - '0'); val fromY = (fromPosStr[1] - '0') - 1
                val captured = currentCells[toY][toX]
                if (captured != null) { if (isSente) senteMochi.add(captured.first.toBase()) else goteMochi.add(captured.first.toBase()) }
                val current = currentCells[fromY][fromX] ?: throw Exception("移動元(${9-fromX}${fromY+1})に駒がありません。")
                val piece = if (isPromote) {
                    when(current.first) {
                        Piece.FU -> Piece.TO; Piece.KY -> Piece.NY; Piece.KE -> Piece.NK; Piece.GI -> Piece.NG
                        Piece.KA -> Piece.UM; Piece.HI -> Piece.RY; else -> current.first
                    }
                } else current.first
                currentCells[toY][toX] = piece to isSente
                currentCells[fromY][fromX] = null
                state.addStep(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), line.trim()), captured != null)
                lastToX = toX; lastToY = toY; return@forEachIndexed
            }
            val dropMatch = dropRegex.find(line)
            if (dropMatch != null) {
                val moveNum = dropMatch.groupValues[1].toInt(); val isSente = moveNum % 2 != 0
                val toPosStr = dropMatch.groupValues[2]; val pieceSym = dropMatch.groupValues[3]
                val tx = decodeX(toPosStr[0]); val ty = decodeY(toPosStr[1]); val toX = 9 - tx; val toY = ty - 1
                val piece = Piece.entries.find { it.symbol == pieceSym || (pieceSym == "王" && it == Piece.OU) || (pieceSym == "竜" && it == Piece.RY) } ?: throw Exception("不明な駒種: $pieceSym")
                if (isSente) senteMochi.remove(piece) else goteMochi.remove(piece)
                currentCells[toY][toX] = piece to isSente
                state.addStep(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), line.trim()), false)
                lastToX = toX; lastToY = toY; return@forEachIndexed
            }
            val finishKeywords = listOf("投了", "中断", "詰み", "切れ負け", "千日手", "持将棋", "封じ手", "タイムアップ")
            if (finishKeywords.any { line.contains(it) }) return@forEachIndexed
            throw Exception("指し手を解析できませんでした。")
        } catch (e: Exception) { 
            throw Exception("${lineNum}行目: ${e.message}\n(内容: $line)") 
        }
    }
    state.currentStep = state.history.size - 1
}
