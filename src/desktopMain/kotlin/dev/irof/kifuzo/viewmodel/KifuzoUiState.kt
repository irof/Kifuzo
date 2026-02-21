package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.KifuInfo
import java.nio.file.Path

enum class FileViewMode {
    HIERARCHY,
    FLAT,
}

enum class FileFilter {
    KIFU_ONLY,
    RECENT,
}

data class KifuzoUiState(
    val treeNodes: List<FileTreeNode> = emptyList(),
    val kifuInfos: Map<Path, KifuInfo> = emptyMap(),
    val isScanning: Boolean = false,
    val selectedFile: Path? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showOverwriteConfirm: Path? = null,
    val viewingText: String? = null,
    val isFlipped: Boolean = false,
    val showSettings: Boolean = false,
    val showImportDialog: Boolean = false,
    val myNameRegex: String = "",
    val isSidebarVisible: Boolean = true,
    val viewMode: FileViewMode = FileViewMode.HIERARCHY,
    val fileFilters: Set<FileFilter> = emptySet(),
)
