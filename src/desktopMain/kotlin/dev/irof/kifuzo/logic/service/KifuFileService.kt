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
import dev.irof.kifuzo.models.FileSortOption
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

interface KifuFileService {
    fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path>
    fun renameFile(path: Path, newName: String): Path?
    fun generateProposedName(path: Path, template: String): String?
    fun updateResult(path: Path, result: String)
    fun updateHeader(path: Path, event: String, startTime: String)
}

class KifuFileServiceImpl : KifuFileService {
    companion object {
        private const val DATE_STRING_LENGTH = 8
    }

    override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> = try {
        val entries = directory.listDirectoryEntries()
        when (sortOption) {
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
    } catch (e: IOException) {
        logger.error(e) { "Failed to scan directory: $directory" }
        emptyList()
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

    override fun generateProposedName(path: Path, template: String): String? {
        val info = scanKifuInfo(path)
        if (info.isError) return null

        // "2026/02/21 12:00:00" -> "20260221"
        val yyyymmdd = info.startTime.replace("/", "").substringBefore(" ").take(DATE_STRING_LENGTH)
            .ifEmpty { "00000000" }

        return template
            .replace("{YYYYMMDD}", yyyymmdd)
            .replace("{Sente}", info.senteName.ifEmpty { "unknown" })
            .replace("{Gote}", info.goteName.ifEmpty { "unknown" })
            .let { it + "." + path.extension }
    }

    override fun updateResult(path: Path, result: String) {
        updateKifuResult(path, result)
    }

    override fun updateHeader(path: Path, event: String, startTime: String) {
        updateKifuHeader(path, event, startTime)
    }
}
