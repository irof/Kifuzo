package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import java.nio.file.Path
import kotlin.io.path.*

class KifuRepository {
    
    fun scanDirectory(directory: Path): List<Path> {
        return try {
            directory.listDirectoryEntries().sortedWith(compareBy({ !it.isDirectory() }, { it.name.lowercase() }))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> {
        return files.filter { it.isRegularFile() && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
            .associateWith { scanKifuInfo(it) }
    }

    fun parse(path: Path, state: ShogiBoardState) {
        parseKifu(path, state)
    }

    fun convertCsa(path: Path): Path {
        convertCsaToKifu(path)
        return path.parent / (path.nameWithoutExtension + ".kifu")
    }

    fun updateSenkei(path: Path, senkei: String) {
        updateKifuSenkei(path, senkei)
    }

    fun importQuestFiles(): Int {
        return importShogiQuestFiles()
    }
}

/**
 * 棋譜の要約情報
 */
data class KifuInfo(
    val path: Path,
    val senteName: String = "",
    val goteName: String = "",
    val senkei: String = ""
)
