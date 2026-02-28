package dev.irof.kifuzo

import java.io.InputStream

/**
 * テストリソースファイルを読み込み、文字列として返します。
 * ファイルパスは src/desktopTest/resources からの相対パスを指定します。
 */
fun readResource(path: String): String {
    val inputStream: InputStream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
        ?: throw IllegalArgumentException("Resource not found: $path")
    return inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
