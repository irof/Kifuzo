package dev.irof.kfv.logic

import dev.irof.kfv.models.*
import kotlin.test.*

class KifuParserTest {

    @Test
    fun testParseKifuWithEvaluations() {
        val lines = listOf(
            "1 ７六歩(77)",
            "* 123",
            "2 ３四歩(33)",
            "* -456",
            "3 ２二角成(88)",
            "* +2000",
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val session = state.session
        assertEquals(123, session.history[1].evaluation)
        assertEquals(-456, session.history[2].evaluation)
        assertEquals(2000, session.history[3].evaluation)
    }

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

    @Test
    fun testParseKifuWithResultAndComments() {
        val lines = listOf(
            "1 ７六歩(77)",
            "* この手は定跡です",
            "2 ３四歩(33)",
            "# ここから中盤です",
            "3 投了",
            "& その他付随情報",
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val session = state.session
        assertEquals(2, session.maxStep, "投了などの特殊行は手数としてカウントされないこと")
        assertEquals("1 ７六歩(77)", session.history[1].lastMoveText)
        assertEquals("2 ３四歩(33)", session.history[2].lastMoveText)
    }

    @Test
    fun testParseKifuWithVariations() {
        val lines = listOf(
            "1 ７六歩(77)",
            "2 ３四歩(33)",
            "変化：2手",
            "2 ８四歩(83)",
            "3 ２六歩(27)",
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val session = state.session
        assertEquals(2, session.maxStep, "変化セクションの内容は本譜に含まれないこと")
        assertEquals("2 ３四歩(33)", session.history[2].lastMoveText)
    }

    @Test
    fun testParseKifuPromotionDetails() {
        val lines = listOf(
            "1 ７六歩(77)",
            "2 ３四歩(33)",
            "3 ２二角成(88)", // 通常の成り
            "4 ４二銀(31)", // 3一の銀が4二へ
            "5 ２一馬(22)", // 2二の馬が2一の桂馬を取る
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val session = state.session
        assertEquals(Piece.UM, session.history[3].cells[1][7]?.first, "2二が馬になっていること")
        assertEquals(Piece.GI, session.history[4].cells[1][5]?.first, "4二(xIndex=5, yIndex=1)が銀になっていること")
        assertEquals(Piece.UM, session.history[5].cells[0][7]?.first, "2一が馬になっていること")
        assertEquals(listOf(Piece.KA, Piece.KE), session.history[5].senteMochigoma, "2二の角と2一の桂馬を取っていること")
    }

    @Test
    fun testParseKifuInitialMochigoma() {
        val lines = listOf(
            "先手持駒：飛二 角",
            "後手持駒：金四 銀四",
            "|・|・|・|・|・|・|・|・|・|一",
            "|・|・|・|・|・|・|・|・|・|二",
            "|・|・|・|・|・|・|・|・|・|三",
            "|・|・|・|・|・|・|・|・|・|四",
            "|・|・|・|・|・|・|・|・|・|五",
            "|・|・|・|・|・|・|・|・|・|六",
            "|・|・|・|・|・|・|・|・|・|七",
            "|・|・|・|・|・|・|・|・|・|八",
            "|・|・|・|・|王|・|・|・|・|九",
            "1 ５八玉(59)",
        )
        val state = ShogiBoardState()
        parseKifu(lines, state)

        val step0 = state.session.history[0]
        // 飛車2枚、角1枚
        assertEquals(3, step0.senteMochigoma.size)
        assertEquals(2, step0.senteMochigoma.count { it == Piece.HI })
        assertEquals(1, step0.senteMochigoma.count { it == Piece.KA })

        // 金4枚、銀4枚
        assertEquals(8, step0.goteMochigoma.size)
        assertEquals(4, step0.goteMochigoma.count { it == Piece.KI })
        assertEquals(4, step0.goteMochigoma.count { it == Piece.GI })
    }
}
