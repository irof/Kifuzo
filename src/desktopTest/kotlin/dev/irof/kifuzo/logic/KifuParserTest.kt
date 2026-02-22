package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.PieceColor
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.models.Square
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("FunctionName")
class KifuParserTest {

    private fun parse(kifu: String): KifuSession {
        val state = ShogiBoardState()
        parseKifu(kifu.trimIndent().lines(), state)
        return state.session
    }

    private fun BoardSnapshot.at(file: Int, rank: Int): Pair<Piece, PieceColor>? {
        val s = Square(file, rank)
        return cells[s.yIndex][s.xIndex]
    }

    @Test
    fun 盤面図が含まれる棋譜を正しくパースできること() {
        val session = parse(
            """
                | ・v桂 ・v金 ・v玉 ・v桂v香|一
                | ・ ・ ・ ・ ・ ・ ・ 馬 ・|二
                | ・ ・ ・v飛 ・v金 ・v歩 ・|三
                | ・ ・v歩v銀 ・ ・v歩 ・v歩|四
                | ・ 歩 ・ ・ ・ ・ ・ ・ ・|五
                | ・ 馬 歩 ・ 歩 ・ ・ ・ 歩|六
                | 歩 ・ ・ ・ ・ 歩 歩 ・v龍|七
                | ・ ・ 玉 ・ ・ ・ ・ ・ ・|八
                | ・ ・ ・ ・ ・ ・ ・ ・ ・|九
                1 ３二銀打
                2 ５二玉(41)
            """,
        )

        // 0手目: 4一に玉(White)がいるか
        val step0 = session.history[0]
        assertEquals(Piece.OU to PieceColor.White, step0.at(4, 1))

        // 2手目: 4一の玉が5二へ移動できているか
        val step2 = session.history[2]
        assertNull(step2.at(4, 1))
        assertEquals(Piece.OU to PieceColor.White, step2.at(5, 2))
    }

    @Test
    fun 評価値コメントが含まれる棋譜から評価値を抽出できること() {
        val session = parse(
            """
                1 ７六歩(77)
                * 123
                2 ３四歩(33)
                * -456
                3 ２二角成(88)
                * +2000
                4 ４二銀(31)
                *#評価値=-500
            """,
        )

        assertEquals(123, session.history[1].evaluation)
        assertEquals(-456, session.history[2].evaluation)
        assertEquals(2000, session.history[3].evaluation)
        assertEquals(-500, session.history[4].evaluation)
    }

    @Test
    fun 詰みコメントから評価値を設定できること() {
        val session = parse(
            """
                1 ７六歩(77)
                *#詰み=先手勝ち
                2 ３四歩(33)
                *#詰み=先手勝ち:4手
                3 ２二角成(88)
                *#詰み=後手勝ち
                4 ４二銀(31)
                *#詰み=後手勝ち:11手
            """,
        )

        assertEquals(31111, session.history[1].evaluation)
        assertEquals(31111, session.history[2].evaluation)
        assertEquals(-31111, session.history[3].evaluation)
        assertEquals(-31111, session.history[4].evaluation)
    }

    @Test
    fun 評価値よりも詰み情報を優先すること() {
        val session = parse(
            """
                1 ７六歩(77)
                * 123
                *#詰み=先手勝ち
                2 ３四歩(33)
                *#詰み=後手勝ち
                * -456
            """,
        )

        // 1手目: 123 より 詰み(31111) を優先
        assertEquals(31111, session.history[1].evaluation)
        // 2手目: -456 より 詰み(-31111) を優先
        assertEquals(-31111, session.history[2].evaluation)
    }

    @Test
    fun 投了行から評価値を設定できること() {
        val session = parse(
            """
                1 ７六歩(77)
                2 ３四歩(33)
                3 投了
            """,
        )

        assertEquals(3, session.maxStep)
        // 3手目（投了）は先手の番なので、先手が負け -> 後手勝ち評価値
        assertEquals(-31111, session.history[3].evaluation)
        assertEquals("3 投了", session.history[3].lastMoveText)
    }

    @Test
    fun 対局者名や戦型情報をスキャンできること() {
        val info = scanKifuInfo(
            """
                先手：先手太郎
                後手：後手花子
                戦型：矢倉
                1 ７六歩(77)
            """.trimIndent().lines(),
        )
        assertEquals("先手太郎", info.senteName)
        assertEquals("後手花子", info.goteName)
        assertEquals("矢倉", info.senkei)
    }

    @Test
    fun 標準的な平手棋譜をパースできること() {
        val session = parse(
            """
                先手：先手
                後手：後手
                手数---指手---消費時間--
                1 ７六歩(77)
                2 ３四歩(33)
            """,
        )

        assertFalse(session.history.isEmpty())
        assertEquals(2, session.maxStep)
        assertEquals("先手", session.senteName)
        assertEquals("後手", session.goteName)

        // 1手目: 7六歩(77)
        val step1 = session.history[1]
        assertNull(step1.at(7, 7)) // 7七が空に
        assertEquals(Piece.FU to PieceColor.Black, step1.at(7, 6)) // 7六に歩

        // 2手目: 3四歩(33)
        val step2 = session.history[2]
        assertNull(step2.at(3, 3)) // 3三が空に
        assertEquals(Piece.FU to PieceColor.White, step2.at(3, 4)) // 3四に歩
    }

    @Test
    fun 駒取りと駒打ちを正しく処理できること() {
        val session = parse(
            """
                1 ７六歩(77)
                2 ３四歩(33)
                3 ２二角成(88)
                4 同　銀(31)
                5 ４五角打
            """,
        )

        // 3手目: 2二角成 (88から2二、2二には後手の角がいる)
        val step3 = session.history[3]
        assertEquals(Piece.UM to PieceColor.Black, step3.at(2, 2)) // 2二に馬
        assertEquals(listOf(Piece.KA), step3.senteMochigoma) // 後手の角をゲット

        // 4手目: 同 銀(31) (2二の馬を銀で取る)
        val step4 = session.history[4]
        assertEquals(Piece.GI to PieceColor.White, step4.at(2, 2)) // 2二に銀
        assertEquals(listOf(Piece.KA), step4.goteMochigoma) // 先手の馬を取って角が1枚に

        // 5手目: 4五角打
        val step5 = session.history[5]
        assertEquals(Piece.KA to PieceColor.Black, step5.at(4, 5)) // 4五に角
        assertEquals(emptyList<Piece>(), step5.senteMochigoma) // 1枚使ったので空に
    }

    @Test
    fun 盤面図からの初期配置をパースできること() {
        // 途中図からの開始（| で囲まれた配置）
        val session = parse(
            """
                |・|・|・|・|・|・|・|・|・|一
                |・|・|・|・|・|・|・|・|・|二
                |・|・|・|・|・|・|・|・|・|三
                |・|・|・|・|・|・|・|・|・|四
                |・|・|・|・|・|・|・|・|・|五
                |・|・|・|・|・|・|・|・|・|六
                |・|・|・|・|・|・|・|・|・|七
                |・|・|・|・|・|・|・|・|・|八
                |・|・|・|・|王|金|・|・|・|九
                1 ５八金(49)
            """,
        )
        val history = session.history
        assertFalse(history.isEmpty())
        val step0 = history[0]

        // 5九は xIndex=4 (9-5)
        assertEquals(Piece.OU to PieceColor.Black, step0.at(5, 9))
    }

    @Test
    fun 結果行やコメント行が含まれていても手数としてカウントすること() {
        val session = parse(
            """
                1 ７六歩(77)
                * この手は定跡です
                2 ３四歩(33)
                # ここから中盤です
                3 投了
                & その他付随情報
            """,
        )

        assertEquals(3, session.maxStep, "投了などの特殊行も手数としてカウントされること")
        assertEquals("1 ７六歩(77)", session.history[1].lastMoveText)
        assertEquals("2 ３四歩(33)", session.history[2].lastMoveText)
        assertEquals("3 投了", session.history[3].lastMoveText)
    }

    @Test
    fun 変化セクションの内容を無視すること() {
        val session = parse(
            """
                1 ７六歩(77)
                2 ３四歩(33)
                変化：2手
                2 ８四歩(83)
                3 ２六歩(27)
            """,
        )

        assertEquals(2, session.maxStep, "変化セクションの内容は本譜に含まれないこと")
        assertEquals("2 ３四歩(33)", session.history[2].lastMoveText)
    }

    @Test
    fun 成りや駒取りを含む詳細な手順をパースできること() {
        val session = parse(
            """
                1 ７六歩(77)
                2 ３四歩(33)
                3 ２二角成(88)
                4 ４二銀(31)
                5 ２一馬(22)
            """,
        )

        assertEquals(Piece.UM, session.history[3].at(2, 2)?.first, "2二が馬になっていること")
        assertEquals(Piece.GI, session.history[4].at(4, 2)?.first, "4二が銀になっていること")
        assertEquals(Piece.UM, session.history[5].at(2, 1)?.first, "2一が馬になっていること")
        assertEquals(listOf(Piece.KA, Piece.KE), session.history[5].senteMochigoma, "2二の角と2一の桂馬を取っていること")
    }

    @Test
    fun 初期持駒がある盤面図をパースできること() {
        val session = parse(
            """
                先手持駒：飛二 角
                後手持駒：金四 銀四
                |・|・|・|・|・|・|・|・|・|一
                |・|・|・|・|・|・|・|・|・|二
                |・|・|・|・|・|・|・|・|・|三
                |・|・|・|・|・|・|・|・|・|四
                |・|・|・|・|・|・|・|・|・|五
                |・|・|・|・|・|・|・|・|・|六
                |・|・|・|・|・|・|・|・|・|七
                |・|・|・|・|・|・|・|・|・|八
                |・|・|・|・|王|・|・|・|・|九
                1 ５八玉(59)
            """,
        )

        val step0 = session.history[0]
        // 飛車2枚、角1枚
        assertEquals(3, step0.senteMochigoma.size)
        assertEquals(2, step0.senteMochigoma.count { it == Piece.HI })
        assertEquals(1, step0.senteMochigoma.count { it == Piece.KA })

        // 金4枚、銀4枚
        assertEquals(8, step0.goteMochigoma.size)
        assertEquals(4, step0.goteMochigoma.count { it == Piece.KI })
        assertEquals(4, step0.goteMochigoma.count { it == Piece.GI })
    }

    @Test
    fun 同や打を含む特殊な表記をパースできること() {
        val session = parse(
            """
                1 ７六歩(77)
                2 ３四歩(33)
                3 同　歩(76)
                4 ５五角打
                5 ８八角成(55)
                6 同　銀(79)
            """,
        )

        val history = session.history
        // 3手目: 「同　歩」が直前の 3四(x=6, y=3) を指しているか
        val step3 = history[3]
        assertEquals(Piece.FU, step3.at(3, 4)?.first, "3四に歩が移動")

        // 4手目: 「打」のパース
        val step4 = history[4]
        assertEquals(Piece.KA, step4.at(5, 5)?.first, "5五に角が打たれている")

        // 6手目: 「同　銀」が直前の 8八(x=1, y=7) を指しているか
        val step6 = history[6]
        assertEquals(Piece.GI, step6.at(8, 8)?.first, "8八に銀が移動")
    }

    @Test
    fun 様々な終局結果を手数としてカウントできること() {
        // 様々な終局条件
        val results = listOf("投了", "持将棋", "千日手", "切れ負け", "反則負け")
        results.forEach { result ->
            val session = parse(
                """
                    1 ７六歩(77)
                    2 $result
                """,
            )
            assertEquals(2, session.maxStep, "$result が正しく終局手数としてカウントされること")
            assertTrue(session.history[2].lastMoveText.contains(result))
        }
    }
}
