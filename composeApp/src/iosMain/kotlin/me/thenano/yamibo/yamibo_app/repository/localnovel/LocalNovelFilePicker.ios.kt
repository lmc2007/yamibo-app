package me.thenano.yamibo.yamibo_app.repository.localnovel

import androidx.compose.runtime.Composable

@Composable
actual fun rememberLocalNovelFilePicker(
    onFilePicked: (LocalNovelFileHandle) -> Unit,
): () -> Unit {
    // TODO: iOS implementation using UIDocumentPickerViewController
    return { /* no-op on iOS for now */ }
}
