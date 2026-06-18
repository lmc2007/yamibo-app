package me.thenano.yamibo.yamibo_app.repository

import kotlinx.coroutines.flow.StateFlow
import me.thenano.yamibo.yamibo_app.repository.font.FontLoadResult
import me.thenano.yamibo.yamibo_app.repository.font.LoadedFont

interface FontRepository {
    val fonts: StateFlow<List<LoadedFont>>
    val platformSupportsFontLoading: Boolean
    val platformUnavailableMessage: String?

    suspend fun loadFontFile(sourceUri: String, displayName: String? = null): FontLoadResult
    fun listLoadedFonts(): List<LoadedFont>
    fun deleteFont(id: String): Boolean

    fun getAppFontId(): String
    fun setAppFontId(id: String): Boolean
    fun getReaderFontId(): String
    fun setReaderFontId(id: String): Boolean
}
