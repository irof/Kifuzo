package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.text.Charsets

/**
 * 標準の平手初期配置を取得します。
 */
fun getInitialCells(): Array<Array<Pair<Piece, PieceColor>?>> {
    val cells = Array(9) { arrayOfNulls<Pair<Piece, PieceColor>>(9) }
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

fun scanKifuInfo(path: Path): KifuInfo {
    var sente = ""
    var gote = ""
    var senkei = ""
    try {
        val lines = readLinesWithEncoding(path)
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) sente = trimmed.substringAfter("：").trim()
            if (trimmed.startsWith("後手：")) gote = trimmed.substringAfter("：").trim()
            if (trimmed.startsWith("戦型：")) senkei = trimmed.substringAfter("：").trim()
            if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) break
        }
    } catch (e: Exception) {
        System.err.println("Failed to scan header for ${path.name}: ${e.message}")
    }
    return KifuInfo(path, sente, gote, senkei)
}

fun parseKifu(path: Path, state: ShogiBoardState) {
    val lines = readLinesWithEncoding(path)
    
    var currentCells = Array(9) { arrayOfNulls<Pair<Piece, PieceColor>>(9) }
    var senteMochi = mutableListOf<Piece>()
    var goteMochi = mutableListOf<Piece>()
    var isStandardStart = true
    
    val pieceSymbolMap = Piece.entries.associateBy { it.symbol }
    var boardY = 0

    var moveStartIndex = -1
    for (i in lines.indices) {
        val line = lines[i] // 解析用に trim() しない状態も考慮
        val trimmed = line.trim()
        
        if (trimmed.startsWith("先手：") || trimmed.startsWith("対局者：")) state.senteName = trimmed.substringAfter("：").trim()
        if (trimmed.startsWith("後手：")) state.goteName = trimmed.substringAfter("：").trim()
        
        // 局面図行の判定: | で始まり、18文字以上のデータがあり、その後に | がある
        if (trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2) {
            isStandardStart = false
            val content = trimmed.substringAfter("|").substringBeforeLast("|")
            // content は " ・ ・ ・" や "v歩 ・" など 18文字以上あるはず
            for (x in 0..8) {
                if (x * 2 + 1 >= content.length) break
                val pStr = content.substring(x * 2, x * 2 + 2)
                
                val color = if (pStr.startsWith('v')) PieceColor.White else PieceColor.Black
                val pieceName = pStr.substring(1).trim()
                
                if (pieceName.isNotEmpty() && pieceName != "・" && pieceName != "　") {
                    val piece = pieceSymbolMap[pieceName] 
                        ?: if (pieceName == "王") Piece.OU 
                        else if (pieceName == "玉") Piece.OU 
                        else if (pieceName == "竜") Piece.RY 
                        else if (pieceName == "馬") Piece.UM
                        else null
                    
                    if (piece != null) {
                        currentCells[boardY][x] = piece to color
                    }
                }
            }
            boardY++
        }
        
        if (trimmed.startsWith("先手持駒：") || trimmed.startsWith("下手持駒：")) {
            isStandardStart = false
            senteMochi.addAll(parseMochigoma(trimmed.substringAfter("：")))
        }
        if (trimmed.startsWith("後手持駒：") || trimmed.startsWith("上手持駒：")) {
            isStandardStart = false
            goteMochi.addAll(parseMochigoma(trimmed.substringAfter("：")))
        }

        if (Regex("""^\s*\d+\s+.*""").matches(trimmed)) {
            moveStartIndex = i
            break
        }
    }

    if (isStandardStart) {
        currentCells = getInitialCells()
    }
    
    // 初期状態を history にセット
    val initialSnapshot = BoardSnapshot(
        Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, 
        senteMochi.toList(), 
        goteMochi.toList(), 
        "開始局面"
    )
    state.history = listOf(initialSnapshot)
    state.currentStep = 0

    if (moveStartIndex == -1) return

    // 指し手の解析（既存ロジックを微調整して継続）
    val moveRegex = Regex("""^\s*(\d+)\s+([^\s(]{2}|同\s*)([^\s(]+)\(([1-9]{2})\).*""")
    val dropRegex = Regex("""^\s*(\d+)\s+([^\s(]{2})([^\s(]+?)打.*""")
    var lastTo: Square? = null
    var isVariationSection = false
    
    fun decodeX(c: Char): Int { val idx = "１２３４５６７８９123456789".indexOf(c); return if (idx == -1) -1 else (idx % 9) + 1 }
    fun decodeY(c: Char): Int { val idx = "一二三四五六七八九１２３４５６７８９1234567８９".indexOf(c); return if (idx == -1) -1 else (idx % 9) + 1 }
    
    for (i in moveStartIndex until lines.size) {
        val line = lines[i].trim()
        if (line.isEmpty() || line.startsWith("*") || line.startsWith("#") || line.startsWith("&")) continue
        if (line.startsWith("変化：") || line.startsWith("変化:")) { isVariationSection = true }
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
                }
                
                val current = currentCells[fromSquare.yIndex][fromSquare.xIndex] ?: throw Exception("移動元($fromSquare)に駒がありません。")
                val piece = if (isPromote) when(current.first) { Piece.FU -> Piece.TO; Piece.KY -> Piece.NY; Piece.KE -> Piece.NK; Piece.GI -> Piece.NG; Piece.KA -> Piece.UM; Piece.HI -> Piece.RY; else -> current.first } else current.first
                
                currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                currentCells[fromSquare.yIndex][fromSquare.xIndex] = null
                
                state.addStep(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = fromSquare, lastTo = toSquare), captured != null)
                lastTo = toSquare
                continue
            }
            
            val dropMatch = dropRegex.find(line)
            if (dropMatch != null) {
                val moveNum = dropMatch.groupValues[1].toInt()
                val turnColor = if (moveNum % 2 != 0) PieceColor.Black else PieceColor.White
                val toPosStr = dropMatch.groupValues[2]; val pieceSym = dropMatch.groupValues[3].substring(0, 1)
                val toSquare = Square(decodeX(toPosStr[0]), decodeY(toPosStr[1]))
                val piece = Piece.entries.find { it.symbol == pieceSym || (pieceSym == "王" && it == Piece.OU) || (pieceSym == "玉" && it == Piece.OU) || (pieceSym == "竜" && it == Piece.RY) || (pieceSym == "龍" && it == Piece.RY) || (pieceSym == "馬" && it == Piece.UM) } ?: throw Exception("不明な駒種: $pieceSym")
                
                if (turnColor == PieceColor.Black) senteMochi.remove(piece) else goteMochi.remove(piece)
                currentCells[toSquare.yIndex][toSquare.xIndex] = piece to turnColor
                
                state.addStep(BoardSnapshot(Array(9) { y -> Array(9) { x -> currentCells[y][x] } }, senteMochi.toList(), goteMochi.toList(), line, lastFrom = null, lastTo = toSquare), false)
                lastTo = toSquare
                continue
            }
        } catch (e: Exception) {
            throw Exception("${i + 1}行目: ${e.message}\n(内容: $line)")
        }
    }
    // 初期表示の決定
    state.currentStep = if (!isStandardStart) {
        // 指定局面の場合は開始を表示
        0
    } else if (state.firstContactStep != -1) {
        // 平手開始の場合は衝突を表示
        state.firstContactStep
    } else {
        // それ以外は終局を表示
        state.history.size - 1
    }
}

/**
 * 持ち駒文字列（例: "飛二 角 銀三"）を解析して Piece のリストを返します。
 */
private fun parseMochigoma(text: String): List<Piece> {
    val t = text.trim()
    if (t == "なし" || t.isEmpty()) return emptyList()
    val list = mutableListOf<Piece>()
    val kanjiDigits = "一二三四五六七八九"
    
    // 全角・半角スペース両方に対応
    t.split(Regex("""[\s　]+""")).forEach { part ->
        if (part.isEmpty()) return@forEach
        val pieceName = part.substring(0, 1)
        val countStr = part.substring(1)
        val count = if (countStr.isEmpty()) 1 else {
            val idx = kanjiDigits.indexOf(countStr)
            if (idx != -1) idx + 1 else countStr.toIntOrNull() ?: 1
        }
        val piece = Piece.entries.find { it.symbol == pieceName || (pieceName == "王" && it == Piece.OU) || (pieceName == "玉" && it == Piece.OU) || (pieceName == "竜" && it == Piece.RY) || (pieceName == "龍" && it == Piece.RY) || (pieceName == "馬" && it == Piece.UM) }
        if (piece != null) repeat(count) { list.add(piece) }
    }
    return list
}

fun updateKifuSenkei(path: Path, senkei: String) {
    if (senkei.isEmpty()) return
    val lines = readLinesWithEncoding(path).toMutableList()
    var senkeiLineIndex = -1
    var headerEndIndex = 0
    for (i in lines.indices) {
        val line = lines[i].trim()
        if (line.startsWith("戦型：")) { senkeiLineIndex = i; break }
        if (Regex("""^\s*\d+\s+.*""").matches(line)) { headerEndIndex = i; break }
    }
    if (senkeiLineIndex != -1) lines[senkeiLineIndex] = "戦型：$senkei"
    else lines.add(headerEndIndex, "戦型：$senkei")
    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}
