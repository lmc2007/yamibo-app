package me.thenano.yamibo.yamibo_app.components.font

import androidx.compose.ui.text.font.FontFamily
import me.thenano.yamibo.yamibo_app.repository.FontRepository
import me.thenano.yamibo.yamibo_app.repository.font.LoadedFont

private val fontFamilyCache = mutableMapOf<String, FontFamily?>()

fun FontRepository.getFontFamily(id: String): FontFamily? {
    if (id.isBlank()) return null
    val font = listLoadedFonts().firstOrNull { it.id == id } ?: return null
    return fontFamilyCache.getOrPut(font.platformPath) { platformFontFamily(font) }
}

internal expect fun platformFontFamily(font: LoadedFont): FontFamily?
