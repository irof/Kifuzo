package logic

import java.io.File
import kotlin.text.Charsets

fun convertCsaToKifu(csaFile: File) {
    val lines = csaFile.readLines()
    val kifLines = mutableListOf<String>()
    kifLines.add("# KIF version=2.0 encoding=UTF-8")
    val pieceMap = mapOf(
        "FU" to "歩", "KY" to "香", "KE" to "桂", "GI" to "銀", "KI" to "金",
        "KA" to "角", "HI" to "飛", "OU" to "玉",
        "TO" to "歩", "NY" to "香", "NK" to "桂", "NG" to "銀", "UM" to "角", "RY" to "飛"
    )
    val promotedPieces = setOf("TO", "NY", "NK", "NG", "UM", "RY")
    val fullWidthDigits = "１２３４５６７８９"; val kanjiDigits = "一二三四五六七八九"
    var moveCount = 1; var lastToX = -1; var lastToY = -1
    for (line in lines) {
        if (line.startsWith("N+")) kifLines.add("先手：" + line.substring(2))
        if (line.startsWith("N-")) kifLines.add("後手：" + line.substring(2))
        if (line.startsWith("\$START_TIME:")) kifLines.add("開始日時：" + line.substring(12))
        if ((line.startsWith("+") || line.startsWith("-")) && line.length >= 7) {
            val fx = line[1] - '0'; val fy = line[2] - '0'; val tx = line[3] - '0'; val ty = line[4] - '0'
            val pieceCsa = line.substring(5, 7); val isPromote = line.endsWith("+")
            val basePieceName = pieceMap[pieceCsa] ?: pieceCsa
            val pieceKif = if (isPromote || promotedPieces.contains(pieceCsa)) "${basePieceName}成" else basePieceName
            val toPosStr = if (tx == lastToX && ty == lastToY) "同　" else "${fullWidthDigits[tx-1]}${kanjiDigits[ty-1]}"
            val fromStr = if (fx == 0) "打" else "($fx$fy)"
            kifLines.add(String.format("%4d %s%s%s", moveCount++, toPosStr, pieceKif, fromStr))
            lastToX = tx; lastToY = ty
        }
    }
    val kifuFile = File(csaFile.parent, csaFile.nameWithoutExtension + ".kifu")
    kifuFile.writeText(kifLines.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
}
