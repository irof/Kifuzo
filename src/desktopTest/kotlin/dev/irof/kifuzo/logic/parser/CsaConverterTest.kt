package dev.irof.kifuzo.logic.parser

import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsaConverterTest {

    @Test
    fun convertCsaToKifuLinesがメタデータを正しく変換できること() {
        val csaLines = listOf(
            "N+先手氏",
            "N-後手氏",
            "\$EVENT:テスト棋戦",
            "\$START_TIME:2026/03/02 12:00:00",
        )
        val kifLines = convertCsaToKifuLines(csaLines)

        assertTrue(kifLines.any { it == "先手：先手氏" })
        assertTrue(kifLines.any { it == "後手：後手氏" })
        assertTrue(kifLines.any { it == "棋戦：テスト棋戦" })
        assertTrue(kifLines.any { it == "開始日時：2026/03/02 12:00:00" })
    }

    @Test
    fun convertCsaToKifuLinesが指し手を正しく変換できること() {
        val csaLines = listOf(
            "+7776FU",
            "T1",
            "-3334FU",
            "T2",
            "+8822UM",
            "T3",
        )
        val kifLines = convertCsaToKifuLines(csaLines)

        // 1手目: ７六歩
        assertTrue(kifLines.any { it.contains("７六歩(77)") })
        // 2手目: ３四歩
        assertTrue(kifLines.any { it.contains("３四歩(33)") })
        // 3手目: ２二角成(88)
        assertTrue(kifLines.any { it.contains("２二角成(88)") })
    }

    @Test
    fun convertCsaToKifuLinesが同を正しく変換できること() {
        val csaLines = listOf(
            "+7776FU",
            "-3334FU",
            "+7675FU",
            "-3272HI",
            "+7574FU",
            "-7274HI", // 同じ 74 に移動
        )
        val kifLines = convertCsaToKifuLines(csaLines)
        assertTrue(kifLines.any { it.contains("同　飛(72)") })
    }

    @Test
    fun convertCsaToKifuLinesが結果を正しく変換できること() {
        val csaLines = listOf(
            "+7776FU",
            "%TORYO",
        )
        val kifLines = convertCsaToKifuLines(csaLines)
        assertTrue(kifLines.any { it.contains("投了") })
        assertTrue(kifLines.any { it.contains("まで1手で投了") })
    }

    @Test
    fun convertCsaToKifuLinesが不規則な持駒行を処理できること() {
        // P+00HIAL (全駒指定の AL はスキップされる)
        val csaLines = listOf("P+00HIAL")
        val kifLines = convertCsaToKifuLines(csaLines)
        assertTrue(kifLines.any { it.contains("先手持駒：飛") })

        // 空の持駒
        val csaEmpty = listOf("P+")
        val kifEmpty = convertCsaToKifuLines(csaEmpty)
        assertTrue(kifEmpty.any { it.contains("先手持駒：なし") })
    }

    @Test
    fun convertCsaToKifuLinesが未知の結果コードを無視すること() {
        val csaLines = listOf("%UNKNOWN")
        val kifLines = convertCsaToKifuLines(csaLines)
        // KIFヘッダーのみが残るはず
        assertEquals(1, kifLines.size)
    }

    @Test
    fun convertCsaToKifuがファイルを生成すること() {
        val dir = createTempDirectory("kifuzo-csa-conv")
        try {
            val csaFile = dir.resolve("test.csa")
            csaFile.writeText(
                """
                V2.2
                +7776FU
                %TORYO
                """.trimIndent(),
            )

            val kifuFile = convertCsaToKifu(csaFile)

            assertTrue(Files.exists(kifuFile))
            assertEquals("test.kifu", kifuFile.fileName.toString())
            val content = kifuFile.readText()
            assertTrue(content.contains("７六歩"))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
