package dev.irof.kifuzo.logic.parser.kif

import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KifHeaderParserTest {

    @Test
    fun parseMochigomaが漢字数字を正しくパースできること() {
        val pieces = Piece.parseMochigoma("飛二 角一 歩三")
        assertEquals(2, pieces.count { it == Piece.HI })
        assertEquals(1, pieces.count { it == Piece.KA })
        assertEquals(3, pieces.count { it == Piece.FU })
        assertEquals(6, pieces.size)
    }

    @Test
    fun parseMochigomaがアラビア数字もパースできること() {
        val pieces = Piece.parseMochigoma("飛2 角1")
        assertEquals(2, pieces.count { it == Piece.HI })
        assertEquals(1, pieces.count { it == Piece.KA })
    }

    @Test
    fun findPieceBySymbolが別名を正しく判定できること() {
        assertEquals(Piece.OU, Piece.findPieceBySymbol("王"))
        assertEquals(Piece.OU, Piece.findPieceBySymbol("玉"))
        assertEquals(Piece.RY, Piece.findPieceBySymbol("竜"))
        assertEquals(Piece.RY, Piece.findPieceBySymbol("龍"))
        assertEquals(Piece.UM, Piece.findPieceBySymbol("馬"))
        assertNull(Piece.findPieceBySymbol("・"))
    }

    @Test
    fun handleMetadataLineが棋戦と場所をマージできること() {
        val hp = HeaderParser()
        KifHeaderParser.handleMetadataLine(hp, "棋戦：A")
        assertEquals("A", hp.event)

        KifHeaderParser.handleMetadataLine(hp, "場所：B")
        assertEquals("A (B)", hp.event)
    }

    @Test
    fun handleBoardLineが固定幅で盤面をパースできること() {
        val hp = HeaderParser()
        // 1段目
        KifHeaderParser.handleBoardLine(hp, "|v香v桂v銀v金v玉v金v銀v桂v香|")
        assertEquals(Piece.KY, hp.currentCells[0][0]?.piece)
        assertEquals(PieceColor.White, hp.currentCells[0][0]?.color)

        // 2段目 (空きマスと龍)
        hp.boardY = 1
        KifHeaderParser.handleBoardLine(hp, "| ・ ・ ・ ・ ・ ・ ・ ・ 龍|")
        assertNull(hp.currentCells[1][0])
        assertEquals(Piece.RY, hp.currentCells[1][8]?.piece)
        assertEquals(PieceColor.Black, hp.currentCells[1][8]?.color)
    }

    @Test
    fun isMochigomaLineが各種手番表記に対応していること() {
        assertTrue(KifHeaderParser.isMochigomaLine("先手の手持駒："))
        assertTrue(KifHeaderParser.isMochigomaLine("後手の手持駒："))
        assertTrue(KifHeaderParser.isMochigomaLine("下手持駒："))
        assertTrue(KifHeaderParser.isMochigomaLine("上手持駒："))
    }
}
