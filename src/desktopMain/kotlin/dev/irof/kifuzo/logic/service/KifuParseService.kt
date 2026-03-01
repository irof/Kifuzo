package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.parser.KifuFormat
import dev.irof.kifuzo.logic.parser.KifuFormatHandler
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.csa.CsaParser
import dev.irof.kifuzo.logic.parser.kif.KifParser
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

/**
 * 棋譜の解析を行うサービス。
 */
interface KifuParseService {
    fun parse(path: Path, state: ShogiBoardState)
    fun parseManually(path: Path, state: ShogiBoardState)
    fun parseManually(lines: List<String>, state: ShogiBoardState)
    fun scanInfo(path: Path): KifuInfo
    fun scanInfo(lines: List<String>, suggestedFormat: KifuFormat? = null): KifuInfo
    fun convertCsaToKifu(path: Path): Path
}

class KifuParseServiceImpl : KifuParseService {
    private val kifParser: KifuFormatHandler = KifParser()
    private val csaParser: KifuFormatHandler = CsaParser()

    override fun parse(path: Path, state: ShogiBoardState) {
        try {
            val lines = readLinesWithEncoding(path)
            val format = KifuFormat.fromPath(path)
            val handler = if (format == KifuFormat.CSA) csaParser else kifParser
            handler.parse(lines, state)
        } catch (e: IOException) {
            logger.error(e) { "Failed to read file: $path" }
            throw KifuParseException("ファイルの読み込みに失敗しました: ${path.name}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun parseManually(path: Path, state: ShogiBoardState) {
        try {
            val lines = readLinesWithEncoding(path)
            parseManually(lines, state)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read file for manual parse: $path" }
            throw KifuParseException("ファイルの読み込みに失敗しました: ${path.name}")
        }
    }

    override fun parseManually(lines: List<String>, state: ShogiBoardState) {
        val format = detectFormat(lines) ?: throw KifuParseException("棋譜形式を判別できませんでした。")
        val handler = if (format == KifuFormat.CSA) csaParser else kifParser
        handler.parse(lines, state)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun scanInfo(path: Path): KifuInfo = try {
        val lines = readLinesWithEncoding(path)
        val formatFromPath = KifuFormat.fromPath(path)
        scanInfo(lines, formatFromPath).copy(path = path)
    } catch (e: Exception) {
        logger.error(e) { "Failed to scan info for $path" }
        KifuInfo(path, isError = true)
    }

    override fun scanInfo(lines: List<String>, suggestedFormat: KifuFormat?): KifuInfo {
        val format = detectFormat(lines) ?: suggestedFormat ?: throw KifuParseException("棋譜形式を判別できませんでした。")
        val handler = if (format == KifuFormat.CSA) csaParser else kifParser
        return handler.scanInfo(lines)
    }

    override fun convertCsaToKifu(path: Path): Path = dev.irof.kifuzo.logic.parser.convertCsaToKifu(path)

    private fun detectFormat(lines: List<String>): KifuFormat? {
        if (lines.isEmpty()) return null
        return lines.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { t ->
                when {
                    isCsaLine(t) -> KifuFormat.CSA
                    isKifLine(t) -> KifuFormat.KIF
                    else -> null
                }
            }
            .filterNotNull()
            .firstOrNull()
    }

    private fun isCsaLine(t: String): Boolean = t.startsWith("V2.") || t.startsWith("P") || t.startsWith("+") || t.startsWith("-") || t.startsWith("$")

    private fun isKifLine(t: String): Boolean = t.startsWith("開始日時") || t.startsWith("棋戦") || t.startsWith("先手") || t.startsWith("後手") || t.startsWith("指し手") || t.startsWith("#")
}
