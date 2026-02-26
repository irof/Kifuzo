package dev.irof.kifuzo.logic

import kotlin.test.Test
import kotlin.test.assertTrue

class CsaConverterTest {

    @Test
    fun CSA形式の行リストをKIF形式に変換できること() {
        val csaLines = """
            V2.2
            N+SenteUser
            N-GoteUser
            ${'$'}EVENT:Tournament
            ${'$'}SITE:Online
            ${'$'}START_TIME:2026/02/21 12:00:00
            ${'$'}OPENING:Yagura
            ${'$'}TIME_LIMIT:00:30+00
            +7776FU
            T3
            -3334FU
            T5
            +8822KA+
            T10
            -3122GI
            T15
            %TORYO
        """.trimIndent().lines()

        val kifLines = convertCsaToKifuLines(csaLines)

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
        val csaLines = """
            P+00HI00KA
            P-00KI00GI
            +7776FU
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)
        // 先手持駒：飛　角
        assertTrue(kifLines.any { it == "先手持駒：飛　角" }, "先手持駒が変換されていること")
        // 後手持駒：金　銀
        assertTrue(kifLines.any { it == "後手持駒：金　銀" }, "後手持駒が変換されていること")
    }

    @Test
    fun 成り駒への成りを正しく処理できること() {
        val csaLines = """
            +7776FU
            -3334FU
            +7675FU
            -3435FU
            +7574FU
            -3536FU
            +7473TO
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)

        // 7手目が「７三歩成」となっていること (「７三と」ではない)
        assertTrue(kifLines.any { it.contains("7 ７三歩成(74)") }, "7手目が「７三歩成」であること")

        val csaLines2 = """
            +7776FU
            -3334FU
            +7675FU
            -3435FU
            +7574FU
            -3536FU
            +7473TO
            -3132GI
            +7363TO
        """.trimIndent().lines()
        val kifLines2 = convertCsaToKifuLines(csaLines2)
        assertTrue(kifLines2.any { it.contains("7 ７三歩成(74)") }, "成った瞬間は「歩成」")
        assertTrue(kifLines2.any { it.contains("9 ６三と(73)") }, "成った後の移動は「と」")
    }

    @Test
    fun 角の成りを正しく処理できること() {
        val csaLines = """
            +7776FU
            -3334FU
            +8822UM
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)
        // 3手目が「２二角成(88)」となっていること
        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること")

        val csaLines2 = """
            +7776FU
            -3334FU
            +8822KA+
        """.trimIndent().lines()
        val kifLines2 = convertCsaToKifuLines(csaLines2)
        assertTrue(kifLines2.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること(KA+)")
    }

    @Test
    fun 駒打ち直後の成りを正しく処理できること() {
        val csaLines = """
            +0022KA
            -3132GI
            +2211UM
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)
        // 3手目が「１一角成(22)」となっていること
        assertTrue(kifLines.any { it.contains("3 １一角成(22)") }, "打った角が成った時に「角成」と表示されること")
    }

    @Test
    fun 成りマーカーを伴う移動で盤面が正しく更新されること() {
        val csaLines = """
            +7776FU
            -3334FU
            +8822KA+
            -3132GI
            +2211UM
        """.trimIndent().lines()
        // 3手目 +8822KA+ で角が成る
        // 5手目 +2211UM で22にある駒(3手目で成った角=馬)が移動する
        val kifLines = convertCsaToKifuLines(csaLines)

        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること")
        // 5手目が「１一馬(22)」であること（「１一角成」ではない）
        assertTrue(kifLines.any { it.contains("5 １一馬(22)") }, "5手目が「馬」であること(既に成っているため)")
    }

    @Test
    fun 盤面状態が不明な場合でも成りを正しく推測できること() {
        // 初期配置にはないはずの場所での移動
        val csaLines = """
            +5554TO
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)
        // 55にとがいることは初期配置からは分からないが、TO(と)に移動したので「歩成」と推測する
        assertTrue(kifLines.any { it.contains("1 ５四歩成(55)") })
    }

    @Test
    fun 盤面設定行がある場合に正しく成りを判定できること() {
        val csaLines = """
            P1-KY-KE-GI-KI-OU-KI-GI-KE-KY
            P2 * -HI *  *  *  *  * -KA *
            P3-FU-FU-FU-FU-FU-FU-FU-FU-FU
            P4 *  *  *  *  *  *  *  *  *
            P5 *  *  *  *  *  *  *  *  *
            P6 *  *  *  *  *  *  *  *  *
            P7+FU+FU+FU+FU+FU+FU+FU+FU+FU
            P8 * +KA *  *  *  *  * +HI *
            P9+KY+KE+GI+KI+OU+KI+GI+KE+KY
            +7776FU
            -3334FU
            +8822KA+
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)
        // 12手目(CSAでの3手目) +8822KA+ が「２二角成」となること
        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") })
    }

    @Test
    fun 終局コードを正しく変換できること() {
        val csaLines = """
            +7776FU
            %TIME_UP
        """.trimIndent().lines()
        val kifLines = convertCsaToKifuLines(csaLines)
        // 2手目に「切れ負け」があること
        assertTrue(kifLines.any { it.contains("2 切れ負け") }, "2手目が「切れ負け」であること")
        // summary行があること
        assertTrue(kifLines.any { it.contains("まで1手でタイムアップ") }, "タイムアップの要約行があること")

        val csaLines2 = """
            +7776FU
            -3334FU
            %TORYO
        """.trimIndent().lines()
        val kifLines2 = convertCsaToKifuLines(csaLines2)
        // 3手目に「投了」があること
        assertTrue(kifLines2.any { it.contains("3 投了") }, "3手目が「投了」であること")
        // summary行があること
        assertTrue(kifLines2.any { it.contains("まで2手で投了") }, "投了の要約行があること")
    }
}
