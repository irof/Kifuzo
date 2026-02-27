package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PieceTest {

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
    fun 駒を成らせることができること() {
        assertEquals(Piece.TO, Piece.FU.promote())
        assertEquals(Piece.UM, Piece.KA.promote())
        assertEquals(Piece.RY, Piece.HI.promote())
        assertEquals(Piece.OU, Piece.OU.promote())
    }
}
