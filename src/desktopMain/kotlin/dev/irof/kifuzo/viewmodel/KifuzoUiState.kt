package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.models.KifuInfo
import java.nio.file.Path

data class KifuzoUiState(
    val treeNodes: List<FileTreeNode> = emptyList(),
    val kifuInfos: Map<Path, KifuInfo> = emptyMap(),
    val isScanning: Boolean = false,
    val selectedFile: Path? = null,
    val errorMessage: String? = null,
    val errorDetail: String? = null,
    val infoMessage: String? = null,
    val showOverwriteConfirm: Path? = null,
    val viewingText: String? = null,
    val isFlipped: Boolean = false,
    val showSettings: Boolean = false,
    val showImportDialog: Boolean = false,
    val myNameRegex: String = "",
    val filenameTemplate: String = "{開始日の年月日}_{開始日の時分秒}_{棋戦名}_{先手}_{後手}",
    val persistFileTreeState: Boolean = true,
    val isSidebarVisible: Boolean = true,
    val isMoveListVisible: Boolean = true,
    val sidebarWidth: Float = 250f,
    val viewMode: FileViewMode = FileViewMode.HIERARCHY,
    val fileFilters: Set<FileFilter> = emptySet(),
    val fileSortOption: FileSortOption = AppSettings.Default.fileSortOption,
    val renameTarget: Path? = null,
    val proposedRenameName: String? = null,
    val editMetadataTarget: Path? = null,
    val pastedKifuText: String? = null,
    val pastedKifuProposedName: String? = null,
)
