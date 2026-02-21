package dev.irof.kfv.logic

import dev.irof.kfv.models.BoardSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class SenkeiDetectorTest {
    @Test
    fun testDetectShikaHisha() {
        // 先手四間飛車の局面を作成
        val cells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
        val rook = cells[7][7]!! // 飛車 (88)
        cells[7][7] = null
        cells[7][3] = rook // 68飛へ移動 (x=3は6筋)

        val finalCells = cells.map { it.toList() }
        val history = List(30) {
            BoardSnapshot(cells = finalCells)
        }

        assertEquals("四間飛車", detectSenkei(history))
    }

    @Test
    fun testDetectGoteNakabisha() {
        // 後手中飛車の局面を作成
        val cells = BoardSnapshot.getInitialCells().map { it.toMutableList() }.toMutableList()
        val rook = cells[1][1]!! // 後手飛車 (22)
        cells[1][1] = null
        cells[1][4] = rook // 52飛へ移動

        val finalCells = cells.map { it.toList() }
        val history = List(30) {
            BoardSnapshot(cells = finalCells)
        }

        assertEquals("中飛車", detectSenkei(history))
    }
}
