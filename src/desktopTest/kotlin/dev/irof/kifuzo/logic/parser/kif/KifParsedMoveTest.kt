package dev.irof.kifuzo.logic.parser.kif

import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KifParsedMoveTest {

    @Test
    fun parseMoveが略称の成駒を正しく判定できること() {
        // 圭 (桂成)
        val moveKe = parseMove("   1 ７六圭(77)", null) as KifuParsedMove.Move
        assertTrue(moveKe.isPromote)

        // 杏 (香成)
        val moveKy = parseMove("   1 ７六杏(77)", null) as KifuParsedMove.Move
        assertTrue(moveKy.isPromote)

        // 全 (銀成)
        val moveGi = parseMove("   1 ７六全(77)", null) as KifuParsedMove.Move
        assertTrue(moveGi.isPromote)
    }

    @Test
    fun getToSquareが不正な座標で例外を投げること() {
        assertFailsWith<KifuParseException> {
            parseMove("   1 ７A歩(77)", null)
        }

        // 同 の移動先不明
        assertFailsWith<KifuParseException> {
            parseMove("   1 同　歩(77)", null)
        }
    }

    @Test
    fun decodeX_Yが各種数値をパースできること() {
        // 全角、半角、漢字の混在テストは parseMove を通じて行う
        val move = parseMove("   1 1一歩(77)", null) as KifuParsedMove.Move
        assertEquals(1, move.to.file)
        assertEquals(1, move.to.rank)

        val moveZen = parseMove("   1 ９九歩(77)", null) as KifuParsedMove.Move
        assertEquals(9, moveZen.to.file)
        assertEquals(9, moveZen.to.rank)
    }

    @Test
    fun getFromSquareが範囲外の座標で例外を投げること() {
        assertFailsWith<KifuParseException> {
            parseMove("   1 ７六歩(00)", null)
        }
    }
}
