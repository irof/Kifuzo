package dev.irof.kifuzo.logic.parser
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.handler.*
import dev.irof.kifuzo.logic.io.*
import dev.irof.kifuzo.logic.parser.*
import dev.irof.kifuzo.logic.service.*

object KifuTestData {
    val BASIC_KIFU = """
        先手：先手
        後手：後手
        手数---指手---消費時間--
        1 ７六歩(77)   ( 0:01/00:00:01)
        2 ３四歩(33)   ( 0:02/00:00:02)
    """.trimIndent()

    val KIFU_WITH_BOARD = """
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
    """.trimIndent()

    val KIFU_WITH_EVALUATION = """
        1 ７六歩(77)
        * 123
        2 ３四歩(33)
        * -456
        3 ２二角成(88)
        * +2000
        4 ４二銀(31)
        *#評価値=-500
    """.trimIndent()

    val KIFU_WITH_MATE = """
        1 ７六歩(77)
        *#詰み=先手勝ち
        2 ３四歩(33)
        *#詰み=先手勝ち:4手
        3 ２二角成(88)
        *#詰み=後手勝ち
        4 ４二銀(31)
        *#詰み=後手勝ち:11手
    """.trimIndent()

    val KIFU_WITH_CAPTURE_AND_DROP = """
        1 ７六歩(77)
        2 ３四歩(33)
        3 ２二角成(88)
        4 同　銀(31)
        5 ４五角打
    """.trimIndent()

    val KIFU_WITH_VARIATION = """
        1 ７六歩(77)
        2 ３四歩(33)
        変化：2手
        2 ８四歩(83)
        3 ２六歩(27)
    """.trimIndent()

    val KIFU_WITH_INITIAL_MOCHI = """
        先手持駒：飛二 角
        後手持駒：金四 銀四
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|一
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|二
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|三
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|四
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|五
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|六
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|七
        | ・ ・ ・ ・ ・ ・ ・ ・ ・|八
        | ・ ・ ・ ・ 王 ・ ・ ・ ・|九
        1 ５八玉(59)
    """.trimIndent()

    val KIFU_WITH_MIXED_COMMENTS = """
        1 ７六歩(77)
        * この手は定跡です
        2 ３四歩(33)
        # ここから中盤です
        3 投了
        & その他付随情報
    """.trimIndent()

    val KIFU_WITH_SPECIAL_NOTATION = """
        1 ７六歩(77)
        2 ３四歩(33)
        3 同　歩(76)
        4 ５五角打
        5 ８八角成(55)
        6 同　銀(79)
    """.trimIndent()

    val KIFU_WITH_COMPLEX_MOVES = """
        1 ７六歩(77)
        2 ３四歩(33)
        3 ２二角成(88)
        4 ４二銀(31)
        5 ２一馬(22)
    """.trimIndent()

    val KIFU_WITH_PRIORITY_MATE = """
        1 ７六歩(77)
        * 123
        *#詰み=先手勝ち
        2 ３四歩(33)
        *#詰み=後手勝ち
        * -456
    """.trimIndent()

    val KIFU_WITH_PLAYER_INFO = """
        棋戦：第1期蔵王戦
        開始日時：2026/02/24 10:00:00
        先手：先手太郎
        後手：後手花子
        1 ７六歩(77)
    """.trimIndent()

    val KIFU_COMPLEX_VARIATION = """
        1 ７六歩(77)
        2 ３四歩(33)
        3 ６六角(88)
        4 ４二銀(31)
        5 ６五角(66)
        変化：5手
        5 ７七角(66)
        6 ４四歩(43)
    """.trimIndent()

    val KIFU_VARIATION_WITH_DOU = """
        1 ７六歩(77)
        2 ３四歩(33)
        3 ２二角成(88)
        変化：4手
        4 同　銀(31)
    """.trimIndent()
}
