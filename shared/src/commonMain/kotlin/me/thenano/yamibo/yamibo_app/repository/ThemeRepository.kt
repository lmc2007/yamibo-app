package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.repository.scheme.YamiboColorScheme

/**
 * Theme repository interface.
 *
 * Provides the current color scheme and allows switching themes.
 */
interface ThemeRepository {
    fun getColorScheme(): YamiboColorScheme
    fun setColorScheme(scheme: YamiboColorScheme)
    fun getAllThemes(): List<YamiboColorScheme>
}
