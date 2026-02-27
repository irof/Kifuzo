package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

@Composable
@Suppress("LongMethod")
fun KifuTextViewer(
    text: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .size(600.dp, 550.dp)
                .clickable(enabled = false) { },
            elevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(ShogiDimensions.Spacing.Large)) {
                Text(AppStrings.KIFU_TEXT, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(ShogiDimensions.Spacing.Large))

                val scrollSVer = rememberScrollState()
                val scrollSHor = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(ShogiDimensions.Board.CellBorderThickness, Color.LightGray),
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = text,
                            fontSize = ShogiDimensions.Text.Body,
                            fontFamily = FontFamily.Monospace,
                            softWrap = false,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(ShogiDimensions.Spacing.Medium)
                                .verticalScroll(scrollSVer)
                                .horizontalScroll(scrollSHor),
                        )
                    }
                }

                Spacer(Modifier.height(ShogiDimensions.Spacing.Large))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onCopy) { Text(AppStrings.COPY_AND_CLOSE) }
                    Spacer(Modifier.width(ShogiDimensions.Spacing.Medium))
                    TextButton(onClick = onDismiss) { Text(AppStrings.CLOSE) }
                }
            }
        }
    }
}
