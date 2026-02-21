package dev.irof.kfv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.irof.kfv.ui.theme.ShogiColors
import dev.irof.kfv.ui.theme.ShogiDimensions
import dev.irof.kfv.ui.theme.ShogiIcons
import dev.irof.kfv.utils.AppStrings

@Composable
fun KifuMenuBar(
    isSidebarVisible: Boolean,
    onToggleSidebar: () -> Unit,
    onImport: () -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(ShogiDimensions.MenuBarWidth)
            .background(ShogiColors.MenuBarBackground)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MenuIconButton(
            label = if (isSidebarVisible) AppStrings.CLOSE_SIDEBAR else AppStrings.OPEN_SIDEBAR,
            icon = ShogiIcons.SidebarToggle,
            onClick = onToggleSidebar,
            tint = if (isSidebarVisible) ShogiColors.Primary else Color.Gray,
        )

        MenuIconButton(
            label = AppStrings.IMPORT_KIFU,
            icon = ShogiIcons.Import,
            onClick = onImport,
            tint = Color.Gray,
        )

        Spacer(modifier = Modifier.weight(1f))

        MenuIconButton(
            label = AppStrings.SETTINGS,
            icon = ShogiIcons.Settings,
            onClick = onShowSettings,
            tint = Color.Gray,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuIconButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color,
) {
    TooltipArea(
        tooltip = {
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = Color(0xFF333333),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White,
                    fontSize = ShogiDimensions.FontSizeCaption,
                )
            }
        },
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private val AppStrings.OPEN_SIDEBAR: String get() = "サイドバーを開く"
private val AppStrings.CLOSE_SIDEBAR: String get() = "サイドバーを閉じる"
