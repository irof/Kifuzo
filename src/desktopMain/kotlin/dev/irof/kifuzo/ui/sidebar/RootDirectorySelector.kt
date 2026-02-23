package dev.irof.kifuzo.ui.sidebar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.irof.kifuzo.models.AppConfig
import dev.irof.kifuzo.ui.theme.ShogiColors
import dev.irof.kifuzo.ui.theme.ShogiDimensions
import dev.irof.kifuzo.ui.theme.ShogiIcons
import dev.irof.kifuzo.utils.AppStrings
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlin.io.path.toPath

@Composable
fun RootDirectorySelector(
    currentRoot: Path?,
    onSetRoot: (Path) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.weight(1f).clickable {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    val initialDir = currentRoot?.toFile() ?: AppConfig.USER_HOME_PATH.toFile()
                    currentDirectory = initialDir
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    onSetRoot(chooser.selectedFile.toPath())
                }
            },
            elevation = 0.dp,
            backgroundColor = Color.White,
            border = BorderStroke(ShogiDimensions.CellBorderThickness, Color.LightGray),
        ) {
            Row(modifier = Modifier.padding(ShogiDimensions.PaddingMedium), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ShogiIcons.FolderSelect,
                    contentDescription = null,
                    tint = ShogiColors.Primary,
                    modifier = Modifier.size(ShogiDimensions.IconSizeSmall),
                )
                Spacer(Modifier.width(ShogiDimensions.PaddingMedium))
                Text(
                    text = currentRoot?.toString() ?: AppStrings.SELECT_KIFU_ROOT,
                    fontSize = ShogiDimensions.FontSizeCaption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = if (currentRoot == null) Color.Gray else Color.Black,
                )
            }
        }

        if (currentRoot != null) {
            Spacer(Modifier.width(ShogiDimensions.PaddingSmall))
            IconButton(onClick = onRefresh, modifier = Modifier.size(ShogiDimensions.ButtonHeight)) {
                Icon(
                    imageVector = ShogiIcons.Refresh,
                    contentDescription = "再読み込み",
                    tint = ShogiColors.Primary,
                )
            }
        }
    }
}
