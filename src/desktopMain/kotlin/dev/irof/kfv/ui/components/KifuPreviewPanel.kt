package dev.irof.kfv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Slider
import androidx.compose.material.Text
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
    onToggleSidebar: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = onToggleSidebar, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = dev.irof.kfv.ui.theme.ShogiIcons.SidebarToggle,
                    contentDescription = "サイドバー切替",
                    tint = Color.Gray,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(24.dp)) // ボタン分とのバランス調整用
        }

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
                evaluations = boardState.session.history.map { it.evaluation },
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
    evaluations: List<Int?>,
    onStepChange: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val currentEval = evaluations.getOrNull(currentStep)
        val evalText = if (currentEval != null) {
            val sign = if (currentEval > 0) "+" else ""
            " (評価値: $sign$currentEval)"
        } else {
            ""
        }

        // 表示用の指し手テキストを作成 (例: "1 ▲７六歩")
        val displayMove = if (currentStep == 0) {
            "開始局面"
        } else {
            val colorSymbol = if (currentStep % 2 != 0) "▲" else "△"
            // "1 ７六歩(77)" -> "1 ▲７六歩"
            // 手数と指し手部分を分離
            val parts = lastMoveText.trim().split(Regex("\\s+"))
            if (parts.size >= 2) {
                val stepNum = parts[0]
                val move = parts[1].substringBefore("(")
                "$stepNum $colorSymbol$move"
            } else {
                "$currentStep $colorSymbol$lastMoveText"
            }
        }

        Text(text = displayMove, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
        Text(text = "($currentStep / $maxStep$evalText)", style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(8.dp))

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
            EvaluationGraph(evaluations = evaluations, currentStep = currentStep, onStepClick = onStepChange, modifier = Modifier.height(240.dp).fillMaxWidth().padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))
        }
    }
}
