package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

interface KifuRepository {
    fun scanDirectory(directory: Path): List<Path>
    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo>
    fun parse(path: Path, state: ShogiBoardState)
    fun convertCsa(path: Path): Path
    fun updateSenkei(path: Path, senkei: String)
    fun importQuestFiles(sourceDir: Path, targetDir: Path): Int
}

class KifuRepositoryImpl : KifuRepository {

    override fun scanDirectory(directory: Path): List<Path> = try {
        directory.listDirectoryEntries().sortedWith(compareBy({ !it.isDirectory() }, { it.name.lowercase() }))
    } catch (e: Exception) {
        emptyList()
    }

    override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = files.filter { it.isRegularFile() && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
        .associateWith { scanKifuInfo(it) }

    override fun parse(path: Path, state: ShogiBoardState) {
        parseKifu(path, state)
    }

    override fun convertCsa(path: Path): Path {
        convertCsaToKifu(path)
        return (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    }

    override fun updateSenkei(path: Path, senkei: String) {
        updateKifuSenkei(path, senkei)
    }

    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importShogiQuestFiles(sourceDir, targetDir)
}
