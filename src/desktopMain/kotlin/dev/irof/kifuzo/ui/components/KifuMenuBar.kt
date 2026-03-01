package dev.irof.kifuzo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings

@Composable
fun KifuMenuBar(
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    isMoveListVisible: Boolean,
    onToggleMoveList: () -> Unit,
    onImport: () -> Unit,
    onPaste: () -> Unit,
    onShowSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(ShogiDimensions.Component.MenuBarWidth)
            .fillMaxHeight()
            .background(ShogiColors.Panel.MenuBarBackground)
            .padding(vertical = ShogiDimensions.Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ファイルブラウザ表示切り替え
        IconButton(onClick = onToggleSidebar) {
            Icon(
                imageVector = ShogiIcons.FileBrowser,
                contentDescription = "ファイルブラウザ表示切り替え",
                tint = if (isSidebarVisible) MaterialTheme.colors.primary else Color.Gray,
            )
        }

        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))

        // 手順リスト表示切り替え
        IconButton(onClick = onToggleMoveList) {
            Icon(
                imageVector = ShogiIcons.MoveList,
                contentDescription = "手順リスト表示切り替え",
                tint = if (isMoveListVisible) MaterialTheme.colors.primary else Color.Gray,
            )
        }

        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))

        // インポート
        IconButton(onClick = onImport) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Input,
                contentDescription = AppStrings.IMPORT_KIFU,
                tint = Color.Gray,
            )
        }

        Spacer(Modifier.height(ShogiDimensions.Spacing.Medium))

        // 貼り付け
        IconButton(onClick = onPaste) {
            Icon(
                imageVector = Icons.Default.ContentPaste,
                contentDescription = AppStrings.PASTE_KIFU,
                tint = Color.Gray,
            )
        }

        Spacer(Modifier.weight(1f))

        // 設定
        IconButton(onClick = onShowSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = AppStrings.SETTINGS,
                tint = Color.Gray,
                modifier = Modifier.size(ShogiDimensions.Icon.Medium),
            )
        }
    }
}
