package dev.irof.kfv.models

import java.nio.file.Path

/**
 * 棋譜の要約情報
 */
data class KifuInfo(
    val path: Path,
    val senteName: String = "",
    val goteName: String = "",
    val senkei: String = "",
    val isError: Boolean = false,
)
