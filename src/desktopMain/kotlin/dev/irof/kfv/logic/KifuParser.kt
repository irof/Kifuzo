package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import java.io.File
import java.nio.charset.Charset
import kotlin.Exception

/**
 * 棋譜の要約情報
 */
data class KifuInfo(
    val file: File,
    val senteName: String = "",
    val goteName: String = "",
    val senkei: String = ""
)

fun getInitialCells(): Array<Array<Pair<Piece, PieceColor>?>> {
    val cells = Array(9) { arrayOfNulls<Pair<Piece, PieceColor>>(9) }
    // 初期配置の駒 (PieceColor.White = 後手, PieceColor.Black = 先手)
    cells[0][0] = Piece.KY to PieceColor.White; cells[0][1] = Piece.KE to PieceColor.White; cells[0][2] = Piece.GI to PieceColor.White
    cells[0][3] = Piece.KI to PieceColor.White; cells[0][4] = Piece.OU to PieceColor.White; cells[0][5] = Piece.KI to PieceColor.White
    cells[0][6] = Piece.GI to PieceColor.White; cells[0][7] = Piece.KE to PieceColor.White; cells[0][8] = Piece.KY to PieceColor.White
    cells[1][1] = Piece.HI to PieceColor.White; cells[1][7] = Piece.KA to PieceColor.White
    for (i in 0..8) cells[2][i] = Piece.FU to PieceColor.White
    
    cells[8][0] = Piece.KY to PieceColor.Black; cells[8][1] = Piece.KE to PieceColor.Black; cells[8][2] = Piece.GI to PieceColor.Black
    cells[8][3] = Piece.KI to PieceColor.Black; cells[8][4] = Piece.OU to PieceColor.Black; cells[8][5] = Piece.KI to PieceColor.Black
    cells[8][6] = Piece.GI to PieceColor.Black; cells[8][7] = Piece.KE to PieceColor.Black; cells[8][8] = Piece.KY to PieceColor.Black
    cells[7][1] = Piece.KA to PieceColor.Black; cells[7][7] = Piece.HI to PieceColor.Black
    for (i in 0..8) cells[6][i] = Piece.FU to PieceColor.Black
    return cells
}

fun scanKifuInfo(file: File): KifuInfo {
    var sente = ""
    var gote = ""
    var senkei = ""
    try {
        val lines = readLinesWithEncoding(file)
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) sente = trimmed.substringAfter("：").trim()
            if (trimmed.startsWith("後手：")) gote = trimmed.substringAfter("：").trim()
            if (trimmed.startsWith("戦型：")) senkei = trimmed.substringAfter("：").trim()
            if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) break
        }
    } catch (e: Exception) {}
    return KifuInfo(file, sente, gote, senkei)
}

fun parseKifu(file: File, state: ShogiBoardState) {
    val initialCells = getInitialCells()
    state.reset(initialCells)
    // Deep copy for currentCells
    var currentCells = Array(9) { y -> Array(9) { x -> initialCells[y][x] } }
    var senteMochi = mutableListOf<Piece>()
    var goteMochi = mutableListOf<Piece>()
    
    val lines = readLinesWithEncoding(file)
    val moveRegex = Regex("""^\s*(\d+)\s+([^\s(]{2}|同\s*)([^\s(]+)\(([1-9]{2})\).*""")
    val dropRegex = Regex("""^\s*(\d+)\s+([^\s(]{2})([^\s(]+?)打.*""")
    
    var lastTo: Square? = null
    var isVariationSection = false
    
    fun decodeX(c: Char): Int { val idx = "１２３４５６７８９123456789".indexOf(c); return if (idx == -1) -1 else (idx % 9) + 1 }
    fun decodeY(c: Char): Int { val idx = "一二三四五六七八九１２３４５６７８９123456789".indexOf(c); return if (idx == -1) -1 else (idx % 9) + 1 }
    
    lines.forEachIndexed { index, line ->
        val lineNum = index + 1
        try {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEachIndexed
            if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) { state.senteName = trimmed.substringAfter("：").trim(); return@forEachIndexed }
            if (trimmed.startsWith("後手：")) { state.goteName = trimmed.substringAfter("：").trim(); return@forEachIndexed }
            if (trimmed.startsWith("*") || trimmed.startsWith("#") || trimmed.startsWith("&")) return@forEachIndexed
            if (trimmed.startsWith("変化：") || trimmed.startsWith("変化:")) { isVariationSection = true }
            if (isVariationSection) return@forEachIndexed
            if (!Regex("""^\s*\d+\s+.*""").matches(line)) return@forEachIndexed
            
            val moveMatch = moveRegex.find(line)
            if (moveMatch != null) {
                val moveNum = moveMatch.groupValues[1].toInt()
                val isSente = moveNum % 2 != 0
                val turnColor = if (isSente) PieceColor.Black else PieceColor.White
                
                val toPosStr = moveMatch.groupValues[2].trim()
                val pieceName = moveMatch.groupValues[3].trim()
                val fromPosStr = moveMatch.groupValues[4]
                
                val isPromote = pieceName.contains("成") || pieceName == "竜" || pieceName == "馬" || pieceName == "龍" || pieceName == "圭" || pieceName == "杏" || pieceName == "全"
                
                val toSquare: Square
                if (toPosStr.startsWith("同")) {
                    toSquare = lastTo ?: throw Exception("同の移動先が不明です")
                } else {
                    val tx = decodeX(toPosStr[0]); val ty = decodeY(toPosStr[1])
                    if (tx == -1 || ty == -1) return@forEachIndexed
                    toSquare = Square(tx, ty)
                }
                
                val fromFile = fromPosStr[0] - '0'
                val fromRank = fromPosStr[1] - '0'
                val fromSquare = Square(fromFile, fromRank)
                
                val captured = currentCells[toSquare.yIndex][toSquare.xIndex]
                if (captured != null) { 
                    if (isSente) senteMochi.add(captured.first.toBase()) 
                    else goteMochi.add(captured.first.toBase()) 
                }
                
                val current = currentCells[fromSquare.yIndex][fromSquare.xIndex] ?: throw Exception("移動元($fromSquare)に駒がありません。")
                val piece = if (isPromote) {
                    when(current.first) {
                        Piece.FU -> Piece.TO; Piece.KY -> Piece.NY; Piece.KE -> Piece.NK; Piece.GI -> Piece.NG
                        Piece.KA -> Piece.UM; Piece.HI -> Piece.RY; else -> current.first
                    }
                } else current.first
                
                currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                currentCells[fromSquare.yIndex][fromSquare.xIndex] = null
                
                state.addStep(
                    BoardSnapshot(
                        Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, 
                        senteMochi.toList(), goteMochi.toList(), line.trim(), 
                        lastFrom = fromSquare, lastTo = toSquare
                    ), 
                    captured != null
                )
                lastTo = toSquare; return@forEachIndexed
            }
            
            val dropMatch = dropRegex.find(line)
            if (dropMatch != null) {
                val moveNum = dropMatch.groupValues[1].toInt()
                val isSente = moveNum % 2 != 0
                val turnColor = if (isSente) PieceColor.Black else PieceColor.White
                
                val toPosStr = dropMatch.groupValues[2]
                val pieceSym = dropMatch.groupValues[3].substring(0, 1)
                val tx = decodeX(toPosStr[0]); val ty = decodeY(toPosStr[1])
                val toSquare = Square(tx, ty)
                
                val piece = Piece.entries.find { it.symbol == pieceSym || (pieceSym == "王" && it == Piece.OU) || (pieceSym == "玉" && it == Piece.OU) || (pieceSym == "竜" && it == Piece.RY) || (pieceSym == "龍" && it == Piece.RY) || (pieceSym == "馬" && it == Piece.UM) } ?: throw Exception("不明な駒種: $pieceSym")
                
                if (isSente) senteMochi.remove(piece) else goteMochi.remove(piece)
                currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                
                state.addStep(
                    BoardSnapshot(
                        Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, 
                        senteMochi.toList(), goteMochi.toList(), line.trim(), 
                        lastFrom = null, lastTo = toSquare
                    ), 
                    false
                )
                lastTo = toSquare; return@forEachIndexed
            }
            val finishKeywords = listOf("投了", "中断", "詰み", "切れ負け", "千日手", "持将棋", "封じ手", "タイムアップ", "反則負け")
            if (finishKeywords.any { line.contains(it) }) return@forEachIndexed
        } catch (e: Exception) { throw Exception("${lineNum}行目: ${e.message}\n(内容: $line)") }
    }
    state.currentStep = if (state.firstContactStep != -1) state.firstContactStep else state.history.size - 1
}

fun updateKifuSenkei(file: File, senkei: String) {
    if (senkei.isEmpty()) return
    val lines = readLinesWithEncoding(file).toMutableList()
    var senkeiLineIndex = -1
    var headerEndIndex = 0
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.startsWith("戦型：")) { senkeiLineIndex = i; break }
        if (Regex("""^\s*\d+\s+.*""").matches(line)) { headerEndIndex = i; break }
    }
    if (senkeiLineIndex != -1) lines[senkeiLineIndex] = "戦型：$senkei"
    else lines.add(headerEndIndex, "戦型：$senkei")
    file.writeText(lines.joinToString("\n"), Charsets.UTF_8)
}
