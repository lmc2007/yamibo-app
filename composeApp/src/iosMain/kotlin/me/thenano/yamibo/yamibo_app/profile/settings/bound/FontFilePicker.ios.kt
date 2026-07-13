package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.i18n.i18n

@Composable
actual fun FontFilePickerButton(
    enabled: Boolean,
    onPicked: (sourceUri: String, displayName: String?) -> Unit,
    onUnavailable: () -> Unit,
) {
    YamiboActionChip(
        text = i18n("載入字體"),
        onClick = onUnavailable,
    )
}
