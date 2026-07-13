package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.i18n.i18n

@Composable
actual fun FontFilePickerButton(
    enabled: Boolean,
    onPicked: (sourceUri: String, displayName: String?) -> Unit,
    onUnavailable: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(it.toString(), null) }
    }
    YamiboActionChip(
        text = i18n("載入字體"),
        onClick = {
            if (enabled) {
                launcher.launch(
                    arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/x-font-otf",
                        "application/octet-stream",
                    )
                )
            } else {
                onUnavailable()
            }
        },
    )
}
