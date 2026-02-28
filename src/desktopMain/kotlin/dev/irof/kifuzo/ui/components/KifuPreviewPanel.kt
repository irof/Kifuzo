package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
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
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.ui.ShogiBoardView
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

private const val ICON_ALPHA_INACTIVE = 0.6f

/**
 * 棋譜プレビューパネルでの操作を抽象化するインターフェース。
 */
@Suppress("TooManyFunctions")
interface KifuPreviewActions {
    fun onToggleFlip()
    fun onToggleMoveList()
    fun onWriteResult(path: Path, result: String)
    fun onShowEditMetadata(path: Path)
    fun onStepChange(step: Int)
    fun onNextStep()
    fun onPrevStep()
    fun onForceParse(path: Path)
    fun onSelectVariation(moves: List<Move>)
    fun onResetToMainHistory()
    fun onOpenExternal(path: Path)
    fun onShowSaveDialog()
}

@Composable
fun KifuPreviewPanel(
    state: KifuzoUiState,
    boardState: ShogiBoardState,
    actions: KifuPreviewActions,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ShogiColors.Panel.Background)
            .padding(horizontal = ShogiDimensions.Spacing.Large, vertical = ShogiDimensions.Spacing.Medium)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        actions.onNextStep()
                        true
                    }
                    Key.DirectionUp -> {
                        actions.onPrevStep()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        val fileName = state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT

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

        KifuHeader(
            hasHistory = boardState.session.moves.isNotEmpty(),
            isMoveListVisible = state.isMoveListVisible,
            onToggleMoveList = { actions.onToggleMoveList() },
        )

        KifuOpenButton(state.selectedFile, boardState.session.moves.isEmpty(), actions::onForceParse)

        if (boardState.session.moves.isNotEmpty() || !boardState.session.initialSnapshot.isStandardInitial()) {
            Spacer(Modifier.height(ShogiDimensions.Spacing.Large))
            KifuMainContent(state, boardState, actions)
        }
    }
}

@Composable
private fun KifuHeader(
    hasHistory: Boolean,
    isMoveListVisible: Boolean,
    onToggleMoveList: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        if (hasHistory) {
            MoveListToggleButton(isMoveListVisible, onToggleMoveList)
        }
    }
}

@Composable
private fun MoveListToggleButton(isVisible: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(ShogiDimensions.Icon.Medium)) {
        Icon(
            imageVector = ShogiIcons.SidebarToggle,
            contentDescription = if (isVisible) "手順を隠す" else "手順を表示",
            tint = if (isVisible) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = ICON_ALPHA_INACTIVE),
        )
    }
}

@Composable
private fun KifuOpenButton(selectedFile: Path?, isHistoryEmpty: Boolean, onForceParse: (Path) -> Unit) {
    selectedFile?.let { selected ->
        if (isHistoryEmpty && selected.extension.lowercase() == "txt") {
            Spacer(Modifier.height(ShogiDimensions.Spacing.Large))
            Button(onClick = { onForceParse(selected) }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight)) {
                Text("棋譜として開く")
            }
        }
    }
}

@Composable
private fun ColumnScope.KifuMainContent(
    state: KifuzoUiState,
    boardState: ShogiBoardState,
    actions: KifuPreviewActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(ShogiDimensions.Spacing.Large),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShogiBoardView(boardState, isFlipped = state.isFlipped, onToggleFlip = actions::onToggleFlip)
            Spacer(Modifier.height(ShogiDimensions.Spacing.Large))
            KifuOperationBar(boardState, state.isFlipped, actions::onStepChange)
        }

        if (state.isMoveListVisible) {
            KifuMoveList(
                moves = boardState.currentMoves,
                currentStep = boardState.currentStep,
                isMainHistory = boardState.currentMoves === boardState.session.moves,
                onStepChange = actions::onStepChange,
                onWriteResult = { result -> state.selectedFile?.let { actions.onWriteResult(it, result) } },
                onSelectVariation = actions::onSelectVariation,
                onResetMain = actions::onResetToMainHistory,
                modifier = Modifier.width(ShogiDimensions.Component.MoveListWidth).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun KifuOperationBar(
    boardState: ShogiBoardState,
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val maxStep = boardState.currentMoves.size
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        KifuStepButtons(boardState.currentStep, maxStep, boardState.session.isStandardStart, boardState.session.firstContactStep, onStepChange)
        Slider(
            value = boardState.currentStep.toFloat(),
            onValueChange = { onStepChange(it.toInt()) },
            valueRange = 0f..maxStep.toFloat(),
            steps = if (maxStep > 1) maxStep - 1 else 0,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.Spacing.Large),
        )
        KifuGraphs(boardState.currentMoves, boardState.currentStep, isFlipped, onStepChange)
    }
}
