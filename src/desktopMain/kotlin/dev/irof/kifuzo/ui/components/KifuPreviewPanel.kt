package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

@Composable
fun KifuPreviewPanel(
    state: KifuzoUiState,
    boardState: ShogiBoardState,
    onToggleFlip: () -> Unit,
    onToggleMoveList: () -> Unit,
    onWriteResult: (Path, String) -> Unit,
    onShowEditMetadata: (Path) -> Unit,
    onStepChange: (Int) -> Unit,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onForceParse: (Path) -> Unit,
    onSelectVariation: (List<Move>) -> Unit,
    onResetToMainHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ShogiColors.PanelBackground)
            .padding(horizontal = ShogiDimensions.PaddingLarge, vertical = ShogiDimensions.PaddingMedium)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> {
                        onNextStep()
                        true
                    }
                    Key.DirectionUp -> {
                        onPrevStep()
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(Unit) { detectTapGestures { focusRequester.requestFocus() } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        val isMainHistory = boardState.currentMoves === boardState.session.moves
        val fileName = (state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT) + if (!isMainHistory) " [変化]" else ""

        KifuMetaInfo(
            fileName = fileName,
            senteName = boardState.session.senteName,
            goteName = boardState.session.goteName,
            startTime = boardState.session.startTime,
            event = boardState.session.event,
            onEdit = { state.selectedFile?.let { onShowEditMetadata(it) } },
        )

        KifuHeader(
            hasHistory = boardState.session.moves.isNotEmpty(),
            isMoveListVisible = state.isMoveListVisible,
            onToggleMoveList = onToggleMoveList,
        )

        KifuOpenButton(state.selectedFile, boardState.session.moves.isEmpty(), onForceParse)

        if (boardState.session.moves.isNotEmpty() || !boardState.session.initialSnapshot.isStandardInitial()) {
            Spacer(Modifier.height(ShogiDimensions.PaddingLarge))
            KifuMainContent(
                state = state,
                boardState = boardState,
                onToggleFlip = onToggleFlip,
                onStepChange = onStepChange,
                onSelectVariation = onSelectVariation,
                onResetToMainHistory = onResetToMainHistory,
                onWriteResult = { result -> state.selectedFile?.let { onWriteResult(it, result) } },
            )
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
    IconButton(onClick = onClick, modifier = Modifier.size(ShogiDimensions.IconSizeMedium)) {
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
            Spacer(Modifier.height(ShogiDimensions.PaddingLarge))
            Button(
                onClick = { onForceParse(selected) },
                modifier = Modifier.height(ShogiDimensions.ButtonHeight),
            ) {
                Text("棋譜として開く")
            }
        }
    }
}

@Composable
private fun ColumnScope.KifuMainContent(
    state: KifuzoUiState,
    boardState: ShogiBoardState,
    onToggleFlip: () -> Unit,
    onStepChange: (Int) -> Unit,
    onSelectVariation: (List<Move>) -> Unit,
    onResetToMainHistory: () -> Unit,
    onWriteResult: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(ShogiDimensions.PaddingLarge),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShogiBoardView(boardState, isFlipped = state.isFlipped, onToggleFlip = onToggleFlip)
            Spacer(Modifier.height(ShogiDimensions.PaddingLarge))
            KifuOperationBar(boardState, state.isFlipped, onStepChange)
        }

        if (state.isMoveListVisible) {
            KifuMoveList(
                moves = boardState.currentMoves,
                currentStep = boardState.currentStep,
                isMainHistory = boardState.currentMoves === boardState.session.moves,
                onStepChange = onStepChange,
                onWriteResult = onWriteResult,
                onSelectVariation = onSelectVariation,
                onResetMain = onResetToMainHistory,
                modifier = Modifier.width(ShogiDimensions.MoveListWidth).fillMaxHeight(),
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingLarge),
        )
        KifuGraphs(boardState.currentMoves, boardState.currentStep, isFlipped, onStepChange)
    }
}
