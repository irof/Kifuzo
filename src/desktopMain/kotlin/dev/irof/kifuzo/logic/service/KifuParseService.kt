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
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

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
        val lines = readLinesWithEncoding(path)
        if (lines.isEmpty()) throw KifuParseException("ファイルが空です。")

        val sample = lines.take(SAMPLE_LINES_FOR_FORMAT_DETECTION)
        val isCsa = isCsaFormat(sample)
        val isKif = isKifFormat(sample)
        val ext = path.extension.lowercase()

        when {
            isCsa -> {
                val warning = if (ext != "csa") {
                    "拡張子は .$ext ですが、中身は CSA 形式のようです。CSA として読み込みました。"
                } else {
                    null
                }
                parseCsa(lines, state, warningMessage = warning)
            }
            isKif -> {
                val warning = if (ext == "csa") {
                    "拡張子は .csa ですが、中身は KIF 形式のようです。KIF として読み込みました。"
                } else {
                    null
                }
                parseKifu(lines, state, warningMessage = warning)
            }
            else -> {
                // どちらとも判定できない場合は拡張子に従ってフォールバックを試みる
                if (ext == "csa") {
                    parseCsa(lines, state)
                } else {
                    parseKifu(lines, state)
                }
            }
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
            line.startsWith("$") ||
            Regex("""^P[1-9+ -]""").containsMatchIn(line) ||
            Regex("""^[+-]\d{4}[A-Z]{2}""").containsMatchIn(line)
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
