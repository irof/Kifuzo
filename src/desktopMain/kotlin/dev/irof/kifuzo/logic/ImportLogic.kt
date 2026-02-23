package dev.irof.kifuzo.logic

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.copyTo
import kotlin.io.path.deleteExisting
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * 指定されたフォルダから特定のテキストファイルを検出し、
 * 棋譜の内容に基づいてリネームしてカレントディレクトリへ移動します。
 */
private const val CSA_HEADER_SCAN_RANGE = 10

fun importShogiQuestFiles(sourceDir: Path, targetDir: Path): Int {
    if (!sourceDir.exists() || !targetDir.exists()) return 0

    val txtFiles = sourceDir.listDirectoryEntries("*.txt").filter { it.isRegularFile() }

    var count = 0
    txtFiles.forEach { file ->
        try {
            val lines = readLinesWithEncoding(file)
            val newFileName = calculateImportTarget(lines, file.getLastModifiedTime().toMillis()) ?: return@forEach

            val targetFile = targetDir / newFileName

            // 移動（コピーして削除）
            file.copyTo(targetFile, overwrite = true)
            file.deleteExisting()
            count++
        } catch (e: IOException) {
            logger.warn(e) { "IO error importing ${file.name}" }
        }
    }
    return count
}

private val importDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    .withZone(ZoneId.systemDefault())

private val nameCleanupRegex = Regex("""\(.*?\)""")

/**
 * 棋譜の内容と更新日時から、インポート後のファイル名を算出します。
 * インポート対象でない場合は null を返します。
 */
fun calculateImportTarget(lines: List<String>, lastModifiedMillis: Long): String? {
    if (lines.isEmpty()) return null

    // CSA判定: 先頭10行以内に N+ または N- があるか
    val headerLines = lines.take(CSA_HEADER_SCAN_RANGE)
    val hasCsaMarker = headerLines.any { it.startsWith("N+") || it.startsWith("N-") }
    if (!hasCsaMarker) return null

    var sente = "unknown"
    var gote = "unknown"

    // 名前抽出
    lines.forEach { line ->
        if (line.startsWith("N+")) {
            sente = line.substring(2).replace(nameCleanupRegex, "").trim()
        } else if (line.startsWith("N-")) {
            gote = line.substring(2).replace(nameCleanupRegex, "").trim()
        }
    }

    // タイムスタンプから日付取得
    val dateStr = importDateFormatter.format(Instant.ofEpochMilli(lastModifiedMillis))

    // 新しいファイル名: {YYYYMMDD}-{先手}-{後手}.csa
    return "$dateStr-$sente-$gote.csa"
}
