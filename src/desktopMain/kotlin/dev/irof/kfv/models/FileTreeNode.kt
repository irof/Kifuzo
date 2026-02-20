package dev.irof.kfv.models

import java.nio.file.Path

/**
 * ファイルツリーの各要素（ファイルまたはディレクトリ）
 */
data class FileTreeNode(
    val path: Path,
    val level: Int,
    val isDirectory: Boolean,
    var isExpanded: Boolean = false,
    val parent: FileTreeNode? = null,
) {
    val name: String get() = path.fileName?.toString() ?: "/"
}
