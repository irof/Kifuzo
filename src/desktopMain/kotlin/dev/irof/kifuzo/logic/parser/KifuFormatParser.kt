package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState

/**
 * 特定の棋譜形式（KIF, CSAなど）をパースするためのインターフェース。
 */
interface KifuFormatParser {
    /**
     * 行リストから棋譜をパースし、ShogiBoardState を更新します。
     */
    fun parse(lines: List<String>, state: ShogiBoardState, warningMessage: String? = null)

    /**
     * 行リストから棋譜の簡易情報をスキャンします。
     */
    fun scanInfo(lines: List<String>): KifuInfo
}
