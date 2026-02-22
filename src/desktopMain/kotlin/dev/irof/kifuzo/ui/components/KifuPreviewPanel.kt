package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
                        if (isKifuFile) {
                            val kifuInfo = state.kifuInfos[selected]
                            val existingSenkei = kifuInfo?.senkei
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

                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { onRename(selected) }, colors = ButtonDefaults.buttonColors(backgroundColor = ShogiColors.Primary, contentColor = Color.White), modifier = Modifier.height(32.dp)) {
                                Text(AppStrings.RENAME, fontSize = 10.sp)
                            }

                            // 終局結果ボタンの追加 (終局していない場合のみ)
                            val lastMove = boardState.session.history.lastOrNull()?.lastMoveText ?: ""
                            val isFinished = listOf("投了", "千日手", "持将棋", "切れ負け", "不戦敗", "反則負け", "中断").any { lastMove.contains(it) }
                            if (!isFinished) {
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { onWriteResult(selected, "投了") }, modifier = Modifier.height(32.dp)) {
                                    Text("投了を追加", fontSize = 10.sp)
                                }
                                Spacer(Modifier.width(4.dp))
                                OutlinedButton(onClick = { onWriteResult(selected, "千日手") }, modifier = Modifier.height(32.dp)) {
                                    Text("千日手", fontSize = 10.sp)
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
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 左側: 盤面、スライダー、評価値グラフ
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ShogiBoardView(
                        boardState,
                        isFlipped = state.isFlipped,
                        onToggleFlip = onToggleFlip,
                    )
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
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
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
    val listState = rememberLazyListState()

    LaunchedEffect(currentStep) {
        if (currentStep in 0 until history.size) {
            listState.animateScrollToItem(currentStep)
        }
    }

    Box(
        modifier = modifier
            .background(Color.White, MaterialTheme.shapes.medium)
            .border(1.dp, Color.LightGray, MaterialTheme.shapes.medium),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
        ) {
            item {
                MoveRow(0, AppStrings.START_POSITION, null, null, currentStep == 0, onStepChange)
            }

            items(history.size - 1) { index ->
                val i = index + 1
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

        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
            if (diff != null && kotlin.math.abs(diff) >= 500) {
                val marker = if (kotlin.math.abs(diff) >= 1000) "!!" else "!"
                val markerColor = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(markerColor, shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = marker,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        if (evaluation != null) {
            val evalSign = if (evaluation > 0) "+" else ""
            Text(
                text = "$evalSign$evaluation",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold,
                color = if (evaluation > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative,
                modifier = Modifier.width(50.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )

            if (diff != null && diff != 0) {
                val diffSign = if (diff > 0) "+" else ""
                val diffColor = if (diff > 0) ShogiColors.EvalPositive else ShogiColors.EvalNegative
                Text(
                    text = " ($diffSign$diff)",
                    style = MaterialTheme.typography.caption.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
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
