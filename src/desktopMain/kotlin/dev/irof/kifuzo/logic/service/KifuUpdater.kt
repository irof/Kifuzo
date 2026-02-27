package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.parseCsa
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.parser.parseKifu
import dev.irof.kifuzo.logic.parser.scanKifuInfo
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.text.Charsets

/**
 * 棋譜ファイルの内容を更新（ヘッダー変更、終局結果の追加など）するためのユーティリティ。
 */
fun updateKifuHeader(path: Path, event: String, startTime: String) {
    val lines = readLinesWithEncoding(path).toMutableList()
    val extension = path.name.substringAfterLast(".").lowercase()

    when (extension) {
        "kifu", "kif" -> updateKifHeader(lines, event, startTime)
        "csa" -> updateCsaHeader(lines, event, startTime)
        else -> return
    }

    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}

private fun updateKifHeader(lines: MutableList<String>, event: String, startTime: String) {
    updateOrAddKifLine(lines, "棋戦：", event)
    updateOrAddKifLine(lines, "開始日時：", startTime)
}

private fun updateOrAddKifLine(lines: MutableList<String>, prefix: String, value: String) {
    val index = lines.indexOfFirst { it.startsWith(prefix) }
    if (index != -1) {
        lines[index] = prefix + value
    } else {
        val insertIndex = calculateKifHeaderInsertIndex(lines)
        lines.add(insertIndex, prefix + value)
    }
}

private fun calculateKifHeaderInsertIndex(lines: List<String>): Int {
    val headerIndex = lines.indexOfFirst { it.startsWith("手数") && it.contains("指手") }
    val moveIndex = lines.indexOfFirst { Regex("""^\s*\d+\s+.*""").matches(it) }

    return when {
        headerIndex != -1 && moveIndex != -1 -> minOf(headerIndex, moveIndex)
        headerIndex != -1 -> headerIndex
        moveIndex != -1 -> moveIndex
        else -> 0
    }
}

private fun updateCsaHeader(lines: MutableList<String>, event: String, startTime: String) {
    updateOrAddCsaLine(lines, "\$EVENT:", event)
    updateOrAddCsaLine(lines, "\$START_TIME:", startTime)
}

private fun updateOrAddCsaLine(lines: MutableList<String>, prefix: String, value: String) {
    val index = lines.indexOfFirst { it.startsWith(prefix) }
    if (index != -1) {
        lines[index] = prefix + value
    } else {
        val insertIndex = lines.indexOfFirst { it.startsWith("+") || it.startsWith("-") || it.startsWith("P") }
        if (insertIndex != -1) {
            lines.add(insertIndex, prefix + value)
        } else {
            lines.add(0, prefix + value)
        }
    }
}

fun updateKifuResult(path: Path, result: String) {
    if (result.isEmpty()) return
    val lines = readLinesWithEncoding(path).toMutableList()

    val lastActualMoveNum = lines.asSequence()
        .mapNotNull { line ->
            Regex("""^\s*(\d+)\s+.*""").find(line)?.groupValues?.get(1)?.toIntOrNull()?.let { num ->
                if (!dev.irof.kifuzo.models.GameResult.isResultLine(line)) num else null
            }
        }.maxOrNull() ?: 0

    val nextMoveNum = lastActualMoveNum + 1
    val resultLine = String.format(java.util.Locale.US, "%4d %s", nextMoveNum, result)

    val existingResultIndex = lines.indexOfLast { dev.irof.kifuzo.models.GameResult.isResultLine(it) }

    if (existingResultIndex != -1) {
        lines[existingResultIndex] = resultLine
    } else {
        lines.add(resultLine)
    }
    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets_UTF_8))
}

private val Charsets_UTF_8 = Charsets.UTF_8
