package dev.irof.kfv.ui.dialogs

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
import androidx.compose.ui.unit.sp
import dev.irof.kfv.utils.AppStrings

@Composable
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(AppStrings.KIFU_TEXT, style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(16.dp))

                val scrollSVer = rememberScrollState()
                val scrollSHor = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(1.dp, Color.LightGray),
                ) {
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        softWrap = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalScroll(scrollSVer)
                            .horizontalScroll(scrollSHor),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onCopy) { Text(AppStrings.COPY_AND_CLOSE) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text(AppStrings.CLOSE) }
                }
            }
        }
    }
}
