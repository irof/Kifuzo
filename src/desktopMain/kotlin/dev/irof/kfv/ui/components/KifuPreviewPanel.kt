package dev.irof.kfv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kfv.models.ShogiBoardState
import dev.irof.kfv.ui.ShogiBoardView
import dev.irof.kfv.ui.theme.ShogiColors
import dev.irof.kfv.utils.AppStrings
import dev.irof.kfv.viewmodel.KifuManagerUiState
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

@Composable
fun KifuPreviewPanel(
    state: KifuManagerUiState,
    boardState: ShogiBoardState,
    onToggleFlip: () -> Unit,
    onDetectSenkei: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ShogiColors.PanelBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(8.dp))
        Text(text = state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)

        state.selectedFile?.let { selected ->
            val ext = selected.extension.lowercase()
            val isKifuFile = ext == "kifu" || ext == "kif"
            val hasHistory = boardState.session.history.isNotEmpty()

            if (hasHistory || ext == "csa") {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (hasHistory) {
                        OutlinedButton(onClick = onToggleFlip, modifier = Modifier.height(32.dp), colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (state.isFlipped) Color.LightGray else Color.White)) { Text(AppStrings.FLIP_BOARD, fontSize = 10.sp) }

                        if (isKifuFile) {
                            val kifuInfo = state.kifuInfos[selected]
                            val existingSenkei = kifuInfo?.senkei
                            Spacer(Modifier.width(8.dp))

                            if (!existingSenkei.isNullOrEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(32.dp).background(Color.White, MaterialTheme.shapes.small).border(1.dp, Color.LightGray, MaterialTheme.shapes.small).padding(horizontal = 8.dp)) {
                                    Text("戦型: $existingSenkei", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(onClick = { onDetectSenkei(selected) }, modifier = Modifier.size(18.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = AppStrings.DETECT_SENKEI, tint = ShogiColors.Info)
                                    }
                                }
                            } else {
                                Button(onClick = { onDetectSenkei(selected) }, colors = ButtonDefaults.buttonColors(backgroundColor = ShogiColors.Info, contentColor = Color.White), modifier = Modifier.height(32.dp)) {
                                    Text(AppStrings.DETECT_SENKEI, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    if (ext == "csa") {
                        if (hasHistory) Spacer(Modifier.width(8.dp))
                        Button(onClick = { onConvertCsa(selected) }, colors = ButtonDefaults.buttonColors(backgroundColor = ShogiColors.Success, contentColor = Color.White), modifier = Modifier.height(32.dp)) {
                            Text(AppStrings.CONVERT_TO_KIFU, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        if (boardState.session.history.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ShogiBoardView(boardState, isFlipped = state.isFlipped)
            Spacer(Modifier.height(8.dp))

            KifuOperationBar(
                currentStep = boardState.currentStep,
                maxStep = boardState.session.maxStep,
                lastMoveText = boardState.currentBoard?.lastMoveText ?: "",
                isStandardStart = boardState.session.isStandardStart,
                firstContactStep = boardState.session.firstContactStep,
                onStepChange = onStepChange,
            )
        }
    }
}

@Composable
private fun KifuOperationBar(
    currentStep: Int,
    maxStep: Int,
    lastMoveText: String,
    isStandardStart: Boolean,
    firstContactStep: Int,
    onStepChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "${AppStrings.MOVE_COUNT}: $currentStep / $maxStep", style = MaterialTheme.typography.caption)
        Text(text = lastMoveText, style = MaterialTheme.typography.body2, modifier = Modifier.height(24.dp))
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
            modifier = Modifier.width(280.dp),
        )
    }
}
