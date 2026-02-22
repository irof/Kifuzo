package dev.irof.kifuzo.logic

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
    fun testCalculateImportTargetSuccess() {
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
    fun testCalculateImportTargetUnknown() {
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
    fun testCalculateImportTargetNonCsa() {
        val lines = """
            これは棋譜ではありません
            ただのテキストファイルです
        """.trimIndent().lines()
        val result = calculateImportTarget(lines, 0L)

        // CSAマーカーがない場合は null
        assertNull(result)
    }

    @Test
    fun testCalculateImportTargetEmpty() {
        assertNull(calculateImportTarget(emptyList(), 0L))
    }
}
