package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.Evaluation
import dev.irof.kifuzo.models.Move
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

private const val ICON_ALPHA_INACTIVE = 0.6f
private const val METADATA_BG_ALPHA = 0.5f
private const val METADATA_BORDER_ALPHA = 0.2f

@Composable
fun KifuStepButtons(
    currentStep: Int,
    maxStep: Int,
    isStandardStart: Boolean,
    firstContactStep: Int,
    onStepChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { onStepChange(0) }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight)) { Text(AppStrings.START, fontSize = ShogiDimensions.Text.Caption) }
        Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
        OutlinedButton(onClick = { onStepChange(currentStep - 1) }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight)) { Text("◀", fontSize = ShogiDimensions.Text.Caption) }
        Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
        if (isStandardStart && firstContactStep != -1) {
            Button(onClick = { onStepChange(firstContactStep) }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight)) { Text(AppStrings.CONTACT, fontSize = ShogiDimensions.Text.Caption) }
            Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
        }
        OutlinedButton(onClick = { onStepChange(currentStep + 1) }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight)) { Text("▶", fontSize = ShogiDimensions.Text.Caption) }
        Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
        Button(onClick = { onStepChange(maxStep) }, modifier = Modifier.height(ShogiDimensions.Component.ButtonHeight)) { Text(AppStrings.END, fontSize = ShogiDimensions.Text.Caption) }
    }
}

@Composable
fun KifuGraphs(
    moves: List<Move>,
    currentStep: Int,
    isFlipped: Boolean,
    onStepChange: (Int) -> Unit,
) {
    val allEvaluations = listOf(Evaluation.Unknown) + moves.map { it.evaluation }
    val allConsumptionTimes = listOf(null) + moves.map { it.consumptionSeconds }

    val hasEval = allEvaluations.any { it.isSignificant() }
    val hasTime = allConsumptionTimes.any { it != null && it > 0 }

    if (hasEval || hasTime) {
        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = ShogiDimensions.Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(ShogiDimensions.Spacing.Large),
        ) {
            val targetHeight = if (hasEval && hasTime) ShogiDimensions.Chart.DualHeight else ShogiDimensions.Chart.DefaultHeight
            if (hasEval) {
                EvaluationGraph(allEvaluations, currentStep, isFlipped, onStepChange, Modifier.height(targetHeight).fillMaxWidth())
            }
            if (hasTime) {
                ConsumptionTimeGraph(allConsumptionTimes, currentStep, onStepChange, Modifier.height(targetHeight).fillMaxWidth())
            }
        }
    }
}

@Composable
fun KifuMetaInfo(
    fileName: String,
    senteName: String,
    goteName: String,
    startTime: String,
    event: String,
    warningMessage: String? = null,
    isPasted: Boolean = false,
    onEdit: () -> Unit,
    onOpenExternal: () -> Unit,
    onSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ShogiDimensions.Spacing.Large)
            .background(Color.White.copy(alpha = METADATA_BG_ALPHA), RoundedCornerShape(ShogiDimensions.Corner.Medium))
            .border(1.dp, Color.Gray.copy(alpha = METADATA_BORDER_ALPHA), RoundedCornerShape(ShogiDimensions.Corner.Medium))
            .padding(ShogiDimensions.Spacing.Medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            KifuMetaText(fileName, senteName, goteName, startTime, event, warningMessage, modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPasted) {
                    IconButton(onClick = onSave, modifier = Modifier.size(ShogiDimensions.Icon.Small)) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = AppStrings.SAVE_KIFU,
                            tint = Color.Gray,
                            modifier = Modifier.size(ShogiDimensions.Icon.Small),
                        )
                    }

                    Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
                }

                IconButton(onClick = onOpenExternal, modifier = Modifier.size(ShogiDimensions.Icon.Small)) {
                    Icon(
                        Icons.AutoMirrored.Filled.Launch,
                        contentDescription = AppStrings.OPEN_IN_SHOGIHOME,
                        tint = Color.Gray,
                        modifier = Modifier.size(ShogiDimensions.Icon.Small),
                    )
                }

                Spacer(Modifier.width(ShogiDimensions.Spacing.Small))

                IconButton(onClick = onEdit, modifier = Modifier.size(ShogiDimensions.Icon.Small)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = AppStrings.EDIT_METADATA,
                        tint = Color.Gray,
                        modifier = Modifier.size(ShogiDimensions.Icon.Small),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun KifuMetaText(
    fileName: String,
    sente: String,
    gote: String,
    startTime: String,
    event: String,
    warningMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ShogiDimensions.Spacing.Small),
    ) {
        KifuFileNameWithWarning(fileName, warningMessage)

        if (!isAllMetadataEmpty(sente, gote, startTime, event)) {
            if (sente.isNotEmpty()) MetaRow(AppStrings.LABEL_SENTE, sente)
            if (gote.isNotEmpty()) MetaRow(AppStrings.LABEL_GOTE, gote)
            if (event.isNotEmpty()) MetaRow(AppStrings.LABEL_EVENT, event)
            if (startTime.isNotEmpty()) MetaRow(AppStrings.LABEL_START_TIME, startTime)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun KifuFileNameWithWarning(fileName: String, warningMessage: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )

        if (warningMessage != null) {
            Spacer(Modifier.width(ShogiDimensions.Spacing.Small))
            androidx.compose.foundation.TooltipArea(
                tooltip = {
                    Box(
                        modifier = Modifier
                            .background(Color.DarkGray.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .padding(ShogiDimensions.Spacing.Small),
                    ) {
                        Text(warningMessage, color = Color.White, style = MaterialTheme.typography.caption)
                    }
                },
            ) {
                Icon(
                    imageVector = dev.irof.kifuzo.ui.theme.ShogiIcons.Warning,
                    contentDescription = "Warning",
                    tint = Color.Red,
                    modifier = Modifier.size(ShogiDimensions.Icon.Small),
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row {
        Text(text = label, style = MaterialTheme.typography.caption, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.caption)
    }
}

@Composable
fun KifuMoveSlider(
    currentStep: Int,
    maxStep: Int,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (maxStep <= 0) return
    androidx.compose.material.Slider(
        value = currentStep.toFloat(),
        onValueChange = { onStepChange(it.toInt()) },
        valueRange = 0f..maxStep.toFloat(),
        modifier = modifier,
    )
}

private fun isAllMetadataEmpty(sente: String, gote: String, startTime: String, event: String): Boolean = sente.isEmpty() && gote.isEmpty() && startTime.isEmpty() && event.isEmpty()
