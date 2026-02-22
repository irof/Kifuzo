package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
            Text(
                text = state.selectedFile?.name ?: AppStrings.SELECT_KIFU_HINT,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
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
            Spacer(Modifier.height(16.dp))

            // 盤面領域と指し手一覧領域を横に並べる
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 左側: 盤面、スライダー、評価値グラフ、局面が大きく動いた手
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ShogiBoardView(boardState, isFlipped = state.isFlipped)
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

                // 右側: 指し手一覧
                MoveList(
                    history = boardState.session.history,
                    currentStep = boardState.currentStep,
                    onStepChange = onStepChange,
                    modifier = Modifier.width(280.dp),
                )
            }
        }
    }
}

@Composable
private fun MoveList(
    history: List<BoardSnapshot>,
    currentStep: Int,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White, MaterialTheme.shapes.medium)
            .border(1.dp, Color.LightGray, MaterialTheme.shapes.medium)
            .padding(vertical = 4.dp),
    ) {
        Text(
            AppStrings.MOVE_LIST,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))

        // 0手目（開始局面）
        MoveRow(0, AppStrings.START_POSITION, null, null, currentStep == 0, onStepChange)

        for (i in 1 until history.size) {
            val board = history[i]
            val prevEval = history[i - 1].evaluation ?: 0
            val curEval = board.evaluation
            val diff = if (curEval != null) curEval - prevEval else null

            val colorSymbol = if (i % 2 != 0) "▲" else "△"
            // "1 ７六歩(77)" -> "７六歩"
            val moveText = board.lastMoveText.trim().split(Regex("\\s+")).getOrNull(1)?.substringBefore("(") ?: board.lastMoveText

            MoveRow(i, "$colorSymbol$moveText", curEval, diff, currentStep == i, onStepChange)
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
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val currentBoard = history.getOrNull(currentStep)
    val lastMoveText = currentBoard?.lastMoveText ?: ""
    val evaluations = history.map { it.evaluation }

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
            EvaluationGraph(
                evaluations = evaluations,
                currentStep = currentStep,
                isFlipped = isFlipped,
                onStepClick = onStepChange,
                modifier = Modifier.height(240.dp).fillMaxWidth().padding(horizontal = 16.dp),
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(text = "($currentStep / $maxStep$evalText)", style = MaterialTheme.typography.caption)

        // --- 局面が大きく動いた手 ---
        val significantMoves = mutableListOf<Triple<Int, String, Int>>() // step, label, diff
        for (i in 1 until history.size) {
            val prevEval = history[i - 1].evaluation ?: 0
            val curEval = history[i].evaluation ?: continue
            val diff = curEval - prevEval
            if (kotlin.math.abs(diff) >= 500) {
                val moveText = history[i].lastMoveText.trim().split(Regex("\\s+")).getOrNull(1)?.substringBefore("(") ?: ""
                val colorSymbol = if (i % 2 != 0) "▲" else "△"
                significantMoves.add(Triple(i, "$i $colorSymbol$moveText", diff))
            }
        }

        if (significantMoves.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("局面が大きく動いた手", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                significantMoves.forEach { (step, label, diff) ->
                    val diffText = if (diff > 0) "+$diff" else diff.toString()
                    val diffColor = if (diff > 0) ShogiColors.EvalPositive.copy(alpha = 0.8f) else ShogiColors.EvalNegative.copy(alpha = 0.8f)

                    OutlinedButton(
                        onClick = { onStepChange(step) },
                        modifier = Modifier.padding(horizontal = 2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = if (currentStep == step) ButtonDefaults.outlinedButtonColors(backgroundColor = Color.LightGray) else ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(text = diffText, fontSize = 9.sp, color = if (kotlin.math.abs(diff) >= 1000) Color.Red else Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveRow(
    step: Int,
    label: String,
    evaluation: Int?,
    diff: Int?,
    isSelected: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val backgroundColor = if (isSelected) ShogiColors.Primary.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onStepChange(step) }
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (step == 0) "" else step.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.caption,
            color = Color.Gray,
        )

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.body2,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )

        if (evaluation != null) {
            val evalSign = if (evaluation > 0) "+" else ""
            Text(
                text = "$evalSign$evaluation",
                style = MaterialTheme.typography.caption,
                color = if (evaluation > 0) ShogiColors.EvalPositive.copy(alpha = 1f) else ShogiColors.EvalNegative.copy(alpha = 1f),
                modifier = Modifier.width(50.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )

            if (diff != null && diff != 0) {
                val diffSign = if (diff > 0) "+" else ""
                val diffColor = if (diff > 0) ShogiColors.EvalPositive.copy(alpha = 1f) else ShogiColors.EvalNegative.copy(alpha = 1f)
                Text(
                    text = " ($diffSign$diff)",
                    style = MaterialTheme.typography.caption.copy(fontSize = 9.sp),
                    color = diffColor,
                    modifier = Modifier.width(50.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            } else {
                Spacer(Modifier.width(50.dp))
            }
        }
    }
}
