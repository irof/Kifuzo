package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import dev.irof.kifuzo.ui.UiTestTags
import dev.irof.kifuzo.ui.sidebar.FileFiltersAndSorting
import dev.irof.kifuzo.ui.sidebar.FileTreeList
import dev.irof.kifuzo.ui.sidebar.FileViewModeSelector
import dev.irof.kifuzo.ui.sidebar.RootDirectorySelector
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path

/**
 * サイドバー（ファイルブラウザ）。
 *
 * 表示モードの切り替え、ファイルのフィルタリングやソート、
 * ルートディレクトリの選択、および棋譜ファイルのツリー表示機能を提供します。
 */
@Composable
fun KifuSidebar(
    state: KifuzoUiState,
    currentRoot: Path?,
    onSetRoot: (Path) -> Unit,
    onRefresh: () -> Unit,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
    onSelectFile: (Path) -> Unit,
    onShowText: (String) -> Unit,
    onRename: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onForceParse: (Path) -> Unit,
    onSetViewMode: (dev.irof.kifuzo.models.FileViewMode) -> Unit,
    onSetFileSortOption: (dev.irof.kifuzo.models.FileSortOption) -> Unit,
    onToggleFileFilter: (dev.irof.kifuzo.models.FileFilter) -> Unit,
    onSelectNext: () -> Unit,
    onSelectPrev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .testTag(UiTestTags.SIDEBAR)
            .fillMaxHeight()
            .padding(ShogiDimensions.Spacing.Large)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                handleSidebarKeyEvent(
                    event = event,
                    state = state,
                    onSelectNext = onSelectNext,
                    onSelectPrev = onSelectPrev,
                    onSelectFile = onSelectFile,
                    onToggleDir = onToggleDir,
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { focusRequester.requestFocus() }
            },
    ) {
        // --- 表示モード切替 ---
        FileViewModeSelector(
            currentMode = state.viewMode,
            onSetViewMode = onSetViewMode,
        )

        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))

        // --- フィルタ・ソート選択 ---
        FileFiltersAndSorting(
            currentFilters = state.fileFilters,
            currentSort = state.fileSortOption,
            onToggleFilter = onToggleFileFilter,
            onSetSort = onSetFileSortOption,
        )

        // --- フォルダ選択・更新 ---
        RootDirectorySelector(
            currentRoot = currentRoot,
            onSetRoot = onSetRoot,
            onRefresh = onRefresh,
        )

        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))

        // --- ファイルツリーリスト ---
        FileTreeList(
            state = state,
            onToggleDir = onToggleDir,
            onSelectFile = onSelectFile,
            onShowText = onShowText,
            onRename = onRename,
            onConvertCsa = onConvertCsa,
            onForceParse = onForceParse,
        )
    }
}

private fun handleSidebarKeyEvent(
    event: androidx.compose.ui.input.key.KeyEvent,
    state: KifuzoUiState,
    onSelectNext: () -> Unit,
    onSelectPrev: () -> Unit,
    onSelectFile: (Path) -> Unit,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val currentIndex = state.treeNodes.indexOfFirst { it.path == state.selectedFile }

    return when (event.key) {
        Key.DirectionDown -> {
            onSelectNext()
            true
        }
        Key.DirectionUp -> {
            onSelectPrev()
            true
        }
        Key.Enter, Key.NumPadEnter -> handleEnterKey(currentIndex, state, onToggleDir)
        Key.DirectionRight -> handleRightKey(currentIndex, state, onSelectFile, onToggleDir)
        Key.DirectionLeft -> handleLeftKey(currentIndex, state, onSelectFile, onToggleDir)
        else -> false
    }
}

private fun handleEnterKey(
    currentIndex: Int,
    state: KifuzoUiState,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
): Boolean {
    val node = state.treeNodes.getOrNull(currentIndex) ?: return false
    return if (node.isDirectory) {
        onToggleDir(node)
        true
    } else {
        false
    }
}

private fun handleRightKey(
    currentIndex: Int,
    state: KifuzoUiState,
    onSelectFile: (Path) -> Unit,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
): Boolean {
    val node = state.treeNodes.getOrNull(currentIndex)
    if (node == null || !node.isDirectory) return false

    if (node.isExpanded) {
        state.treeNodes.getOrNull(currentIndex + 1)?.let { onSelectFile(it.path) }
    } else {
        onToggleDir(node)
    }
    return true
}

private fun handleLeftKey(
    currentIndex: Int,
    state: KifuzoUiState,
    onSelectFile: (Path) -> Unit,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
): Boolean {
    val node = state.treeNodes.getOrNull(currentIndex) ?: return false

    return if (node.isDirectory && node.isExpanded) {
        onToggleDir(node)
        true
    } else {
        findAndCloseParent(currentIndex, node.level, state, onSelectFile, onToggleDir)
    }
}

private fun findAndCloseParent(
    currentIndex: Int,
    currentLevel: Int,
    state: KifuzoUiState,
    onSelectFile: (Path) -> Unit,
    onToggleDir: (dev.irof.kifuzo.models.FileTreeNode) -> Unit,
): Boolean {
    var i = currentIndex - 1
    while (i >= 0) {
        val parentCandidate = state.treeNodes[i]
        if (parentCandidate.isDirectory && parentCandidate.level < currentLevel) {
            onSelectFile(parentCandidate.path)
            if (parentCandidate.isExpanded) {
                onToggleDir(parentCandidate)
            }
            return true
        }
        i--
    }
    return false
}
