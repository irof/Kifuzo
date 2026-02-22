package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.Square
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals

class SenkeiDetectorTest {

    private fun detect(kifu: String): String {
        val state = ShogiBoardState()
        parseKifu(kifu.trimIndent().lines(), state)
        return detectSenkei(state.session.history)
    }

    @Test
    fun testDetectShikaHisha() {
        // 四間飛車にするための手順
        val result = detect(
            """
                1 ７六歩(77)
                2 ３四歩(33)
                3 ６六歩(67)
                4 ８四歩(83)
                5 ６八飛(28)
                6 ６二銀(71)
                7 ４八玉(59)
                8 ４二玉(51)
                9 ３八玉(48)
                10 ３二玉(42)
                11 ２八玉(38)
                12 ５二金右(41)
            """,
        )
        assertEquals("四間飛車", result)
    }

    @Test
    fun testDetectGoteNakabisha() {
        // 後手中飛車にするための手順
        val result = detect(
            """
                1 ７六歩(77)
                2 ５二飛(82)
                3 ２六歩(27)
                4 ５四歩(53)
                5 ２五歩(26)
                6 ３二金(41)
                7 ２四歩(25)
                8 同　歩(23)
                9 同　飛(28)
                10 ３四歩(33)
            """,
        )
        assertEquals("中飛車", result)
    }
}
