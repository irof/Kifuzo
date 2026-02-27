package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

interface KifuParseService {
    fun parse(path: Path, state: ShogiBoardState)
    fun parseManually(path: Path, state: ShogiBoardState)
    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo>
    fun convertCsaToKifu(path: Path): Path
}

class KifuParseServiceImpl : KifuParseService {
    companion object {
        private const val SAMPLE_LINES_FOR_FORMAT_DETECTION = 20
    }

    override fun parse(path: Path, state: ShogiBoardState) {
        if (path.extension.lowercase() == "csa") {
            parseCsa(path, state)
        } else {
            parseKifu(path, state)
        }
    }

    override fun parseManually(path: Path, state: ShogiBoardState) {
        val lines = readLinesWithEncoding(path)
        parseManually(lines, state)
    }

    fun parseManually(lines: List<String>, state: ShogiBoardState) {
        if (lines.isEmpty()) throw KifuParseException("ファイルが空です。")

        // 最初の数行で判定
        val sample = lines.take(SAMPLE_LINES_FOR_FORMAT_DETECTION)
        val isCsa = isCsaFormat(sample)
        val isKif = isKifFormat(sample)

        when {
            isCsa -> parseCsa(lines, state)
            isKif -> parseKifu(lines, state)
            else -> throw KifuParseException("棋譜形式（KIF/CSA）を判定できませんでした。")
        }
    }

    private fun isCsaFormat(sample: List<String>): Boolean = sample.any { line ->
        line.startsWith("V") || line.startsWith("N+") || line.startsWith("N-") ||
            line.startsWith("P") || line.startsWith("$") || line.startsWith("+") || line.startsWith("-")
    }

    private fun isKifFormat(sample: List<String>): Boolean = sample.any { line ->
        line.startsWith("開始日時：") || line.startsWith("場所：") || line.startsWith("手合割：") ||
            line.startsWith("先手：") || line.startsWith("後手：") || line.startsWith("指し手") ||
            Regex("""^\s*1\s+""").containsMatchIn(line)
    }

    override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = files
        .filter { it.extension.lowercase() in listOf("kifu", "kif", "csa") }
        .associateWith { scanKifuInfo(it) }

    override fun convertCsaToKifu(path: Path): Path {
        dev.irof.kifuzo.logic.parser.convertCsaToKifu(path)
        return (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    }
}
