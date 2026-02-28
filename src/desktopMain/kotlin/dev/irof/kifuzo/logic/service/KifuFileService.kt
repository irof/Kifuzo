package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

interface KifuFileService {
    fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path>
    fun renameFile(path: Path, newName: String): Path?
    fun generateProposedName(path: Path, info: KifuInfo, template: String): String?
    fun generateProposedNameFromText(text: String, info: KifuInfo, template: String): String?
    fun updateResult(path: Path, result: String)
    fun updateHeader(path: Path, event: String, startTime: String)
}

class KifuFileServiceImpl : KifuFileService {
    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss")
        private const val SAMPLE_LINES_FOR_EXTENSION_DETECTION = 20
    }

    override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> {
        val entries = directory.listDirectoryEntries()
        return when (sortOption) {
            FileSortOption.NAME -> entries.sortedWith(compareBy({ !it.isDirectory() }, { it.name.lowercase() }))
            FileSortOption.LAST_MODIFIED -> entries.sortedWith(
                compareBy<Path> { !it.isDirectory() }
                    .thenByDescending {
                        try {
                            Files.getLastModifiedTime(it).toInstant()
                        } catch (e: IOException) {
                            logger.debug(e) { "Failed to get last modified time for $it" }
                            java.time.Instant.MIN
                        }
                    },
            )
        }
    }

    override fun renameFile(path: Path, newName: String): Path? {
        val targetPath = path.parent?.resolve(newName)
        return when {
            targetPath == null -> null
            path == targetPath -> path
            else -> try {
                Files.move(path, targetPath)
                targetPath
            } catch (e: IOException) {
                logger.error(e) { "Failed to rename file from $path to $targetPath" }
                null
            }
        }
    }

    override fun generateProposedName(path: Path, info: KifuInfo, template: String): String? {
        if (info.isError) return null

        val dt = if (info.startTime.isNotEmpty()) {
            parseStartTime(info.startTime)
        } else {
            try {
                LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault())
            } catch (e: IOException) {
                logger.debug(e) { "Failed to get last modified time for $path" }
                LocalDateTime.now()
            }
        }

        return generateProposedNameFromInfo(info, template, path.extension.ifEmpty { "kifu" }, dt)
    }

    override fun generateProposedNameFromText(text: String, info: KifuInfo, template: String): String? {
        if (info.isError) return null

        val dt = if (info.startTime.isNotEmpty()) {
            parseStartTime(info.startTime)
        } else {
            LocalDateTime.now()
        }

        // 形式を判定して拡張子を決める
        val lines = text.lines()
        val sample = lines.take(SAMPLE_LINES_FOR_EXTENSION_DETECTION)
        val isCsa = sample.any { line ->
            line.startsWith("V") || line.startsWith("N+") || line.startsWith("N-") ||
                line.startsWith("$") ||
                Regex("""^P[1-9+ -]""").containsMatchIn(line) ||
                Regex("""^[+-]\d{4}[A-Z]{2}""").containsMatchIn(line)
        }
        val extension = if (isCsa) "csa" else "kifu"
        return generateProposedNameFromInfo(info, template, extension, dt)
    }

    private fun generateProposedNameFromInfo(info: KifuInfo, template: String, extension: String, dt: LocalDateTime): String? {
        val yyyymmdd = dt.format(DATE_FORMATTER)
        val hhmmss = dt.format(TIME_FORMATTER)

        return template
            .replace("{YYYYMMDD}", yyyymmdd)
            .replace("{HHMMSS}", hhmmss)
            .replace("{Event}", sanitizeFilename(info.event).ifEmpty { "unknown_event" })
            .replace("{Sente}", sanitizeFilename(info.senteName).ifEmpty { "unknown" })
            .replace("{Gote}", sanitizeFilename(info.goteName).ifEmpty { "unknown" })
            .let { "$it.$extension" }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun parseStartTime(startTime: String): LocalDateTime = try {
        // "2026/02/21 12:00:00" または "2026/02/21" などの形式を想定
        val datePart = startTime.substringBefore(" ").replace("/", "-")
        val timePart = startTime.substringAfter(" ", "00:00:00")
        LocalDateTime.parse("${datePart}T$timePart")
    } catch (e: java.time.format.DateTimeParseException) {
        logger.debug(e) { "Failed to parse startTime: $startTime" }
        LocalDateTime.now()
    } catch (e: IndexOutOfBoundsException) {
        logger.debug(e) { "Invalid startTime format: $startTime" }
        LocalDateTime.now()
    }

    private fun sanitizeFilename(name: String): String = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")

    override fun updateResult(path: Path, result: String) {
        updateKifuResult(path, result)
    }

    override fun updateHeader(path: Path, event: String, startTime: String) {
        updateKifuHeader(path, event, startTime)
    }
}
