package dev.irof.kifuzo.logic.parser.kif

import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KifParserTest {

    @Test
    fun 評価値コメントを抽出できること() {
        val state = ShogiBoardState()
        val kifLines = listOf(
            "指し手",
            "   1 ７六歩(77)",
            "* 123",
            "   2 ３四歩(33)",
            "*#評価値=-456",
            "   3 ２二角成(88)",
            "*#詰み=先手勝ち",
        )
        KifParser().parse(kifLines, state)

        assertEquals(Evaluation.Score(123), state.session.moves[0].evaluation)
        assertEquals(Evaluation.Score(-456), state.session.moves[1].evaluation)
        assertEquals(Evaluation.SenteWin, state.session.moves[2].evaluation)
    }

    @Test
    fun formatResultが結果行を正しく追加および更新できること() {
        val parser = KifParser()
        val lines = mutableListOf(
            "指し手",
            "   1 ７六歩(77)",
        )

        // 追加
        parser.formatResult(lines, "投了")
        assertTrue(lines.last().contains("2 投了"))

        // 更新
        parser.formatResult(lines, "中断")
        assertTrue(lines.last().contains("2 中断"))
        assertEquals(3, lines.size)
    }

    @Test
    fun formatHeaderがヘッダーを正しく更新および追加できること() {
        val parser = KifParser()
        val lines = mutableListOf(
            "棋戦：旧棋戦",
            "指し手",
        )

        // 更新
        parser.formatHeader(lines, "新棋戦", "2026/03/02")
        assertTrue(lines.any { it == "棋戦：新棋戦" })
        assertTrue(lines.any { it == "開始日時：2026/03/02" })

        // 新規追加 (場所指定なしから追加される位置の確認)
        val linesEmpty = mutableListOf("指し手")
        parser.formatHeader(linesEmpty, "新規", "2026/03/03")
        // updateOrAddKifLine が 0 番目に挿入し続けるため、後から追加したものが先頭に来る
        assertEquals("開始日時：2026/03/03", linesEmpty[0])
        assertEquals("棋戦：新規", linesEmpty[1])
        assertEquals("指し手", linesEmpty[2])
    }

    @Test
    fun formatHeaderが空のリストでも正しく動作すること() {
        val parser = KifParser()
        val lines = mutableListOf<String>()
        parser.formatHeader(lines, "棋戦", "日時")
        assertEquals(2, lines.size)
    }

    @Test
    fun parseが不正な変化手順を無視すること() {
        val state = ShogiBoardState()
        val kifLines = listOf(
            "指し手",
            "   1 ７六歩(77)",
            "変化：10手", // 10手目は存在しない
        )
        // 例外が発生せずにパースが完了することを確認
        KifParser().parse(kifLines, state)
        assertEquals(1, state.session.moves.size)
    }
}
