package dev.irof.kfv.viewmodel

import dev.irof.kfv.models.FileTreeNode
import dev.irof.kfv.models.KifuInfo
import java.nio.file.Path

data class KifuManagerUiState(
    val treeNodes: List<FileTreeNode> = emptyList(),
    val kifuInfos: Map<Path, KifuInfo> = emptyMap(),
    val isScanning: Boolean = false,
    val selectedSenkei: String? = null,
    val selectedFile: Path? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showOverwriteConfirm: Path? = null,
    val viewingText: String? = null,
    val isFlipped: Boolean = false,
    val showSettings: Boolean = false,
    val showImportDialog: Boolean = false,
    val myNameRegex: String = "",
) {
    val filteredNodes: List<FileTreeNode>
        get() = if (selectedSenkei == null) {
            treeNodes
        } else {
            treeNodes.filter { node -> node.isDirectory || kifuInfos[node.path]?.senkei == selectedSenkei }
        }

    val availableSenkei: List<String>
        get() = kifuInfos.values.map { it.senkei }.filter { it.isNotEmpty() }.distinct().sorted()
}
