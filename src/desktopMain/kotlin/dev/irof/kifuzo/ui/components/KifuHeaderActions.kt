package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
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
import dev.irof.kifuzo.models.ShogiConstants
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings
import dev.irof.kifuzo.viewmodel.KifuzoUiState
import java.nio.file.Path
import kotlin.io.path.extension

@Composable
fun KifuHeaderActions(
    selectedFile: Path,
    state: KifuzoUiState,
    history: List<BoardSnapshot>,
    onDetectSenkei: (Path) -> Unit,
    onConvertCsa: (Path) -> Unit,
    onRename: (Path) -> Unit,
    onWriteResult: (Path, String) -> Unit,
) {
    val ext = selectedFile.extension.lowercase()
    val isKifuFile = ext == "kifu" || ext == "kif"
    val hasHistory = history.isNotEmpty()

    if (!hasHistory && ext != "csa") return

    Spacer(Modifier.height(ShogiDimensions.PaddingMedium))
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (hasHistory && isKifuFile) {
            SenkeiAction(selectedFile, state.kifuInfos[selectedFile]?.senkei, onDetectSenkei)
            Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
            Button(onClick = { onRename(selectedFile) }, colors = ButtonDefaults.buttonColors(backgroundColor = ShogiColors.Primary, contentColor = Color.White), modifier = Modifier.height(ShogiDimensions.ButtonHeight)) {
                Text(AppStrings.RENAME, fontSize = ShogiDimensions.FontSizeCaption)
            }
            GameResultAction(selectedFile, history.lastOrNull(), onWriteResult)
        }

        if (ext == "csa") {
            if (hasHistory) Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
            Button(onClick = { onConvertCsa(selectedFile) }, colors = ButtonDefaults.buttonColors(backgroundColor = ShogiColors.Success, contentColor = Color.White), modifier = Modifier.height(ShogiDimensions.ButtonHeight)) {
                Text(AppStrings.CONVERT_TO_KIFU, fontSize = ShogiDimensions.FontSizeCaption)
            }
        }
    }
}

@Composable
private fun SenkeiAction(path: Path, existingSenkei: String?, onDetectSenkei: (Path) -> Unit) {
    if (!existingSenkei.isNullOrEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(ShogiDimensions.ButtonHeight).background(Color.White, MaterialTheme.shapes.small).border(ShogiDimensions.CellBorderThickness, Color.LightGray, MaterialTheme.shapes.small).padding(horizontal = ShogiDimensions.PaddingMedium)) {
            Text("戦型: $existingSenkei", fontSize = ShogiDimensions.FontSizeCaption, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            IconButton(onClick = { onDetectSenkei(path) }, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = AppStrings.DETECT_SENKEI, tint = ShogiColors.Info)
            }
        }
    } else {
        Button(onClick = { onDetectSenkei(path) }, colors = ButtonDefaults.buttonColors(backgroundColor = ShogiColors.Info, contentColor = Color.White), modifier = Modifier.height(ShogiDimensions.ButtonHeight)) {
            Text(AppStrings.DETECT_SENKEI, fontSize = ShogiDimensions.FontSizeCaption)
        }
    }
}

@Composable
private fun GameResultAction(path: Path, lastSnapshot: BoardSnapshot?, onWriteResult: (Path, String) -> Unit) {
    val lastMove = lastSnapshot?.lastMoveText ?: ""
    val evaluation = lastSnapshot?.evaluation?.orZero() ?: 0
    val isMate = kotlin.math.abs(evaluation) >= ShogiConstants.MATE_SCORE_THRESHOLD
    val isFinished = isMate || dev.irof.kifuzo.models.GameResult.ALL_KEYWORDS.any { lastMove.contains(it) }

    if (isFinished) return

    Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
    var showMenu by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { showMenu = true }, modifier = Modifier.height(ShogiDimensions.ButtonHeight)) {
            Text("終局手を追加", fontSize = ShogiDimensions.FontSizeCaption)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            dev.irof.kifuzo.models.GameResult.UI_SELECTIONS.forEach { result ->
                DropdownMenuItem(onClick = {
                    showMenu = false
                    onWriteResult(path, result)
                }) {
                    Text(result, fontSize = ShogiDimensions.FontSizeBody)
                }
            }
        }
    }
}
