package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.models.Piece
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HeaderParserTest {

    @Test
    fun resetToStandardが盤面を初期化すること() {
        val parser = HeaderParser()
        parser.prepareNonStandardBoard()
        parser.senteMochi.add(Piece.FU)

        parser.resetToStandard()
        assertTrue(parser.isStandardStart)
        assertTrue(parser.senteMochi.isEmpty())
        assertEquals(Piece.KY, parser.currentCells[0][0]?.piece)
    }

    @Test
    fun 形式が混在している場合に例外を投げること() {
        val parser = HeaderParser()
        parser.processLine("棋戦：A", 0) // KIF
        assertFailsWith<KifuParseException> {
            parser.processLine("N+Sente", 1) // CSA
        }
    }

    @Test
    fun 盤面が不完全な場合に例外を投げること() {
        val parser = HeaderParser()
        parser.boardY = 1 // 1行だけ
        assertFailsWith<KifuParseException> {
            parser.build()
        }
    }

    @Test
    fun 空行や無効な行を無視すること() {
        val parser = HeaderParser()
        assertEquals(false, parser.processLine("", 0))
        assertEquals(false, parser.processLine("  ", 1))
        assertEquals(false, parser.processLine("未知の行", 2))
    }
}
