package dev.irof.kifuzo.logic.parser.csa

import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.KifuTestData
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.Piece
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsaParserTest {

    @Test
    fun CSAの評価値コメントを抽出できること() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_EVALUATION.lines(), state)

        // 1手目
        assertEquals(Evaluation.Score(123), state.session.moves[0].evaluation)
        assertEquals("1 ７六歩", state.session.moves[0].moveText)
        // 2手目
        assertEquals(Evaluation.Score(-456), state.session.moves[1].evaluation)
        assertEquals("2 ３四歩", state.session.moves[1].moveText)
        // 3手目
        assertEquals(Evaluation.SenteWin, state.session.moves[2].evaluation)
        assertEquals("3 ２二角成", state.session.moves[2].moveText)
    }

    @Test
    fun 同の表記を生成できること() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_DOU_1.lines(), state)

        // 3手目が 22 なので、2手目の 34 とは異なるため通常の座標が表示される
        assertEquals("3 ２二角", state.session.moves[2].moveText)

        val stateSame = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_DOU_2.lines(), stateSame)

        // 4手目(-3122GI)が3手目(+8822KA+)と同じ座標(22)に移動するので「同」になる
        assertEquals("4 同　銀", stateSame.session.moves[3].moveText)
    }

    @Test
    fun 駒が取られた時にfirstContactStepが設定されること() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_FIRST_CONTACT.lines(), state)

        // 3手目(+8822KA+)で角が取られるので、firstContactStepは3になるはず
        assertEquals(3, state.session.firstContactStep)
        // initialStep も衝突手数を優先する
        assertEquals(3, state.session.initialStep)
    }

    @Test
    fun CSA形式での駒の成りが正しく処理されること() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_PROMOTION.lines(), state)

        // 1手目(+7776TO)で歩が「と」になるので、「成」が付くはず
        assertEquals("1 ７六歩成", state.session.moves[0].moveText)
        assertEquals(Piece.TO, state.session.moves[0].resultSnapshot.cells[5][2]?.piece)
        assertEquals("第1期蔵王戦", state.session.event)
        assertEquals("2026/02/24 10:00:00", state.session.startTime)
    }

    @Test
    fun 不完全な指し手行が含まれていてもエラーにならないこと() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_INCOMPLETE.lines(), state)

        // 初期局面(initialSnapshot) 以外の指し手(moves)が2手
        assertEquals(2, state.session.moves.size)
    }

    @Test
    fun CSA形式の初期持駒が正しく処理されること() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_INITIAL_MOCHI.lines(), state)

        val initialSnapshot = state.session.initialSnapshot
        assertEquals(listOf(Piece.HI, Piece.KA), initialSnapshot.senteMochigoma)
        assertEquals(listOf(Piece.KI, Piece.GI), initialSnapshot.goteMochigoma)
    }

    @Test
    fun 途中局面開始のCSAでは初期手数が0になること() {
        val state = ShogiBoardState()
        CsaParser().parse(KifuTestData.CSA_MID_GAME_START.lines(), state)

        // 3手目 (+8822UM) で 22の飛車 を取るので衝突が発生する
        assertEquals(3, state.session.firstContactStep)
        // 途中局面開始なので、初期手数は 0 であるべき
        assertEquals(0, state.session.initialStep, "途中局面開始（isStandardStart=false）の場合は開始局面(0)を優先すべき")
    }

    @Test
    fun formatResultが結果行を正しく追加および更新できること() {
        val parser = CsaParser()
        val lines = mutableListOf("+7776FU")

        parser.formatResult(lines, "投了")
        assertEquals("%TORYO", lines.last())

        parser.formatResult(lines, "中断")
        assertEquals("%CHUDAN", lines.last())
        assertEquals(2, lines.size)
    }

    @Test
    fun isValidCsaMoveが正しく判定すること() {
        val parser = CsaParser()
        // 正常
        // 内部的に private なので parse を通じて確認するか、リフレクション等が必要
        // ここでは parse 時のエラーとして確認
        val state = ShogiBoardState()
        kotlin.test.assertFailsWith<KifuParseException> {
            parser.parse(listOf("+7A76FU"), state) // Aは数字ではない
        }
    }

    @Test
    fun handleCsaResultLineが未知のコードを適切に処理すること() {
        val state = ShogiBoardState()
        val csaLines = listOf(
            "+7776FU",
            "%UNKNOWN_CODE",
        )
        CsaParser().parse(csaLines, state)
        // 1手完結 + 結果行も1手(Move)として追加される
        assertEquals(2, state.session.moves.size)
        // 最後に結果テキストがセットされているはず
        assertTrue(state.session.moves.last().moveText.contains("UNKNOWN_CODE"))
    }

    @Test
    fun parseが短すぎる指し手行で例外を投げること() {
        val state = ShogiBoardState()
        kotlin.test.assertFailsWith<KifuParseException> {
            CsaParser().parse(listOf("+7776F"), state) // 長さが足りない
        }
    }
}
