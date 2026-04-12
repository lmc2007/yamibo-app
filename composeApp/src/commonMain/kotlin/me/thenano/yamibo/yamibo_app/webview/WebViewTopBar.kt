package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import androidx.compose.ui.zIndex

@Composable
fun WebViewTopBar(
    title: String,
    url: String,
    onCloseClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onOpenBrowserClick: () -> Unit = {},
    showNavigation: Boolean = true,
    useBackIcon: Boolean = false,
) {
    val colors = YamiboTheme.colors
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .zIndex(1f),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.brownDeep,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit button (X or Back Arrow)
                Text(
                text = if (useBackIcon) YamiboIcons.Back else "✖",
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clickable { onCloseClick() },
                    fontSize = 20.sp,
                    color = Color.White
                )

                // Title & Subtitle column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.basicMarquee(initialDelayMillis = 1500),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                if (showNavigation) {
                    // Action buttons group
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        Text(
                            text = "←",
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { onBackClick() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        // Forward
                        Text(
                            text = "→",
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable { onForwardClick() },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        // More menu
                        Box {
                            Text(
                                text = "⋮",
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clickable { showMenu = true },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("重新整理", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        onRefreshClick()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("在瀏覽器開啟") },
                                    onClick = {
                                        onOpenBrowserClick()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
