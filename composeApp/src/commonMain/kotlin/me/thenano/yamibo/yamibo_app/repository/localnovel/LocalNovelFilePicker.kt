package me.thenano.yamibo.yamibo_app.repository.localnovel

import androidx.compose.runtime.Composable

data class LocalNovelFileHandle(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
)

@Composable
expect fun rememberLocalNovelFilePicker(
    onFilePicked: (LocalNovelFileHandle) -> Unit,
): () -> Unit
