package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

interface KifuParseService {
    fun parse(path: Path, state: ShogiBoardState)
    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo>
    fun convertCsaToKifu(path: Path): Path
}

class KifuParseServiceImpl : KifuParseService {
    override fun parse(path: Path, state: ShogiBoardState) {
        if (path.extension.lowercase() == "csa") {
            parseCsa(path, state)
        } else {
            parseKifu(path, state)
        }
    }

    override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = files
        .filter { it.extension.lowercase() in listOf("kifu", "kif", "csa") }
        .associateWith { scanKifuInfo(it) }

    override fun convertCsaToKifu(path: Path): Path {
        dev.irof.kifuzo.logic.convertCsaToKifu(path)
        return (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    }
}
