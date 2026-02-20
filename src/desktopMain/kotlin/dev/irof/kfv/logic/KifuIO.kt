package dev.irof.kfv.logic

import java.io.File
import java.nio.charset.Charset
import kotlin.text.Charsets

/**
 * 棋譜ファイルからテキストを読み込みます。
 * UTF-8 と Shift_JIS を自動判別します。
 */
fun readTextWithEncoding(file: File): String {
    val bytes = file.readBytes()
    try {
        val text = bytes.toString(Charsets.UTF_8)
        if (!text.contains("\uFFFD")) return text
    } catch (e: Exception) {
        System.err.println("UTF-8 decoding failed for ${file.name}, trying Shift_JIS...")
    }
    // デフォルトで Shift_JIS (Windows-31J) を試す
    return bytes.toString(Charset.forName("Shift_JIS"))
}

/**
 * 棋譜ファイルから各行を読み込みます。
 */
fun readLinesWithEncoding(file: File): List<String> {
    return readTextWithEncoding(file).lines()
}
