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
            .fillMaxHeight()
            .padding(ShogiDimensions.Spacing.Large)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        onSelectNext()
                        true
                    }
                    Key.DirectionUp -> {
                        onSelectPrev()
                        true
                    }
                    Key.Enter, Key.NumPadEnter, Key.DirectionRight -> {
                        val selectedNode = state.treeNodes.find { it.path == state.selectedFile }
                        if (selectedNode != null && selectedNode.isDirectory) {
                            onToggleDir(selectedNode)
                            true
                        } else {
                            false
                        }
                    }
                    else -> false
                }
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
