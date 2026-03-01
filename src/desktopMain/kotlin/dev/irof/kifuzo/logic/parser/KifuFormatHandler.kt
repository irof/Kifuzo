package dev.irof.kifuzo.logic.parser

import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState

/**
 * 特定の棋譜形式（KIF, CSAなど）を処理するためのインターフェース。
 * パース、スキャン、およびファイル内容の更新ロジックをカプセル化します。
 */
interface KifuFormatHandler {
    /**
     * 行リストから棋譜をパースし、ShogiBoardState を更新します。
     */
    fun parse(lines: List<String>, state: ShogiBoardState, warningMessage: String? = null)

    /**
     * 行リストから棋譜の簡易情報をスキャンします。
     */
    fun scanInfo(lines: List<String>): KifuInfo

    /**
     * 指定されたメタデータ（棋戦名、開始日時）でヘッダー行を更新または追加します。
     */
    fun formatHeader(lines: MutableList<String>, event: String, startTime: String)

    /**
     * 指定された終局結果で行リストを更新または追加します。
     */
    fun formatResult(lines: MutableList<String>, result: String)
}
