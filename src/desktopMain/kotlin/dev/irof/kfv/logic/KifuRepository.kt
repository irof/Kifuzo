package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import java.io.File

class KifuRepository {
    
    fun scanDirectory(directory: File): List<File> {
        val contents = directory.listFiles { file ->
            file.isDirectory || (file.isFile && file.extension.lowercase() in listOf("kif", "kifu", "kifz", "csa", "jkf", "txt"))
        }?.toList() ?: emptyList()
        return contents.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    fun getKifuInfos(files: List<File>): Map<File, KifuInfo> {
        return files.filter { it.isFile && (it.extension.lowercase() == "kifu" || it.extension.lowercase() == "kif") }
            .associateWith { scanKifuInfo(it) }
    }

    fun parse(file: File, state: ShogiBoardState) {
        parseKifu(file, state)
    }

    fun convertCsa(file: File): File {
        convertCsaToKifu(file)
        return File(file.parent, file.nameWithoutExtension + ".kifu")
    }

    fun updateSenkei(file: File, senkei: String) {
        updateKifuSenkei(file, senkei)
    }

    fun importQuestFiles(): Int {
        return importShogiQuestFiles()
    }
}
