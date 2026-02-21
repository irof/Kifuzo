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
    fun testBoardLayoutStandard() {
        val isFlipped = false
        val rangeX = BoardLayout.getRangeX(isFlipped) // 0..8
        val labels = BoardLayout.getSujiLabels() // 9, 8, ..., 1

        // 左端 (x = rangeX.first = 0)
        val x0 = rangeX.first
        assertEquals(0, x0)
        assertEquals("９", labels[x0])
        assertEquals(9, Square.fromIndex(x0, 0).file)

        // 右端 (x = rangeX.last = 8)
        val x8 = rangeX.last
        assertEquals(8, x8)
        assertEquals("１", labels[x8])
        assertEquals(1, Square.fromIndex(x8, 0).file)
    }

    @Test
    fun testBoardLayoutFlipped() {
        val isFlipped = true
        val rangeX = BoardLayout.getRangeX(isFlipped) // 8 downTo 0
        val labels = BoardLayout.getSujiLabels() // 9, 8, ..., 1

        // 左端 (x = rangeX.first = 8)
        val xFirst = rangeX.first
        assertEquals(8, xFirst)
        assertEquals("１", labels[xFirst])
        assertEquals(1, Square.fromIndex(xFirst, 0).file)

        // 右端 (x = rangeX.last = 0)
        val xLast = rangeX.last
        assertEquals(0, xLast)
        assertEquals("９", labels[xLast])
        assertEquals(9, Square.fromIndex(xLast, 0).file)
    }
}
