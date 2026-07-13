package me.thenano.yamibo.yamibo_app.repository.font

import kotlinx.serialization.Serializable

@Serializable
data class LoadedFont(
    val id: String,
    val name: String,
    val fileName: String,
    val platformPath: String,
    val createdAt: Long,
)

sealed interface FontLoadResult {
    data class Success(val font: LoadedFont) : FontLoadResult
    data class Unsupported(val message: String) : FontLoadResult
    data class Failure(val message: String) : FontLoadResult
}

@Serializable
internal data class LoadedFontList(
    val fonts: List<LoadedFont> = emptyList(),
)
