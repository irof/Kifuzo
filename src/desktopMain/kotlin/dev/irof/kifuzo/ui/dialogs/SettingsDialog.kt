package dev.irof.kifuzo.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun SettingsDialog(
    initialRegex: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var tempRegex by remember { mutableStateOf(initialRegex) }
    var rawSettings by remember { mutableStateOf(AppSettings.getAllSettings()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.SETTINGS) },
        modifier = Modifier.width(500.dp),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(AppStrings.MY_NAME_REGEX_LABEL, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = tempRegex,
                    onValueChange = { tempRegex = it },
                    placeholder = { Text("例: (irof|名無し)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(AppStrings.AUTO_FLIP_HINT, fontSize = 10.sp, color = Color.Gray)

                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))

                Text(AppStrings.RAW_PREFS_LABEL, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                rawSettings.forEach { (key, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(key, fontSize = 10.sp, color = Color.Gray)
                            var editingValue by remember(key) { mutableStateOf(value) }
                            BasicTextField(
                                value = editingValue,
                                onValueChange = {
                                    editingValue = it
                                    AppSettings.putSetting(key, it)
                                },
                                textStyle = TextStyle(fontSize = 12.sp),
                                modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f)).padding(4.dp),
                            )
                        }
                        IconButton(onClick = {
                            AppSettings.removeSetting(key)
                            rawSettings = AppSettings.getAllSettings()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = AppStrings.DELETE, tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(AppStrings.CLOSE) }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onSave(tempRegex) }) { Text(AppStrings.SAVE_NAME_SETTING) }
            }
        },
    )
}
