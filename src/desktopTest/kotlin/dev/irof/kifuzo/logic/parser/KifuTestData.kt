package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.readResource

/**
 * テストで使用する棋譜データのパスを管理するオブジェクト。
 * 実際のデータは src/desktopTest/resources/kif/ または csa/ に配置されています。
 */
object KifuTestData {
    val BASIC_KIFU get() = readResource("kif/basic.kif")
    val KIFU_WITH_BOARD get() = readResource("kif/with_board.kif")
    val KIFU_WITH_EVALUATION get() = readResource("kif/with_evaluation.kif")
    val KIFU_WITH_MATE get() = readResource("kif/with_mate.kif")
    val KIFU_WITH_CAPTURE_AND_DROP get() = readResource("kif/with_capture_and_drop.kif")
    val KIFU_WITH_VARIATION get() = readResource("kif/with_variation.kif")
    val KIFU_WITH_INITIAL_MOCHI get() = readResource("kif/with_initial_mochi.kif")
    val KIFU_WITH_MIXED_COMMENTS get() = readResource("kif/with_mixed_comments.kif")
    val KIFU_WITH_SPECIAL_NOTATION get() = readResource("kif/with_special_notation.kif")
    val KIFU_WITH_COMPLEX_MOVES get() = readResource("kif/with_complex_moves.kif")
    val KIFU_WITH_PLAYER_INFO get() = readResource("kif/with_player_info.kif")
    val KIFU_COMPLEX_VARIATION get() = readResource("kif/complex_variation.kif")
    val KIFU_VARIATION_WITH_DOU get() = readResource("kif/variation_with_dou.kif")

    val MID_GAME_START get() = readResource("kif/mid_game_start.kif")
    val VARIATION_MOCHIGOMA get() = readResource("kif/variation_mochigoma.kif")
    val NESTED_VARIATION get() = readResource("kif/nested_variation.kif")
    val MULTIPLE_VARIATIONS get() = readResource("kif/multiple_variations.kif")
    val INCOMPLETE_BOARD get() = readResource("kif/incomplete_board.kif")
    val FULL_METADATA get() = readResource("kif/full_metadata.kif")
    val COMPLEX_BOARD get() = readResource("kif/complex_board.kif")
    val MIXED_FORMAT get() = readResource("kif/mixed_format.kif")

    val SIMPLE_CSA get() = readResource("csa/simple.csa")
    val CSA_EVALUATION get() = readResource("csa/evaluation.csa")
    val CSA_DOU_1 get() = readResource("csa/dou_notation_1.csa")
    val CSA_DOU_2 get() = readResource("csa/dou_notation_2.csa")
    val CSA_FIRST_CONTACT get() = readResource("csa/first_contact.csa")
    val CSA_PROMOTION get() = readResource("csa/promotion.csa")
    val CSA_INCOMPLETE get() = readResource("csa/incomplete_moves.csa")
    val CSA_INITIAL_MOCHI get() = readResource("csa/initial_mochi.csa")
    val CSA_MID_GAME_START get() = readResource("csa/mid_game_start.csa")
    val CSA_INVALID_DROP get() = readResource("csa/invalid_drop.csa")

    val CSA_FULL_SESSION get() = readResource("csa/full_session.csa")
    val CSA_PROMOTION_SEQ get() = readResource("csa/promotion_sequential.csa")
    val CSA_PROMOTION_AFTER get() = readResource("csa/promotion_after.csa")
    val CSA_KA_PROM_1 get() = readResource("csa/ka_promotion_1.csa")
    val CSA_KA_PROM_2 get() = readResource("csa/ka_promotion_2.csa")
    val CSA_DROP_PROM get() = readResource("csa/drop_then_promote.csa")
    val CSA_PROM_MOVE get() = readResource("csa/promote_then_move.csa")
    val CSA_BOARD_SETUP_PROM get() = readResource("csa/board_setup_promote.csa")

    val EVALUATION_REGRESSION_1 get() = readResource("kif/evaluation_regression_1.kif")
    val EVALUATION_REGRESSION_2 get() = readResource("kif/evaluation_regression_2.kif")
}
