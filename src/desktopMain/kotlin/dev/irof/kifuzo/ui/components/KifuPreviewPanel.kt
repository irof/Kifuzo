package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.ui.ShogiBoardView
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path
import kotlin.io.path.name

private const val ICON_ALPHA_INACTIVE = 0.6f

/**
 * 棋譜表示のナビゲーション操作。
 */
interface KifuNavigationActions {
    fun onStepChange(step: Int)
    fun onNextStep()
    fun onPrevStep()
    fun onToggleFlip()
    fun onSelectVariation(moves: List<Move>)
    fun onResetToMainHistory()
}

/**
 * 棋譜ファイルやメタデータに関連する操作。
 */
interface KifuFileActions {
    fun onWriteResult(path: Path, result: String)
    fun onShowEditMetadata(path: Path)
    fun onForceParse(path: Path)
    fun onOpenExternal(path: Path)
    fun onShowSaveDialog()
}

/**
 * 棋譜プレビューパネルでの操作を統合したインターフェース。
 */
interface KifuPreviewActions :
    KifuNavigationActions,
    KifuFileActions

@Composable
fun KifuPreviewPanel(
    state: KifuzoUiState,
    boardState: ShogiBoardState,
    actions: KifuPreviewActions,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val isFlipped = state.isFlipped
    val fileName = state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ShogiColors.Panel.MenuBarBackground)
            .padding(vertical = ShogiDimensions.Spacing.Medium)
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        KifuMetaInfo(
            fileName = fileName,
            senteName = boardState.session.senteName,
            goteName = boardState.session.goteName,
            startTime = boardState.session.startTime,
            event = boardState.session.event,
            warningMessage = boardState.session.warningMessage,
            isPasted = state.pastedKifuText != null,
            onEdit = { state.selectedFile?.let { actions.onShowEditMetadata(it) } },
            onOpenExternal = { state.selectedFile?.let { actions.onOpenExternal(it) } },
            onSave = { actions.onShowSaveDialog() },
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BoardArea(boardState, isFlipped, actions::onToggleFlip, Modifier.weight(1f))

            if (state.isMoveListVisible) {
                KifuMoveList(
                    moves = boardState.currentMoves,
                    currentStep = boardState.currentStep,
                    isMainHistory = boardState.currentMoves == boardState.session.moves,
                    onStepChange = actions::onStepChange,
                    onSelectVariation = actions::onSelectVariation,
                    onResetMain = actions::onResetToMainHistory,
                    onWriteResult = { result -> state.selectedFile?.let { actions.onWriteResult(it, result) } },
                    modifier = Modifier.width(ShogiDimensions.Component.MoveListWidth).fillMaxHeight(),
                )
            }
        }

        KifuFooter(boardState, isFlipped, actions::onStepChange)
    }
}

@Composable
private fun BoardArea(
    boardState: ShogiBoardState,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
        ShogiBoardView(boardState, isFlipped, onToggleFlip)
    }
}

@Composable
private fun KifuFooter(
    boardState: ShogiBoardState,
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val maxStep = boardState.currentMoves.size
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = ShogiDimensions.Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KifuStepButtons(
            currentStep = boardState.currentStep,
            maxStep = maxStep,
            isStandardStart = boardState.session.isStandardStart,
            firstContactStep = boardState.session.firstContactStep,
            onStepChange = onStepChange,
        )
        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
        KifuMoveSlider(
            currentStep = boardState.currentStep,
            maxStep = maxStep,
            onStepChange = onStepChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.Spacing.Large),
        )
        KifuGraphs(boardState.currentMoves, boardState.currentStep, isFlipped, onStepChange)
    }
}
