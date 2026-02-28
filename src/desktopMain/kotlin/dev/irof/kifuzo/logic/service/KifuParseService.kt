package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.parser.KifuFormatParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.csa.CsaParser
import dev.irof.kifuzo.logic.parser.kif.KifParser
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
    fun parseManually(lines: List<String>, state: ShogiBoardState)
    fun scanKifuInfo(lines: List<String>): KifuInfo
    fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo>
    fun convertCsaToKifu(path: Path): Path
}

class KifuParseServiceImpl : KifuParseService {
    companion object {
        private const val SAMPLE_LINES_FOR_FORMAT_DETECTION = 20
    }

    private val kifParser: KifuFormatParser = KifParser()
    private val csaParser: KifuFormatParser = CsaParser()

    override fun parse(path: Path, state: ShogiBoardState) {
        val lines = readLinesWithEncoding(path)
        if (lines.isEmpty()) throw KifuParseException("ファイルが空です。")

        val sample = lines.take(SAMPLE_LINES_FOR_FORMAT_DETECTION)
        val isCsa = isCsaFormat(sample)
        val isKif = isKifFormat(sample)
        val ext = path.extension.lowercase()

        val parser = when {
            isCsa -> csaParser
            isKif -> kifParser
            else -> if (ext == "csa") csaParser else kifParser
        }

        val warning = when {
            isCsa && ext != "csa" -> "拡張子は .$ext ですが、中身は CSA 形式のようです。CSA として読み込みました。"
            isKif && ext == "csa" -> "拡張子は .csa ですが、中身は KIF 形式のようです。KIF として読み込みました。"
            else -> null
        }

        parser.parse(lines, state, warningMessage = warning)
    }

    override fun parseManually(path: Path, state: ShogiBoardState) {
        val lines = readLinesWithEncoding(path)
        parseManually(lines, state)
    }

    override fun parseManually(lines: List<String>, state: ShogiBoardState) {
        if (lines.isEmpty()) throw KifuParseException("ファイルが空です。")

        val sample = lines.take(SAMPLE_LINES_FOR_FORMAT_DETECTION)
        val isCsa = isCsaFormat(sample)
        val isKif = isKifFormat(sample)

        val parser = when {
            isCsa -> csaParser
            isKif -> kifParser
            else -> throw KifuParseException("棋譜形式（KIF/CSA）を判定できませんでした。")
        }

        parser.parse(lines, state)
    }

    override fun scanKifuInfo(lines: List<String>): KifuInfo {
        if (lines.isEmpty()) return KifuInfo(java.nio.file.Paths.get(""), isError = true)

        val sample = lines.take(SAMPLE_LINES_FOR_FORMAT_DETECTION)
        val isCsa = isCsaFormat(sample)

        val parser = when {
            isCsa -> csaParser
            else -> kifParser
        }

        return parser.scanInfo(lines)
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
        .associateWith { path ->
            try {
                val lines = readLinesWithEncoding(path)
                val parser = if (path.extension.lowercase() == "csa") csaParser else kifParser
                parser.scanInfo(lines).copy(path = path)
            } catch (e: java.io.IOException) {
                logger.error(e) { "IO error scanning header for $path" }
                KifuInfo(path, isError = true)
            } catch (e: KifuParseException) {
                logger.error(e) { "Parse error scanning header for $path" }
                KifuInfo(path, isError = true)
            }
        }

    override fun convertCsaToKifu(path: Path): Path {
        dev.irof.kifuzo.logic.parser.convertCsaToKifu(path)
        return (path.parent ?: path).resolve(path.nameWithoutExtension + ".kifu")
    }
}
