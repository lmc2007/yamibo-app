package me.thenano.yamibo.yamibo_app.thread.detail.novel.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBar

@Composable
internal fun ThreadTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    @Suppress("UNUSED_PARAMETER") windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
) {
    YamiboTopBar(
        title = title,
        titleFontSize = 16,
        onBack = onBack,
        actions = actions,
    )
}
