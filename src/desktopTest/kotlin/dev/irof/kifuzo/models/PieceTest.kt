package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PieceTest {

    @Test
    fun 文字列から駒を検索できること() {
        assertEquals(Piece.FU, Piece.findPieceBySymbol("歩"))
        assertEquals(Piece.OU, Piece.findPieceBySymbol("玉"))
        assertEquals(Piece.OU, Piece.findPieceBySymbol("王"))
        assertEquals(Piece.RY, Piece.findPieceBySymbol("竜"))
        assertEquals(Piece.RY, Piece.findPieceBySymbol("龍"))
        assertEquals(Piece.UM, Piece.findPieceBySymbol("馬"))
    }

    @Test
    fun 成り駒を元の駒に変換できること() {
        assertEquals(Piece.FU, Piece.TO.toBase())
        assertEquals(Piece.KA, Piece.UM.toBase())
        assertEquals(Piece.HI, Piece.RY.toBase())
        assertEquals(Piece.FU, Piece.FU.toBase())
    }

    @Test
    fun 成っているかどうかを判定できること() {
        assertTrue(Piece.TO.isPromoted())
        assertTrue(Piece.UM.isPromoted())
        assertFalse(Piece.FU.isPromoted())
        assertFalse(Piece.OU.isPromoted())
    }

    @Test
    fun 持駒文字列をパースできること() {
        // 空、なし
        assertTrue(Piece.parseMochigoma("").isEmpty())
        assertTrue(Piece.parseMochigoma("なし").isEmpty())

        // 単一
        assertEquals(listOf(Piece.HI), Piece.parseMochigoma("飛"))

        // 数値付き
        assertEquals(listOf(Piece.HI, Piece.HI), Piece.parseMochigoma("飛二"))
        assertEquals(listOf(Piece.GI, Piece.GI, Piece.GI), Piece.parseMochigoma("銀3"))

        // 複数
        val result = Piece.parseMochigoma("飛二 角 銀三")
        assertEquals(6, result.size)
        assertEquals(2, result.count { it == Piece.HI })
        assertEquals(1, result.count { it == Piece.KA })
        assertEquals(3, result.count { it == Piece.GI })

        // 10枚以上（数値）
        val manyFu = Piece.parseMochigoma("歩18")
        assertEquals(18, manyFu.size)
        assertTrue(manyFu.all { it == Piece.FU })
    }
}
