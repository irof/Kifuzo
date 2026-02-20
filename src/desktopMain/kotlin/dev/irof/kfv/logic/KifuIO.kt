package dev.irof.kfv.logic

import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.text.Charsets

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
        System.err.println("UTF-8 decoding failed for $path, trying Shift_JIS...")
    }
    return bytes.toString(Charset.forName("Shift_JIS"))
}

/**
 * 棋譜ファイルから各行を読み込みます。
 */
fun readLinesWithEncoding(path: Path): List<String> = readTextWithEncoding(path).lines()
