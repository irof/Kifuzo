package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

interface KifuRepository {
    fun scanDirectory(directory: Path, sortOption: FileSortOption = FileSortOption.NAME): List<Path>
    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo>
    fun parse(path: Path, state: ShogiBoardState)
    fun convertCsa(path: Path): Path
    fun updateResult(path: Path, result: String)
    fun generateProposedName(path: Path, template: String): String?
    fun renameFileTo(path: Path, newName: String): Path?
    fun renameKifuFile(path: Path, template: String): Path?
    fun importQuestFiles(sourceDir: Path, targetDir: Path): Int
}

class KifuRepositoryImpl : KifuRepository {
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
                            java.time.Instant.MIN
                        }
                    },
            )
        }
    } catch (e: IOException) {
        logger.error(e) { "Failed to scan directory: $directory" }
        emptyList()
    }

    override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = files.filter {
        it.isRegularFile() && (it.extension.lowercase() in listOf("kifu", "kif", "csa"))
    }
        .associateWith { scanKifuInfo(it) }

    override fun parse(path: Path, state: ShogiBoardState) {
        if (path.extension.lowercase() == "csa") {
            parseCsa(path, state)
        } else {
            parseKifu(path, state)
        }
    }

    override fun convertCsa(path: Path): Path {
        convertCsaToKifu(path)
        return (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    }

    override fun updateResult(path: Path, result: String) {
        updateKifuResult(path, result)
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

    override fun renameFileTo(path: Path, newName: String): Path? {
        val targetPath = path.parent?.resolve(newName) ?: return null
        if (path == targetPath) return path

        return try {
            java.nio.file.Files.move(path, targetPath)
            targetPath
        } catch (e: IOException) {
            logger.error(e) { "Failed to rename file from $path to $targetPath" }
            null
        }
    }

    override fun renameKifuFile(path: Path, template: String): Path? {
        val proposedName = generateProposedName(path, template) ?: return null
        return renameFileTo(path, proposedName)
    }

    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importShogiQuestFiles(sourceDir, targetDir)
}
