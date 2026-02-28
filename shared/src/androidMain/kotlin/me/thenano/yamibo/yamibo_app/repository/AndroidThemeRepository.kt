package me.thenano.yamibo.yamibo_app.repository

class AndroidThemeRepository : ThemeRepository {

    /** default color scheme */
    private val defaultScheme = YamiboColorScheme()

    override fun getColorScheme(): YamiboColorScheme = defaultScheme
}
