package me.thenano.yamibo.yamibo_app.thread.image

import androidx.compose.ui.window.DialogProperties

actual fun imageContextMenuDialogProperties(): DialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = false,
    )
