package dev.irof.kifuzo.logic

import kotlin.test.Test
import kotlin.test.assertTrue

class CsaConverterTest {

    @Test
    fun testConvertCsaToKifuLines() {
        val csaLines = listOf(
            "V2.2",
            "N+SenteUser",
            "N-GoteUser",
            "\$START_TIME:2026/02/21 12:00:00",
            "+7776FU",
            "T3",
            "-3334FU",
            "T5",
            "+8822KA+",
            "T10",
            "-3122GI",
            "T15",
            "%TORYO",
        )

        val kifLines = convertCsaToKifuLines(csaLines)

        // ヘッダー確認
        assertTrue(kifLines.any { it.startsWith("先手：SenteUser") })
        assertTrue(kifLines.any { it.startsWith("後手：GoteUser") })
        assertTrue(kifLines.any { it.startsWith("開始日時：2026/02/21 12:00:00") })

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
    fun testConvertCsaToPromotedMove() {
        val csaLines = listOf(
            "+7776FU", // 1. 7六歩
            "-3334FU", // 2. 3四歩
            "+7675FU", // 3. 7五歩
            "-3435FU", // 4. 3五歩
            "+7574FU", // 5. 7四歩
            "-3536FU", // 6. 3六歩
            "+7473TO", // 7. 7三歩成 (成る: CSAではTO)
        )
        val kifLines = convertCsaToKifuLines(csaLines)

        // 7手目が「７三歩成」となっていること (「７三と」ではない)
        assertTrue(kifLines.any { it.contains("7 ７三歩成(74)") }, "7手目が「７三歩成」であること")

        val csaLines2 = listOf(
            "+7776FU",
            "-3334FU",
            "+7675FU",
            "-3435FU",
            "+7574FU",
            "-3536FU",
            "+7473TO", // 7. 7三歩成 (CSAではTO)
            "-3132GI",
            "+7363TO", // 9. 6三と (成り駒の移動)
        )
        val kifLines2 = convertCsaToKifuLines(csaLines2)
        assertTrue(kifLines2.any { it.contains("7 ７三歩成(74)") }, "成った瞬間は「歩成」")
        assertTrue(kifLines2.any { it.contains("9 ６三と(73)") }, "成った後の移動は「と」")
    }

    @Test
    fun testConvertCsaCornerPromotion() {
        val csaLines = listOf(
            "+7776FU", // 1. 7六歩
            "-3334FU", // 2. 3四歩
            "+8822UM", // 3. 2二角成 (8八の角が2二に移動して成る)
        )
        val kifLines = convertCsaToKifuLines(csaLines)
        // 3手目が「２二角成(88)」となっていること
        assertTrue(kifLines.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること")

        val csaLines2 = listOf(
            "+7776FU",
            "-3334FU",
            "+8822KA+", // 末尾に + がある形式も確認
        )
        val kifLines2 = convertCsaToKifuLines(csaLines2)
        assertTrue(kifLines2.any { it.contains("3 ２二角成(88)") }, "3手目が「角成」であること(KA+)")
    }

    @Test
    fun testConvertCsaDropAndPromote() {
        val csaLines = listOf(
            "+0022KA", // 1. 2二角打
            "-3132GI", // 2. 3二銀
            "+2211UM", // 3. 1一角成 (2二の角が1一へ移動して成る)
        )
        val kifLines = convertCsaToKifuLines(csaLines)
        // 3手目が「１一角成(22)」となっていること
        assertTrue(kifLines.any { it.contains("3 １一角成(22)") }, "打った角が成った時に「角成」と表示されること")
    }

    @Test
    fun testConvertCsaDrop() {
        val csaLines = listOf(
            "+0045KA",
            "T5",
        )
        val kifLines = convertCsaToKifuLines(csaLines)
        // 0045KA -> 4五角打
        assertTrue(kifLines.any { it.contains("1 ４五角打") })
    }
}
