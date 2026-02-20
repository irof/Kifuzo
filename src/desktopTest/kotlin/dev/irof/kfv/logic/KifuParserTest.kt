package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import kotlin.test.*

class KifuParserTest {

    @Test
    fun testScanKifuInfo() {
        val lines = listOf(
            "先手：先手太郎",
            "後手：後手花子",
            "戦型：矢倉",
            "1 ７六歩(77)",
        )
        val info = scanKifuInfo(lines)
        assertEquals("先手太郎", info.senteName)
        assertEquals("後手花子", info.goteName)
        assertEquals("矢倉", info.senkei)
    }

    @Test
    fun testParseKifuStandard() {
        val lines = listOf(
            "先手：先手",
            "後手：後手",
            "手数---指手---消費時間--",
            "1 ７六歩(77)",
            "2 ３四歩(33)",
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val session = state.session
        assertFalse(session.history.isEmpty())
        assertEquals(2, session.maxStep)
        assertEquals("先手", session.senteName)
        assertEquals("後手", session.goteName)

        // 1手目: 7六歩(77)
        val step1 = session.history[1]
        assertNull(step1.cells[6][2]) // 7七が空に
        assertEquals(Piece.FU to PieceColor.Black, step1.cells[5][2]) // 7六に歩

        // 2手目: 3四歩(33)
        val step2 = session.history[2]
        assertNull(step2.cells[2][6]) // 3三が空に
        assertEquals(Piece.FU to PieceColor.White, step2.cells[3][6]) // 3四に歩
    }

    @Test
    fun testParseKifuWithCaptureAndDrop() {
        val lines = listOf(
            "1 ７六歩(77)",
            "2 ３四歩(33)",
            "3 ２二角成(88)",
            "4 同　銀(31)",
            "5 ４五角打",
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val session = state.session
        // 3手目: 2二角成 (88から2二、2二には後手の角がいる)
        val step3 = session.history[3]
        assertEquals(Piece.UM to PieceColor.Black, step3.cells[1][7]) // 2二に馬
        assertEquals(listOf(Piece.KA), step3.senteMochigoma) // 後手の角をゲット

        // 4手目: 同 銀(31) (2二の馬を銀で取る)
        val step4 = session.history[4]
        assertEquals(Piece.GI to PieceColor.White, step4.cells[1][7]) // 2二に銀
        assertEquals(listOf(Piece.KA), step4.goteMochigoma) // 先手の馬を取って角が1枚に

        // 5手目: 4五角打
        val step5 = session.history[5]
        assertEquals(Piece.KA to PieceColor.Black, step5.cells[4][5]) // 4五に角
        assertEquals(emptyList<Piece>(), step5.senteMochigoma) // 1枚使ったので空に
    }

    @Test
    fun testParseKifuInitialPosition() {
        // 途中図からの開始（| で囲まれた配置）
        val linesWithBar = listOf(
            "|・|・|・|・|・|・|・|・|・|一",
            "|・|・|・|・|・|・|・|・|・|二",
            "|・|・|・|・|・|・|・|・|・|三",
            "|・|・|・|・|・|・|・|・|・|四",
            "|・|・|・|・|・|・|・|・|・|五",
            "|・|・|・|・|・|・|・|・|・|六",
            "|・|・|・|・|・|・|・|・|・|七",
            "|・|・|・|・|・|・|・|・|・|八",
            "|・|・|・|・|王|金|・|・|・|九",
            "1 ５八金(49)",
        )
        val state = ShogiBoardState()
        try {
            parseKifu(linesWithBar, state)
        } catch (e: Exception) {
            fail("パース失敗: ${e.message}")
        }
        val history = state.session.history
        assertFalse(history.isEmpty())
        val step0 = history[0]

        // デバッグ用: 9段目の内容をダンプ
        val row9 = (0..8).map { x -> step0.cells[8][x]?.first?.symbol ?: "・" }.joinToString("|")
        // assertEquals(Piece.OU to PieceColor.Black, step0.cells[8][4], "5九は玉であるべき (実際の内容: $row9)")

        // 5九は xIndex=4 (9-5)
        assertEquals(Piece.OU, step0.cells[8][4]?.first, "5九(xIndex=4)の駒 (実際の内容: $row9)")
        assertEquals(PieceColor.Black, step0.cells[8][4]?.second)
    }
}
