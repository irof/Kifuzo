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
        private const val DEFAULT_VALUE = "unknown"
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

        val lines = text.lines()
        val extension = determineExtension(lines)
        return generateProposedNameFromInfo(info, template, extension, dt)
    }

    private fun determineExtension(lines: List<String>): String {
        val sample = lines.take(SAMPLE_LINES_FOR_EXTENSION_DETECTION)
        val isKif = sample.any { line ->
            line.startsWith("開始日時") || line.startsWith("場所") || line.startsWith("手合割") ||
                line.startsWith("先手") || line.startsWith("後手") || line.startsWith("指し手") ||
                line.startsWith("対局者")
        }
        val isCsa = !isKif && sample.any { line ->
            line.startsWith("V") || line.startsWith("N+") || line.startsWith("N-") ||
                line.startsWith("$") ||
                Regex("""^P[1-9+ -]""").containsMatchIn(line) ||
                Regex("""^[+-]\d{4}[A-Z]{2}""").containsMatchIn(line)
        }
        return if (isCsa) "csa" else "kifu"
    }

    private fun generateProposedNameFromInfo(info: KifuInfo, template: String, extension: String, dt: LocalDateTime): String? {
        val replacements = mapOf(
            "{開始日の年月日}" to dt.format(DATE_FORMATTER),
            "{開始日の時分秒}" to dt.format(TIME_FORMATTER),
            "{棋戦名}" to sanitizeFilename(info.event).ifEmpty { DEFAULT_VALUE },
            "{先手}" to sanitizeFilename(info.senteName).ifEmpty { DEFAULT_VALUE },
            "{後手}" to sanitizeFilename(info.goteName).ifEmpty { DEFAULT_VALUE },
        )

        var resultName = template
        replacements.forEach { (key, value) ->
            resultName = resultName.replace(key, value)
        }

        return "$resultName.$extension"
    }

    @Suppress("TooGenericExceptionCaught")
    private fun parseStartTime(startTime: String): LocalDateTime = try {
        val datePart = startTime.substringBefore(" ").replace("/", "-")
        val timePart = startTime.substringAfter(" ", "00:00:00")
        LocalDateTime.parse("${datePart}T$timePart")
    } catch (e: Exception) {
        logger.debug(e) { "Failed to parse startTime: $startTime" }
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
