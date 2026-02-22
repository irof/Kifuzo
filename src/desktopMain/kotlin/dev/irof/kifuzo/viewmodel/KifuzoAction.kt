package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.models.FileTreeNode
import java.nio.file.Path

sealed class KifuzoAction {
    data class SetRootDirectory(val path: Path) : KifuzoAction()
    data class ToggleDirectory(val node: FileTreeNode) : KifuzoAction()
    data class SelectFile(val path: Path) : KifuzoAction()
    data class SaveSettings(val regex: String) : KifuzoAction()
    data class SetViewingText(val text: String?) : KifuzoAction()
    data object ToggleFlipped : KifuzoAction()
    data class ShowSettings(val show: Boolean) : KifuzoAction()
    data class ShowImportDialog(val show: Boolean) : KifuzoAction()
    data object ClearErrorAndInfo : KifuzoAction()
    data class ImportFiles(val sourceDir: Path) : KifuzoAction()
    data class ConvertCsa(val path: Path) : KifuzoAction()
    data object ConfirmOverwrite : KifuzoAction()
    data object HideOverwriteConfirm : KifuzoAction()
    data class DetectAndWriteSenkei(val path: Path) : KifuzoAction()
    data object ToggleSidebar : KifuzoAction()
    data class SetViewMode(val mode: FileViewMode) : KifuzoAction()
    data class ToggleFileFilter(val filter: FileFilter) : KifuzoAction()
    data class UpdateSidebarWidth(val delta: Float) : KifuzoAction()
    data object RefreshFiles : KifuzoAction()
    data object SelectNextFile : KifuzoAction()
    data object SelectPrevFile : KifuzoAction()

    // 指し手操作
    data class ChangeStep(val step: Int) : KifuzoAction()
    data object NextStep : KifuzoAction()
    data object PrevStep : KifuzoAction()
}
