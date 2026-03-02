package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShogiDomainTest {
    @Test
    fun Evaluationの各種メソッドが期待通りに動作すること() {
        assertEquals(100, Evaluation.Score(100).orNull())
        assertEquals(ShogiConstants.WIN_SCORE, Evaluation.SenteWin.orNull())
        assertEquals(ShogiConstants.LOSE_SCORE, Evaluation.GoteWin.orNull())
        assertNull(Evaluation.Unknown.orNull())

        assertEquals(100, Evaluation.Score(100).orZero())
        assertEquals(0, Evaluation.Unknown.orZero())

        assertTrue(Evaluation.Score(100).isSignificant())
        assertFalse(Evaluation.Score(0).isSignificant())
        assertTrue(Evaluation.SenteWin.isSignificant())
        assertTrue(Evaluation.GoteWin.isSignificant())
        assertFalse(Evaluation.Unknown.isSignificant())
    }

    @Test
    fun PieceColor_toSymbolが期待通りに動作すること() {
        assertEquals("▲", PieceColor.Black.toSymbol())
        assertEquals("△", PieceColor.White.toSymbol())
    }

    @Test
    fun Move_toMoveLabelが期待通りに動作すること() {
        val snapshot = BoardSnapshot(BoardSnapshot.getInitialCells())
        // KIF形式の行を模したデータ
        val move1 = Move(1, "   1 ７六歩(77)   ( 0:01/00:00:01)", snapshot)
        assertEquals("▲７六歩", move1.toMoveLabel())

        // CSA形式のパース後を模したデータ
        val move2 = Move(2, "2 ３四歩", snapshot)
        assertEquals("△３四歩", move2.toMoveLabel())

        // 形式が合わない場合のフォールバック
        val move3 = Move(3, "特殊な手", snapshot)
        assertEquals("▲特殊な手", move3.toMoveLabel())
    }

    @Test
    fun GameResult_isFinishedが期待通りに動作すること() {
        assertFalse(GameResult.isFinished("▲２六歩", Evaluation.Score(100)))
        assertTrue(GameResult.isFinished("投了", Evaluation.Score(100)))
        assertTrue(GameResult.isFinished("▲２六歩", Evaluation.SenteWin))
        assertTrue(GameResult.isFinished("▲２六歩", Evaluation.Score(40000)))
        assertTrue(GameResult.isFinished("詰み", Evaluation.Score(0)))
    }

    @Test
    fun BoardLayout_getRangeYが期待通りに動作すること() {
        val rangeNormal = BoardLayout.getRangeY(false)
        assertEquals(0, rangeNormal.first)
        assertEquals(8, rangeNormal.last)
        assertEquals(1, rangeNormal.step)

        val rangeFlipped = BoardLayout.getRangeY(true)
        assertEquals(8, rangeFlipped.first)
        assertEquals(0, rangeFlipped.last)
        assertEquals(-1, rangeFlipped.step)
    }

    @Test
    fun Square_fromIndexが期待通りに動作すること() {
        val s1 = Square.fromIndex(0, 0)
        assertEquals(9, s1.file)
        assertEquals(1, s1.rank)

        val s2 = Square.fromIndex(8, 8)
        assertEquals(1, s2.file)
        assertEquals(9, s2.rank)
    }
}
