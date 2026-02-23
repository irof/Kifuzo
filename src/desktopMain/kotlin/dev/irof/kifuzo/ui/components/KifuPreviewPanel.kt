package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.ShogiBoardState
import dev.irof.kifuzo.ui.ShogiBoardView
import dev.irof.kifuzo.ui.theme.ShogiColors
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
    onDetectSenkei: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onRename: (Path) -> Unit,
    onWriteResult: (Path, String) -> Unit,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ShogiColors.PanelBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        KifuFileName(state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT)

        state.selectedFile?.let { selected ->
            KifuHeaderActions(
                selectedFile = selected,
                state = state,
                history = boardState.session.history,
                onDetectSenkei = onDetectSenkei,
                onConvertCsa = onConvertCsa,
                onRename = onRename,
                onWriteResult = onWriteResult,
            )
        }

        if (boardState.session.history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            KifuMainContent(state, boardState, onToggleFlip, onStepChange)
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
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShogiBoardView(boardState, isFlipped = state.isFlipped, onToggleFlip = onToggleFlip)
            Spacer(Modifier.height(16.dp))
            KifuOperationBar(
                currentStep = boardState.currentStep,
                maxStep = boardState.session.maxStep,
                history = boardState.session.history,
                isStandardStart = boardState.session.isStandardStart,
                firstContactStep = boardState.session.firstContactStep,
                isFlipped = state.isFlipped,
                onStepChange = onStepChange,
            )
        }

        KifuMoveList(
            history = boardState.session.history,
            currentStep = boardState.currentStep,
            onStepChange = onStepChange,
            modifier = Modifier.width(280.dp).fillMaxHeight(),
        )
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
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val evaluations = history.map { it.evaluation }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onStepChange(0) }, modifier = Modifier.height(32.dp)) { Text(AppStrings.START, fontSize = 10.sp) }
            Spacer(Modifier.width(4.dp))
            OutlinedButton(onClick = { onStepChange(currentStep - 1) }, modifier = Modifier.height(32.dp)) { Text("◀", fontSize = 10.sp) }
            Spacer(Modifier.width(4.dp))
            if (isStandardStart && firstContactStep != -1) {
                Button(onClick = { onStepChange(firstContactStep) }, modifier = Modifier.height(32.dp)) { Text(AppStrings.CONTACT, fontSize = 10.sp) }
                Spacer(Modifier.width(4.dp))
            }
            OutlinedButton(onClick = { onStepChange(currentStep + 1) }, modifier = Modifier.height(32.dp)) { Text("▶", fontSize = 10.sp) }
            Spacer(Modifier.width(4.dp))
            Button(onClick = { onStepChange(maxStep) }, modifier = Modifier.height(32.dp)) { Text(AppStrings.END, fontSize = 10.sp) }
        }
        Slider(
            value = currentStep.toFloat(),
            onValueChange = { onStepChange(it.toInt()) },
            valueRange = 0f..maxStep.toFloat(),
            steps = if (maxStep > 1) maxStep - 1 else 0,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        if (evaluations.any { it != null }) {
            Spacer(Modifier.height(8.dp))
            EvaluationGraph(
                evaluations = evaluations,
                currentStep = currentStep,
                isFlipped = isFlipped,
                onStepClick = onStepChange,
                modifier = Modifier.height(240.dp).fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
    }
}
