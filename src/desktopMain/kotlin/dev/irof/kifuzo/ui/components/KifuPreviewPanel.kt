package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.ui.ShogiBoardView
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

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
            .pointerInput(Unit) {
                detectTapGestures { focusRequester.requestFocus() }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        KifuFileName(state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT)

        state.selectedFile?.let { selected ->
            KifuHeaderActions(
                history = boardState.session.history,
                isMoveListVisible = state.isMoveListVisible,
                onToggleMoveList = onToggleMoveList,
            )

            if (boardState.session.history.isEmpty() && selected.extension.lowercase() == "txt") {
                Spacer(Modifier.height(ShogiDimensions.PaddingLarge))
                Button(
                    onClick = { onForceParse(selected) },
                    modifier = Modifier.height(ShogiDimensions.ButtonHeight),
                ) {
                    Text("棋譜として開く")
                }
            }
        }

        if (boardState.session.history.isNotEmpty()) {
            Spacer(Modifier.height(ShogiDimensions.PaddingLarge))
            KifuMainContent(
                state,
                boardState,
                onToggleFlip,
                onStepChange,
                onShowEditMetadata = { state.selectedFile?.let { onShowEditMetadata(it) } },
            ) { result ->
                state.selectedFile?.let { onWriteResult(it, result) }
            }
        }
    }
}

@Composable
private fun KifuFileName(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            softWrap = false,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ColumnScope.KifuMainContent(
    state: KifuzoUiState,
    boardState: ShogiBoardState,
    onToggleFlip: () -> Unit,
    onStepChange: (Int) -> Unit,
    onShowEditMetadata: () -> Unit,
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
            KifuOperationBar(
                currentStep = boardState.currentStep,
                maxStep = boardState.session.maxStep,
                history = boardState.session.history,
                isStandardStart = boardState.session.isStandardStart,
                firstContactStep = boardState.session.firstContactStep,
                startTime = boardState.session.startTime,
                event = boardState.session.event,
                isFlipped = state.isFlipped,
                onStepChange = onStepChange,
                onShowEditMetadata = onShowEditMetadata,
            )
        }

        if (state.isMoveListVisible) {
            KifuMoveList(
                history = boardState.session.history,
                currentStep = boardState.currentStep,
                onStepChange = onStepChange,
                onWriteResult = onWriteResult,
                modifier = Modifier.width(ShogiDimensions.MoveListWidth).fillMaxHeight(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KifuOperationBar(
    currentStep: Int,
    maxStep: Int,
    history: List<BoardSnapshot>,
    isStandardStart: Boolean,
    firstContactStep: Int,
    startTime: String,
    event: String,
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
    onShowEditMetadata: () -> Unit,
) {
    val evaluations = history.map { it.evaluation }
    val consumptionTimes = history.map { it.consumptionSeconds }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onStepChange(0) }, modifier = Modifier.height(ShogiDimensions.ButtonHeight)) { Text(AppStrings.START, fontSize = ShogiDimensions.FontSizeCaption) }
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            OutlinedButton(onClick = { onStepChange(currentStep - 1) }, modifier = Modifier.height(ShogiDimensions.ButtonHeight)) { Text("◀", fontSize = ShogiDimensions.FontSizeCaption) }
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            if (isStandardStart && firstContactStep != -1) {
                Button(onClick = { onStepChange(firstContactStep) }, modifier = Modifier.height(ShogiDimensions.ButtonHeight)) { Text(AppStrings.CONTACT, fontSize = ShogiDimensions.FontSizeCaption) }
                Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            }
            OutlinedButton(onClick = { onStepChange(currentStep + 1) }, modifier = Modifier.height(ShogiDimensions.ButtonHeight)) { Text("▶", fontSize = ShogiDimensions.FontSizeCaption) }
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            Button(onClick = { onStepChange(maxStep) }, modifier = Modifier.height(ShogiDimensions.ButtonHeight)) { Text(AppStrings.END, fontSize = ShogiDimensions.FontSizeCaption) }
        }
        Slider(
            value = currentStep.toFloat(),
            onValueChange = { onStepChange(it.toInt()) },
            valueRange = 0f..maxStep.toFloat(),
            steps = if (maxStep > 1) maxStep - 1 else 0,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingLarge),
        )

        if (evaluations.any { it is Evaluation.Score } || consumptionTimes.any { it != null }) {
            Spacer(Modifier.height(ShogiDimensions.PaddingMedium))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(ShogiDimensions.PaddingLarge),
            ) {
                val hasEval = evaluations.any { it is Evaluation.Score }
                val hasTime = consumptionTimes.any { it != null }
                val targetHeight = if (hasEval && hasTime) ShogiDimensions.DualGraphHeight else ShogiDimensions.GraphHeight

                if (hasEval) {
                    EvaluationGraph(
                        evaluations = evaluations,
                        currentStep = currentStep,
                        isFlipped = isFlipped,
                        onStepClick = onStepChange,
                        modifier = Modifier.height(targetHeight).fillMaxWidth(),
                    )
                }
                if (hasTime) {
                    ConsumptionTimeGraph(
                        times = consumptionTimes,
                        currentStep = currentStep,
                        onStepClick = onStepChange,
                        modifier = Modifier.height(targetHeight).fillMaxWidth(),
                    )
                }
            }
        }
        KifuMetaInfo(startTime, event, onShowEditMetadata)
    }
}

@Composable
private fun KifuMetaInfo(startTime: String, event: String, onEdit: () -> Unit) {
    if (startTime.isEmpty() && event.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ShogiDimensions.PaddingLarge)
            .padding(top = ShogiDimensions.PaddingMedium),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (event.isNotEmpty()) {
                Row {
                    Text(
                        text = AppStrings.LABEL_EVENT,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = event,
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
            if (startTime.isNotEmpty()) {
                Row {
                    Text(
                        text = AppStrings.LABEL_START_TIME,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = startTime,
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }

        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = AppStrings.EDIT_METADATA,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
