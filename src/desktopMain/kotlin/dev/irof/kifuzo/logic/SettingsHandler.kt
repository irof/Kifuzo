package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.ShogiBoardState
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.regex.PatternSyntaxException

private val logger = KotlinLogging.logger {}

/**
 * アプリケーション設定とそれに関連する副作用をハンドルするクラス。
 */
class SettingsHandler(
    private val boardState: ShogiBoardState,
    private val onAutoFlip: (Boolean) -> Unit,
) {
    fun saveSettings(newRegex: String, newTemplate: String) {
        AppSettings.myNameRegex = newRegex
        AppSettings.filenameTemplate = newTemplate
        updateAutoFlip(newRegex)
    }

    fun updateAutoFlip(myNameRegex: String) {
        if (myNameRegex.isEmpty()) return
        val regex = try {
            Regex(myNameRegex)
        } catch (e: PatternSyntaxException) {
            logger.warn(e) { "Invalid regex pattern: $myNameRegex" }
            null
        } ?: return

        val goteMatch = regex.containsMatchIn(boardState.session.goteName)
        val senteMatch = regex.containsMatchIn(boardState.session.senteName)

        if (goteMatch && !senteMatch) {
            onAutoFlip(true)
        } else if (senteMatch) {
            onAutoFlip(false)
        }
    }
}
