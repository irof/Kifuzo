package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.logic.parser.KifuFormat
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KifuParseServiceTest {

    private val service = KifuParseServiceImpl()

    @Test
    fun parseManuallyが形式を自動判別してパースできること() {
        val kifLines = listOf(
            "開始日時：2026/03/02",
            "指し手",
            "   1 ７六歩(77)",
        )
        val state = ShogiBoardState()
        service.parseManually(kifLines, state)

        assertTrue(state.session.moves.isNotEmpty())
        // 1手目の棋譜テキストを確認。toMoveLabel() ではなくオリジナルの moveText を想定
        assertTrue(state.session.moves[0].moveText.contains("７六歩"))

        val csaLines = listOf(
            "V2.2",
            "N+Sente",
            "N-Gote",
            "+7776FU",
        )
        service.parseManually(csaLines, state)
        assertTrue(state.session.moves.isNotEmpty())
    }

    @Test
    fun scanInfoが形式を自動判別して情報を取得できること() {
        val kifLines = listOf(
            "棋戦：テスト棋戦",
            "先手：先手さん",
            "後手：後手さん",
            "指し手",
        )
        val info = service.scanInfo(kifLines)
        assertEquals("テスト棋戦", info.event)
        assertEquals("先手さん", info.senteName)
        assertEquals("後手さん", info.goteName)
        assertEquals(KifuFormat.KIF, info.format)

        val csaLines = listOf(
            "V2.2",
            "N+Sente",
            "N-Gote",
            "\$EVENT:CSA-Event",
        )
        val infoCsa = service.scanInfo(csaLines)
        assertEquals("CSA-Event", infoCsa.event)
        assertEquals("Sente", infoCsa.senteName)
        assertEquals("Gote", infoCsa.goteName)
        assertEquals(KifuFormat.CSA, infoCsa.format)
    }
}
