package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals

class KifuSessionTest {
    @Test
    fun KifuSessionのプロパティが正しく取得できること() {
        val snapshot = BoardSnapshot(BoardSnapshot.getInitialCells())
        val moves = listOf(Move(1, "▲７六歩", snapshot))
        val session = KifuSession(initialSnapshot = snapshot, moves = moves)

        assertEquals(1, session.maxStep)
        assertEquals(2, session.history.size)
        assertEquals(snapshot, session.getSnapshotAt(0))
        assertEquals(snapshot, session.getSnapshotAt(1))
    }

    @Test
    fun coerceStepが範囲内に制限されること() {
        val session = KifuSession(moves = listOf(Move(1, "▲７六歩", BoardSnapshot(BoardSnapshot.getInitialCells()))))
        assertEquals(0, session.coerceStep(-1))
        assertEquals(0, session.coerceStep(0))
        assertEquals(1, session.coerceStep(1))
        assertEquals(1, session.coerceStep(2))
    }
}
