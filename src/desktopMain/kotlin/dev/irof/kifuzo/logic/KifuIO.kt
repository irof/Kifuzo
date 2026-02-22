package dev.irof.kifuzo.logic

import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.text.Charsets

private val logger = KotlinLogging.logger {}

/**
 * 棋譜ファイルからテキストを読み込みます。
 * UTF-8 と Shift_JIS を自動判別します。
 */
fun readTextWithEncoding(path: Path): String {
    val bytes = path.readBytes()
    try {
        val text = bytes.toString(Charsets.UTF_8)
        if (!text.contains("\uFFFD")) return text
    } catch (e: Exception) {
        logger.debug { "UTF-8 decoding failed for $path, trying Shift_JIS..." }
    }
    return bytes.toString(Charset.forName("Shift_JIS"))
}

/**
 * 棋譜ファイルから各行を読み込みます。
 */
fun readLinesWithEncoding(path: Path): List<String> {
    val text = readTextWithEncoding(path)
    if (text.isEmpty()) return emptyList()
    val lines = text.lines()
    // String.lines() が末尾の改行により空文字列を返す場合、それを除去する
    return if (lines.isNotEmpty() && lines.last().isEmpty()) {
        lines.dropLast(1)
    } else {
        lines
    }
}
