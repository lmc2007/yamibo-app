package me.thenano.yamibo.yamibo_app.thread.reader.components.overlay

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme


/** Floating circle buttons (Refresh, Settings, and optionally Manga Reader) */
@Composable
fun ReaderFloatButtons(
    visible: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    showMangaReader: Boolean = false,
    onMangaReader: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
    ) {
        // Refresh & Settings: tied to overlay visibility
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(imageVector = YamiboIcons.Reload, contentDescription = appString(Res.string.ui_refresh), tint = colors.brownPrimary, modifier = Modifier.size(24.dp))
                }
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .size(56.dp)
                        .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(imageVector = YamiboIcons.Setting, contentDescription = appString(Res.string.settings_title), tint = colors.brownPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Manga Reader: always visible when showMangaReader is true (independent of overlay)
        if (showMangaReader) {
            IconButton(
                onClick = onMangaReader,
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(imageVector = YamiboIcons.Book, contentDescription = appString(Res.string.ui_comic_reading_mode), tint = colors.brownPrimary, modifier = Modifier.size(24.dp))
            }
        }
    }
}
