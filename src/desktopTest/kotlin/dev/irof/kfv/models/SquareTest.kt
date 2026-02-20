package dev.irof.kfv.models

import kotlin.test.Test
import kotlin.test.assertEquals

class SquareTest {
    @Test
    fun testIndexConversion() {
        // 1筋1段 (11) -> x=8, y=0
        val s11 = Square(1, 1)
        assertEquals(8, s11.xIndex)
        assertEquals(0, s11.yIndex)

        // 9筋9段 (99) -> x=0, y=8
        val s99 = Square(9, 9)
        assertEquals(0, s99.xIndex)
        assertEquals(8, s99.yIndex)

        // 5筋5段 (55) -> x=4, y=4
        val s55 = Square(5, 5)
        assertEquals(4, s55.xIndex)
        assertEquals(4, s55.yIndex)
    }

    @Test
    fun testFromIndex() {
        val s = Square.fromIndex(0, 0) // x=0 (9筋), y=0 (1段)
        assertEquals(9, s.file)
        assertEquals(1, s.rank)
    }
}
