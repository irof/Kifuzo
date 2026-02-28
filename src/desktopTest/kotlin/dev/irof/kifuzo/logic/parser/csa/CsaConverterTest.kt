package dev.irof.kifuzo.logic.parser.csa

import dev.irof.kifuzo.logic.parser.KifuTestData
import dev.irof.kifuzo.logic.parser.convertCsaToKifuLines
import kotlin.test.Test
import kotlin.test.assertTrue

class CsaConverterTest {

    @Test
    fun CSA形式の行リストをKIF形式に変換できること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_FULL_SESSION.lines())

        // ヘッダー確認
        assertTrue(kifLines.any { it.startsWith("先手：SenteUser") })
        assertTrue(kifLines.any { it.startsWith("後手：GoteUser") })
        assertTrue(kifLines.any { it.startsWith("棋戦：Tournament") })
        assertTrue(kifLines.any { it.startsWith("場所：Online") })
        assertTrue(kifLines.any { it.startsWith("開始日時：2026/02/21 12:00:00") })
        assertTrue(kifLines.any { it.startsWith("戦型：Yagura") })
        assertTrue(kifLines.any { it.startsWith("持ち時間：00:30+00") })

        // 指し手確認
        // 1手目: 7776FU -> 7六歩(77)
        assertTrue(kifLines.any { it.contains("1 ７六歩(77)") })
        // 2手目: 3334FU -> 3四歩(33)
        assertTrue(kifLines.any { it.contains("2 ３四歩(33)") })
        // 3手目: 8822KA+ -> 2二角成(88)
        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") })
        // 4手目: 3122GI -> 同　銀(31) (移動先が2二で3手目と同じ)
        assertTrue(kifLines.any { it.contains("4 同　銀(31)") })

        // 終局確認
        assertTrue(kifLines.any { it.contains("まで4手で投了") })
    }

    @Test
    fun CSAの初期持駒をKIFに変換できること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_INITIAL_MOCHI.lines())
        // 先手持駒：飛　角
        assertTrue(kifLines.any { it == "先手持駒：飛　角" }, "先手持駒が変換されていること")
        // 後手持駒：金　銀
        assertTrue(kifLines.any { it == "後手持駒：金　銀" }, "後手持駒が変換されていること")
    }

    @Test
    fun 成り駒への成りを正しく処理できること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_PROMOTION_SEQ.lines())

        // 7手目が「７三歩成」となっていること (「７三と」ではない)
        assertTrue(kifLines.any { it.contains("7 ７三歩成(74)") }, "7手目が「７三歩成」であること")

        val kifLines2 = convertCsaToKifuLines(KifuTestData.CSA_PROMOTION_AFTER.lines())
        assertTrue(kifLines2.any { it.contains("7 ７三歩成(74)") }, "成った瞬間は「歩成」")
        assertTrue(kifLines2.any { it.contains("9 ６三と(73)") }, "成った後の移動は「と」")
    }

    @Test
    fun 角の成りを正しく処理できること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_KA_PROM_1.lines())
        // 3手目が「２二角成(88)」となっていること
        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること")

        val kifLines2 = convertCsaToKifuLines(KifuTestData.CSA_KA_PROM_2.lines())
        assertTrue(kifLines2.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること(KA+)")
    }

    @Test
    fun 駒打ち直後の成りを正しく処理できること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_DROP_PROM.lines())
        // 3手目が「１一角成(22)」となっていること
        assertTrue(kifLines.any { it.contains("3 １一角成(22)") }, "打った角が成った時に「角成」と表示されること")
    }

    @Test
    fun 成りマーカーを伴う移動で盤面が正しく更新されること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_PROM_MOVE.trim().lines())

        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること")
        // 4手目: -3132GI (32銀)
        // 5手目: +2211UM (1一馬)
        assertTrue(kifLines.any { it.contains("5 １一馬(22)") }, "5手目が「馬」であること(既に成っているため)")
    }

    @Test
    fun 盤面状態が不明な場合でも成りを正しく推測できること() {
        // 初期配置にはないはずの場所での移動
        val csaLines = listOf("+5554TO")
        val kifLines = convertCsaToKifuLines(csaLines)
        // 55にとがいることは初期配置からは分からないが、TO(と)に移動したので「歩成」と推測する
        assertTrue(kifLines.any { it.contains("1 ５四歩成(55)") })
    }

    @Test
    fun 盤面設定行がある場合に正しく成りを判定できること() {
        val kifLines = convertCsaToKifuLines(KifuTestData.CSA_BOARD_SETUP_PROM.lines())
        // 12手目(CSAでの3手目) +8822KA+ が「２二角成」となること
        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") })
    }

    @Test
    fun 終局コードを正しく変換できること() {
        val csaLines = listOf("+7776FU", "%TIME_UP")
        val kifLines = convertCsaToKifuLines(csaLines)
        // 2手目に「切れ負け」があること
        assertTrue(kifLines.any { it.contains("2 切れ負け") }, "2手目が「切れ負け」であること")
        // summary行があること
        assertTrue(kifLines.any { it.contains("まで1手でタイムアップ") }, "タイムアップの要約行があること")

        val csaLines2 = listOf("+7776FU", "-3334FU", "%TORYO")
        val kifLines2 = convertCsaToKifuLines(csaLines2)
        // 3手目に「投了」があること
        assertTrue(kifLines2.any { it.contains("3 投了") }, "3手目が「投了」であること")
        // summary行があること
        assertTrue(kifLines2.any { it.contains("まで2手で投了") }, "投了の要約行があること")
    }
}
