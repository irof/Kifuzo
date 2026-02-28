package dev.irof.kifuzo.models

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

enum class FileViewMode {
    HIERARCHY,
    FLAT,
}

enum class FileFilter {
    RECENT,
}

enum class FileSortOption {
    NAME,
    LAST_MODIFIED,
}
