package me.thenano.yamibo.yamibo_app.thread.reader.components.overlay

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


/** Floating circle buttons (Refresh & Settings) */
@Composable
fun ReaderFloatButtons(
    visible: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Refresh
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(imageVector = YamiboIcons.Reload, contentDescription = "重新整理", tint = colors.brownPrimary, modifier = Modifier.size(24.dp))
            }
            // Settings
            IconButton(
                onClick = onSettings,
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.brownPrimary.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(imageVector = YamiboIcons.Setting, contentDescription = "設定", tint = colors.brownPrimary, modifier = Modifier.size(28.dp))
            }
        }
    }
}