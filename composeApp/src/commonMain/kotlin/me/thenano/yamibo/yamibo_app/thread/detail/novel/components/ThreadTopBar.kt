package me.thenano.yamibo.yamibo_app.thread.detail.novel.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.RowScope
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBar

@Composable
internal fun ThreadTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
    YamiboTopBar(
        title = title,
        titleFontSize = 16,
        onBack = onBack,
        actions = actions,
    )
}
