package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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

    @Test
    fun すべての駒に対してpromoteとtoBaseとisPromotedが動作すること() {
        Piece.entries.forEach { piece ->
            val promoted = piece.promote()
            val base = piece.toBase()

            if (piece.isPromoted()) {
                assertEquals(piece, promoted, "Promoted piece $piece should promote to itself")
                assertNotEquals(piece, base, "Promoted piece $piece should have a different base")
                assertEquals(piece, base.promote(), "Base of $piece should promote back to $piece")
            } else if (piece == Piece.OU || piece == Piece.KI) {
                assertEquals(piece, promoted, "$piece should not promote")
                assertEquals(piece, base, "$piece should have itself as base")
                assertFalse(piece.isPromoted())
            } else {
                assertNotEquals(piece, promoted, "Base piece $piece should promote to something else")
                assertEquals(piece, base, "Base piece $piece should have itself as base")
                assertFalse(piece.isPromoted())
            }
        }
    }
}
