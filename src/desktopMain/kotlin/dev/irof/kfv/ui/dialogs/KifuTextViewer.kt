package dev.irof.kfv.ui.dialogs

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KifuTextViewer(
    text: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    // 背景（シールド）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // ダイアログ本体
        Card(
            modifier = Modifier
                .size(600.dp, 550.dp)
                .clickable(enabled = false) { },
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("棋譜テキスト", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(16.dp))
                
                val scrollSVer = rememberScrollState()
                val scrollSHor = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
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
                            .horizontalScroll(scrollSHor)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onCopy) { Text("コピーして閉じる") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("閉じる") }
                }
            }
        }
    }
}
