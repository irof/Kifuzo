package dev.irof.kfv.viewmodel

import dev.irof.kfv.models.FileTreeNode
import java.nio.file.Path

sealed class KifuManagerAction {
    data class SetRootDirectory(val path: Path) : KifuManagerAction()
    data class ToggleDirectory(val node: FileTreeNode) : KifuManagerAction()
    data class SelectFile(val path: Path) : KifuManagerAction()
    data class SetSelectedSenkei(val senkei: String?) : KifuManagerAction()
    data class SaveSettings(val regex: String) : KifuManagerAction()
    data class SetViewingText(val text: String?) : KifuManagerAction()
    data object ToggleFlipped : KifuManagerAction()
    data class ShowSettings(val show: Boolean) : KifuManagerAction()
    data class ShowImportDialog(val show: Boolean) : KifuManagerAction()
    data object ClearErrorAndInfo : KifuManagerAction()
    data class ImportFiles(val sourceDir: Path) : KifuManagerAction()
    data class ConvertCsa(val path: Path) : KifuManagerAction()
    data object ConfirmOverwrite : KifuManagerAction()
    data object HideOverwriteConfirm : KifuManagerAction()
    data class DetectAndWriteSenkei(val path: Path) : KifuManagerAction()
    
    // 指し手操作
    data class ChangeStep(val step: Int) : KifuManagerAction()
    data object NextStep : KifuManagerAction()
    data object PrevStep : KifuManagerAction()
}
