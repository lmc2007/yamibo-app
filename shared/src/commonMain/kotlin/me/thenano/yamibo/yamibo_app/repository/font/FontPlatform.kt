package me.thenano.yamibo.yamibo_app.repository.font

import androidx.compose.ui.text.font.FontFamily

interface FontPlatform {
    val supportsFontLoading: Boolean
    val unavailableMessage: String?

    suspend fun importFont(sourceUri: String, displayName: String?, id: String): FontImportResult
    fun deleteFont(font: LoadedFont): Boolean
    fun fontFamily(font: LoadedFont): FontFamily?
}

sealed interface FontImportResult {
    data class Success(
        val name: String,
        val fileName: String,
        val platformPath: String,
    ) : FontImportResult

    data class Unsupported(val message: String) : FontImportResult
    data class Failure(val message: String) : FontImportResult
}
