package dev.irof.kfv.logic

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
