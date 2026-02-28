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
import dev.irof.kifuzo.models.AppSettings
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun SettingsDialog(
    initialRegex: String,
    initialTemplate: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var tempRegex by remember { mutableStateOf(initialRegex) }
    var tempTemplate by remember { mutableStateOf(initialTemplate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.SETTINGS) },
        modifier = Modifier.width(500.dp),
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NameRegexSection(tempRegex, onValueChange = { tempRegex = it })
                Spacer(Modifier.height(ShogiDimensions.Spacing.Large))
                FilenameTemplateSection(tempTemplate, onValueChange = { tempTemplate = it })
                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(ShogiDimensions.Spacing.Large))
                RawSettingsSection()
            }
        },
        buttons = {
            SettingsFooter(tempRegex, tempTemplate, onDismiss, onSave)
        },
    )
}

@Composable
private fun NameRegexSection(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(AppStrings.MY_NAME_REGEX_LABEL, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("例: (irof|名無し)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(AppStrings.AUTO_FLIP_HINT, fontSize = ShogiDimensions.Text.Caption, color = Color.Gray)
    }
}

@Composable
private fun FilenameTemplateSection(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(AppStrings.FILENAME_TEMPLATE_LABEL, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("例: {開始日の年月日}_{開始日の時分秒}_{棋戦名}_{先手}_{後手}") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(AppStrings.FILENAME_TEMPLATE_HINT, fontSize = ShogiDimensions.Text.Caption, color = Color.Gray)
    }
}

@Composable
private fun RawSettingsSection() {
    var rawSettings by remember { mutableStateOf(AppSettings.Default.getAllSettings()) }
    Column {
        Text(AppStrings.RAW_PREFS_LABEL, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))
        rawSettings.forEach { (key, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = ShogiDimensions.Spacing.Small),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(key, fontSize = ShogiDimensions.Text.Caption, color = Color.Gray)
                    var editingValue by remember(key) { mutableStateOf(value) }
                    BasicTextField(
                        value = editingValue,
                        onValueChange = {
                            editingValue = it
                            AppSettings.Default.putSetting(key, it)
                        },
                        textStyle = TextStyle(fontSize = ShogiDimensions.Text.Body),
                        modifier = Modifier.fillMaxWidth().background(Color.LightGray.copy(alpha = 0.2f)).padding(ShogiDimensions.Spacing.Small),
                    )
                }
                IconButton(onClick = {
                    AppSettings.Default.removeSetting(key)
                    rawSettings = AppSettings.Default.getAllSettings()
                }) {
                    Icon(Icons.Default.Delete, contentDescription = AppStrings.DELETE, tint = Color.Red, modifier = Modifier.size(ShogiDimensions.Icon.Small))
                }
            }
        }
    }
}

@Composable
private fun SettingsFooter(regex: String, template: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(ShogiDimensions.Spacing.Medium), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(AppStrings.CLOSE) }
        Spacer(Modifier.width(ShogiDimensions.Spacing.Medium))
        Button(onClick = { onSave(regex, template) }) { Text(AppStrings.SAVE_SETTINGS) }
    }
}
