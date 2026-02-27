package dev.irof.kifuzo.logic.parser.kif
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.handler.FileActionHandler
import dev.irof.kifuzo.logic.handler.ImportHandler
import dev.irof.kifuzo.logic.handler.SettingsHandler
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import dev.irof.kifuzo.logic.service.FileTreeManager
import dev.irof.kifuzo.logic.service.KifuRepository
import dev.irof.kifuzo.logic.service.KifuRepositoryImpl
import dev.irof.kifuzo.logic.service.KifuSessionBuilder
import dev.irof.kifuzo.models.Piece
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PieceParserTest {

    @Test
    fun 文字列から駒を検索できること() {
        assertEquals(Piece.FU, Piece.Companion.findPieceBySymbol("歩"))
        assertEquals(Piece.OU, Piece.Companion.findPieceBySymbol("玉"))
        assertEquals(Piece.OU, Piece.Companion.findPieceBySymbol("王"))
        assertEquals(Piece.RY, Piece.Companion.findPieceBySymbol("竜"))
        assertEquals(Piece.RY, Piece.Companion.findPieceBySymbol("龍"))
        assertEquals(Piece.UM, Piece.Companion.findPieceBySymbol("馬"))
    }

    @Test
    fun 持駒文字列をパースできること() {
        // 空、なし
        assertTrue(Piece.Companion.parseMochigoma("").isEmpty())
        assertTrue(Piece.Companion.parseMochigoma("なし").isEmpty())

        // 単一
        assertEquals(listOf(Piece.HI), Piece.Companion.parseMochigoma("飛"))

        // 数値付き
        assertEquals(listOf(Piece.HI, Piece.HI), Piece.Companion.parseMochigoma("飛二"))
        assertEquals(listOf(Piece.GI, Piece.GI, Piece.GI), Piece.Companion.parseMochigoma("銀3"))

        // 複数
        val result = Piece.Companion.parseMochigoma("飛二 角 銀三")
        assertEquals(6, result.size)
        assertEquals(2, result.count { it == Piece.HI })
        assertEquals(1, result.count { it == Piece.KA })
        assertEquals(3, result.count { it == Piece.GI })

        // 10枚以上（数値）
        val manyFu = Piece.Companion.parseMochigoma("歩18")
        assertEquals(18, manyFu.size)
        assertTrue(manyFu.all { it == Piece.FU })
    }
}
