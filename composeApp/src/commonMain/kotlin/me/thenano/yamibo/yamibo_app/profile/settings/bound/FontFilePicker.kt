package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.compose.runtime.Composable

@Composable
expect fun FontFilePickerButton(
    enabled: Boolean,
    onPicked: (sourceUri: String, displayName: String?) -> Unit,
    onUnavailable: () -> Unit,
)
