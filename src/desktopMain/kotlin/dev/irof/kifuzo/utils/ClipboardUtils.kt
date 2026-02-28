package dev.irof.kifuzo.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private val logger = KotlinLogging.logger {}

fun copyToClipboard(text: String) {
    val selection = StringSelection(text)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
}

fun getFromClipboard(): String? = try {
    Toolkit.getDefaultToolkit().systemClipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as String
} catch (e: java.awt.datatransfer.UnsupportedFlavorException) {
    logger.debug(e) { "Clipboard does not contain string flavor" }
    null
} catch (e: java.io.IOException) {
    logger.debug(e) { "Failed to get text from clipboard" }
    null
}
