package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.Evaluation
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

private const val FILENAME_WEIGHT = 8f
private const val ICON_ALPHA_INACTIVE = 0.6f
private const val METADATA_BG_ALPHA = 0.5f
private const val METADATA_BORDER_ALPHA = 0.2f

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
    onSelectVariation: (List<BoardSnapshot>) -> Unit,
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
            .pointerInput(Unit) {
                detectTapGestures { focusRequester.requestFocus() }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        val isMainHistory = boardState.currentHistory === boardState.session.history
        KifuHeader(
            fileName = (state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT) + if (!isMainHistory) " [変化]" else "",
            hasHistory = boardState.session.history.isNotEmpty(),
            isMoveListVisible = state.isMoveListVisible,
            onToggleMoveList = onToggleMoveList,
        )

        state.selectedFile?.let { selected ->
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
                state = state,
                boardState = boardState,
                onToggleFlip = onToggleFlip,
                onStepChange = onStepChange,
                onShowEditMetadata = { state.selectedFile?.let { onShowEditMetadata(it) } },
                onSelectVariation = onSelectVariation,
                onResetToMainHistory = onResetToMainHistory,
                onWriteResult = { result -> state.selectedFile?.let { onWriteResult(it, result) } },
            )
        }
    }
}

@Composable
private fun KifuHeader(
    fileName: String,
    hasHistory: Boolean,
    isMoveListVisible: Boolean,
    onToggleMoveList: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(ShogiDimensions.IconSizeMedium).weight(1f))

        Text(
            text = fileName,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            softWrap = false,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(FILENAME_WEIGHT),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (hasHistory) {
                MoveListToggleButton(isMoveListVisible, onToggleMoveList)
            } else {
                Spacer(Modifier.size(ShogiDimensions.IconSizeMedium))
            }
        }
    }
}

@Composable
private fun MoveListToggleButton(isVisible: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(ShogiDimensions.IconSizeMedium),
    ) {
        Icon(
            imageVector = ShogiIcons.SidebarToggle,
            contentDescription = if (isVisible) "手順を隠す" else "手順を表示",
            tint = if (isVisible) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = ICON_ALPHA_INACTIVE),
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
    onSelectVariation: (List<BoardSnapshot>) -> Unit,
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
            KifuOperationBar(
                currentStep = boardState.currentStep,
                maxStep = boardState.currentHistory.size - 1,
                history = boardState.currentHistory,
                isStandardStart = boardState.session.isStandardStart,
                firstContactStep = boardState.session.firstContactStep,
                senteName = boardState.session.senteName,
                goteName = boardState.session.goteName,
                startTime = boardState.session.startTime,
                event = boardState.session.event,
                isFlipped = state.isFlipped,
                onStepChange = onStepChange,
                onShowEditMetadata = onShowEditMetadata,
            )
        }

        if (state.isMoveListVisible) {
            KifuMoveList(
                history = boardState.currentHistory,
                currentStep = boardState.currentStep,
                isMainHistory = boardState.currentHistory === boardState.session.history,
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
    currentStep: Int,
    maxStep: Int,
    history: List<BoardSnapshot>,
    isStandardStart: Boolean,
    firstContactStep: Int,
    senteName: String,
    goteName: String,
    startTime: String,
    event: String,
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
    onShowEditMetadata: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        KifuStepButtons(currentStep, maxStep, isStandardStart, firstContactStep, onStepChange)

        Slider(
            value = currentStep.toFloat(),
            onValueChange = { onStepChange(it.toInt()) },
            valueRange = 0f..maxStep.toFloat(),
            steps = if (maxStep > 1) maxStep - 1 else 0,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingLarge),
        )

        KifuGraphs(history, currentStep, isFlipped, onStepChange)
        KifuMetaInfo(senteName, goteName, startTime, event, onShowEditMetadata)
    }
}

@Composable
private fun KifuStepButtons(
    currentStep: Int,
    maxStep: Int,
    isStandardStart: Boolean,
    firstContactStep: Int,
    onStepChange: (Int) -> Unit,
) {
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
}

@Composable
private fun KifuGraphs(
    history: List<BoardSnapshot>,
    currentStep: Int,
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val evaluations = history.map { it.evaluation }
    val consumptionTimes = history.map { it.consumptionSeconds }
    val hasEval = evaluations.any { it is Evaluation.Score }
    val hasTime = consumptionTimes.any { it != null }

    if (hasEval || hasTime) {
        Spacer(Modifier.height(ShogiDimensions.PaddingMedium))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.PaddingLarge),
            verticalArrangement = Arrangement.spacedBy(ShogiDimensions.PaddingLarge),
        ) {
            val targetHeight = if (hasEval && hasTime) ShogiDimensions.DualGraphHeight else ShogiDimensions.GraphHeight
            if (hasEval) {
                EvaluationGraph(evaluations, currentStep, isFlipped, onStepChange, Modifier.height(targetHeight).fillMaxWidth())
            }
            if (hasTime) {
                ConsumptionTimeGraph(consumptionTimes, currentStep, onStepChange, Modifier.height(targetHeight).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun KifuMetaInfo(senteName: String, goteName: String, startTime: String, event: String, onEdit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ShogiDimensions.PaddingLarge)
            .padding(top = ShogiDimensions.PaddingLarge)
            .background(Color.White.copy(alpha = METADATA_BG_ALPHA), RoundedCornerShape(ShogiDimensions.CornerMedium))
            .border(1.dp, Color.Gray.copy(alpha = METADATA_BORDER_ALPHA), RoundedCornerShape(ShogiDimensions.CornerMedium))
            .padding(ShogiDimensions.PaddingMedium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            KifuMetaText(senteName, goteName, startTime, event, modifier = Modifier.weight(1f))

            IconButton(onClick = onEdit, modifier = Modifier.size(ShogiDimensions.IconSizeSmall)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = AppStrings.EDIT_METADATA,
                    tint = Color.Gray,
                    modifier = Modifier.size(ShogiDimensions.IconSizeSmall),
                )
            }
        }
    }
}

@Composable
private fun KifuMetaText(sente: String, gote: String, startTime: String, event: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ShogiDimensions.PaddingSmall),
    ) {
        if (isAllMetadataEmpty(sente, gote, startTime, event)) {
            Text(
                text = AppStrings.NO_METADATA_HINT,
                style = MaterialTheme.typography.caption,
                color = Color.Gray.copy(alpha = ICON_ALPHA_INACTIVE),
            )
        } else {
            if (sente.isNotEmpty()) MetaRow(AppStrings.LABEL_SENTE, sente)
            if (gote.isNotEmpty()) MetaRow(AppStrings.LABEL_GOTE, gote)
            if (event.isNotEmpty()) MetaRow(AppStrings.LABEL_EVENT, event)
            if (startTime.isNotEmpty()) MetaRow(AppStrings.LABEL_START_TIME, startTime)
        }
    }
}

private fun isAllMetadataEmpty(sente: String, gote: String, startTime: String, event: String): Boolean = sente.isEmpty() && gote.isEmpty() && startTime.isEmpty() && event.isEmpty()

@Composable
private fun MetaRow(label: String, value: String) {
    Row {
        Text(text = label, style = MaterialTheme.typography.caption, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.caption)
    }
}
