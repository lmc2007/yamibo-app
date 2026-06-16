package me.thenano.yamibo.yamibo_app.thread.detail.tag.components

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBar

@Composable
fun TagDetailTopBar(
    title: String,
    onBack: () -> Unit
) {
    YamiboTopBar(title = title, titleFontSize = 18, onBack = onBack)
}
