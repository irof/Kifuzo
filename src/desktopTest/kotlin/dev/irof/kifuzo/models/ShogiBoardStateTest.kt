package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShogiBoardStateTest {
    @Test
    fun ShogiBoardStateの初期状態が正しいこと() {
        val boardState = ShogiBoardState()
        assertEquals(0, boardState.currentStep)
        assertTrue(boardState.currentMoves.isEmpty())
        assertEquals(boardState.currentInitialSnapshot, boardState.currentBoard)
    }

    @Test
    fun updateSessionで状態が更新されること() {
        val boardState = ShogiBoardState()
        val snapshot = BoardSnapshot(BoardSnapshot.getInitialCells())
        val moves = listOf(Move(1, "▲７六歩", snapshot))
        val session = KifuSession(initialSnapshot = snapshot, moves = moves)

        boardState.updateSession(session)
        assertEquals(session, boardState.session)
        assertEquals(moves, boardState.currentMoves)
        assertEquals(0, boardState.currentStep)
    }

    @Test
    fun currentStepの変更でcurrentBoardが更新されること() {
        val boardState = ShogiBoardState()
        val snapshot1 = BoardSnapshot(BoardSnapshot.getInitialCells())
        val snapshot2 = snapshot1.copy(lastFrom = Square(7, 7), lastTo = Square(7, 6))
        val moves = listOf(Move(1, "▲７六歩", snapshot2))
        val session = KifuSession(initialSnapshot = snapshot1, moves = moves)

        boardState.updateSession(session)
        boardState.currentStep = 1
        assertEquals(snapshot2, boardState.currentBoard)
        assertEquals(2, boardState.currentHistory.size)
    }

    @Test
    fun switchHistoryとresetToMainHistoryが正しく動作すること() {
        val boardState = ShogiBoardState()
        val snapshot1 = BoardSnapshot(BoardSnapshot.getInitialCells())
        val mainMoves = listOf(Move(1, "▲７六歩", snapshot1))
        val session = KifuSession(initialSnapshot = snapshot1, moves = mainMoves)
        boardState.updateSession(session)

        val variationMoves = listOf(
            Move(1, "▲７六歩", snapshot1), // 分岐点
            Move(2, "△３四歩", snapshot1),
        )
        boardState.switchHistory(variationMoves)

        assertEquals(1, boardState.currentMoves.size)
        assertEquals("△３四歩", boardState.currentMoves[0].moveText)
        assertEquals(0, boardState.currentStep)

        boardState.resetToMainHistory()
        assertEquals(mainMoves, boardState.currentMoves)
        assertEquals(0, boardState.currentStep)
    }

    @Test
    fun clearで初期化されること() {
        val boardState = ShogiBoardState()
        boardState.currentStep = 10 // 無効だがセットしてみる
        boardState.clear()
        assertEquals(0, boardState.currentStep)
        assertTrue(boardState.currentMoves.isEmpty())
    }
}
