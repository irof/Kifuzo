package logic

import java.io.File
import java.util.*
import kotlin.text.Charsets

fun convertCsaToKifu(csaFile: File) {
    val lines = csaFile.readLines()
    val kifLines = mutableListOf<String>()
    kifLines.add("# KIF version=2.0 encoding=UTF-8")
    
    // KIF形式に合わせた駒名マップ
    val pieceMap = mapOf(
        "FU" to "歩", "KY" to "香", "KE" to "桂", "GI" to "銀", "KI" to "金",
        "KA" to "角", "HI" to "飛", "OU" to "玉",
        "TO" to "と", "NY" to "成香", "NK" to "成桂", "NG" to "成銀", "UM" to "馬", "RY" to "龍"
    )
    
    val fullWidthDigits = "１２３４５６７８９"
    val kanjiDigits = "一二三四五六七八九"
    
    var moveCount = 1
    var lastToX = -1
    var lastToY = -1
    var totalSenteSeconds = 0
    var totalGoteSeconds = 0

    for (i in lines.indices) {
        val line = lines[i].trim()
        
        // ヘッダー情報の処理
        if (line.startsWith("N+")) { kifLines.add("先手：" + line.substring(2)); continue }
        if (line.startsWith("N-")) { kifLines.add("後手：" + line.substring(2)); continue }
        if (line.startsWith("\$START_TIME:")) { kifLines.add("開始日時：" + line.substring(12)); continue }
        
        // 終了系の処理
        if (line.startsWith("%")) {
            val ending = when(line) {
                "%TORYO" -> "投了"
                "%CHUDAN" -> "中断"
                "%TIME_UP" -> "タイムアップ"
                "%SENNICHITE" -> "千日手"
                "%KACHI" -> "入玉勝ち"
                "%HIKIWAKE" -> "持将棋"
                else -> null
            }
            if (ending != null) kifLines.add("まで${moveCount - 1}手で${ending}")
            continue
        }

        // 指し手の処理
        if ((line.startsWith("+") || line.startsWith("-")) && line.length >= 7) {
            val isSente = line.startsWith("+")
            val fx = line[1] - '0'; val fy = line[2] - '0'; val tx = line[3] - '0'; val ty = line[4] - '0'
            val pieceCsa = line.substring(5, 7)
            val isPromote = line.endsWith("+")
            
            val basePieceName = pieceMap[pieceCsa] ?: pieceCsa
            // 既に成っている駒（TO, NY等）以外で、成りの指示がある場合に「成」を付与
            val pieceKif = if (isPromote && !listOf("TO", "NY", "NK", "NG", "UM", "RY").contains(pieceCsa)) {
                "${basePieceName}成"
            } else {
                basePieceName
            }
            
            val toPosStr = if (tx == lastToX && ty == lastToY) "同　" else "${fullWidthDigits[tx-1]}${kanjiDigits[ty-1]}"
            val fromStr = if (fx == 0) "打" else "($fx$fy)"
            
            // 消費時間の取得（次の行が T で始まっている場合）
            var seconds = 0
            if (i + 1 < lines.size && lines[i+1].trim().startsWith("T")) {
                val tLine = lines[i+1].trim()
                seconds = tLine.substring(1).toIntOrNull() ?: 0
            }
            
            if (isSente) totalSenteSeconds += seconds else totalGoteSeconds += seconds
            
            // 時間のフォーマット
            val moveTimeStr = String.format(Locale.US, "%2d:%02d", seconds / 60, seconds % 60)
            val totalSec = if (isSente) totalSenteSeconds else totalGoteSeconds
            val totalTimeStr = String.format(Locale.US, "%02d:%02d:%02d", totalSec / 3600, (totalSec % 3600) / 60, totalSec % 60)
            
            // KIF形式: "   1 ７六歩(77)   ( 0:01/00:00:01)"
            kifLines.add(String.format(Locale.US, "%4d %s%s%-7s   ( %s/%s)", moveCount++, toPosStr, pieceKif, fromStr, moveTimeStr, totalTimeStr))
            
            lastToX = tx; lastToY = ty
        }
    }
    
    val kifuFile = File(csaFile.parent, csaFile.nameWithoutExtension + ".kifu")
    kifuFile.writeText(kifLines.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
}
