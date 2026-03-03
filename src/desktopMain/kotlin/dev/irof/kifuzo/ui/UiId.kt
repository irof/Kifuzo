package dev.irof.kifuzo.ui

/**
 * UI 要素を識別するための ID。
 *
 * プロダクトコード内で UI 構造を特定したり、自動化ツールやテストから
 * 要素を識別するために使用されます。
 */
object UiId {
    /** サイドバー全体のコンテナ */
    const val SIDEBAR = "sidebar"

    /** 指し手リスト全体のコンテナ */
    const val MOVE_LIST = "move_list"

    /** メニューバーの各操作ボタンを識別するためのプレフィックス */
    object Menu {
        const val TOGGLE_SIDEBAR = "menu_toggle_sidebar"
        const val TOGGLE_MOVE_LIST = "menu_toggle_move_list"
    }
}
