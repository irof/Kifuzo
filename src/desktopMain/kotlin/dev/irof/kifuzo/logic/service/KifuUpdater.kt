package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.parser.KifuFormat
import dev.irof.kifuzo.logic.parser.KifuFormatHandler
import dev.irof.kifuzo.logic.parser.csa.CsaParser
import dev.irof.kifuzo.logic.parser.kif.KifParser
import java.nio.file.Path
import kotlin.text.Charsets

/**
 * 棋譜ファイルの内容を更新（ヘッダー変更、終局結果の追加など）するためのユーティリティ。
 */
fun updateKifuHeader(path: Path, event: String, startTime: String) {
    val handler = getHandlerForPath(path) ?: return
    val lines = readLinesWithEncoding(path).toMutableList()

    handler.formatHeader(lines, event, startTime)

    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}

/**
 * 棋譜ファイルに終局結果を書き込みます。
 */
fun updateKifuResult(path: Path, result: String) {
    if (result.isEmpty()) return
    val handler = getHandlerForPath(path) ?: return
    val lines = readLinesWithEncoding(path).toMutableList()

    handler.formatResult(lines, result)

    java.nio.file.Files.write(path, lines.joinToString("\n").toByteArray(Charsets.UTF_8))
}

private fun getHandlerForPath(path: Path): KifuFormatHandler? {
    val format = KifuFormat.fromPath(path)
    return when (format) {
        KifuFormat.KIF -> KifParser()
        KifuFormat.CSA -> CsaParser()
        else -> null
    }
}
