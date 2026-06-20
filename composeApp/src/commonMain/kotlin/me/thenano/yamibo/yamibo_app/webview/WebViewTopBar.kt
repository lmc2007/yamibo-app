package me.thenano.yamibo.yamibo_app.webview

import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.components.navigation.NavigationBackSymbol

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

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
                    text = if (useBackIcon) NavigationBackSymbol else "✖",
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
                                    text = { Text(i18n("重新整理"), fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        onRefreshClick()
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(i18n("在瀏覽器開啟")) },
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
