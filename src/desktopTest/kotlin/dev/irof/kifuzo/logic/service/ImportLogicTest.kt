package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportLogicTest {

    private fun getMillis(year: Int, month: Int, day: Int): Long = LocalDateTime.of(year, month, day, 12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    @Test
    fun インポート後のファイル名を正しく計算できること() {
        val lines = """
            V2.2
            N+SentePlayer(1500)
            N-GotePlayer(1450)
            +7776FU
        """.trimIndent().lines()
        val millis = getMillis(2026, 2, 21)
        val result = calculateImportTarget(lines, millis)

        // レーティング部分は削除され、日付-先手-後手.csa になること
        assertEquals("20260221-SentePlayer-GotePlayer.csa", result)
    }

    @Test
    fun 後手が不明な場合にunknownとしてファイル名を計算すること() {
        val lines = """
            V2.2
            N+Sente
            +7776FU
        """.trimIndent().lines()
        val millis = getMillis(2026, 1, 1)
        val result = calculateImportTarget(lines, millis)

        // 後手が不明な場合は unknown になること
        assertEquals("20260101-Sente-unknown.csa", result)
    }

    @Test
    fun CSA形式でないファイルの場合はnullを返すこと() {
        val lines = """
            これは棋譜ではありません
            ただのテキストファイルです
        """.trimIndent().lines()
        val result = calculateImportTarget(lines, 0L)

        // CSAマーカーがない場合は null
        assertNull(result)
    }

    @Test
    fun 空のファイルリストの場合はnullを返すこと() {
        assertNull(calculateImportTarget(emptyList(), 0L))
    }
}
