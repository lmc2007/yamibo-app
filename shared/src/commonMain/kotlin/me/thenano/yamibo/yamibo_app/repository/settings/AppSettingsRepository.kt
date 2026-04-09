package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore
import me.thenano.yamibo.yamibo_app.repository.scheme.YamiboColorScheme

enum class AppThemeMode(val label: String) {
    SYSTEM("跟隨系統"),
    LIGHT("淺色模式"),
    DARK("深色模式")
}

enum class AppThemeScheme(val label: String) {
    DEFAULT("百合會"),
    DEFAULT_DARK("百合會(暗色)"),
    CLASSIC_BLACK("傳統黑"),
    CLASSIC_WHITE("傳統白"),
    CATPPUCCIN("Catppuccin"),
    GREEN_APPLE("Green Apple"),
    LAVENDER("Lavender"),
    MIDNIGHT_DUSK("Midnight Dusk"),
    NORD("Nord"),
    STRAWBERRY_DAIQUIRI("Strawberry Daiquiri"),
    TAKO("Tako"),
    TEAL_TURQUOISE("Teal & Turquoise"),
    TIDAL_WAVE("Tidal Wave"),
    YIN_YANG("Yin & Yang"),
    YOTSUBA("Yotsuba"),
    MONOCHROME("Monochrome");

    fun toScheme(): YamiboColorScheme = when(this) {
        DEFAULT -> YamiboColorScheme.Default
        DEFAULT_DARK -> YamiboColorScheme.DefaultDark
        CLASSIC_BLACK -> YamiboColorScheme.ClassicBlack
        CLASSIC_WHITE -> YamiboColorScheme.ClassicWhite
        CATPPUCCIN -> YamiboColorScheme.Catppuccin
        GREEN_APPLE -> YamiboColorScheme.GreenApple
        LAVENDER -> YamiboColorScheme.Lavender
        MIDNIGHT_DUSK -> YamiboColorScheme.MidnightDusk
        NORD -> YamiboColorScheme.Nord
        STRAWBERRY_DAIQUIRI -> YamiboColorScheme.StrawberryDaiquiri
        TAKO -> YamiboColorScheme.Tako
        TEAL_TURQUOISE -> YamiboColorScheme.TealTurquoise
        TIDAL_WAVE -> YamiboColorScheme.TidalWave
        YIN_YANG -> YamiboColorScheme.YinYang
        YOTSUBA -> YamiboColorScheme.Yotsuba
        MONOCHROME -> YamiboColorScheme.Monochrome
    }
}

class AppSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "appsettings") {

    val themeMode by enumSetting(
        name = "顏色主題",
        default = AppThemeMode.SYSTEM
    )

    val themeScheme by enumSetting(
        name = "配色風格",
        default = AppThemeScheme.DEFAULT
    )

    val isMangaMode by boolSetting(
        name = "漫畫模式",
        default = false
    )

    val clearCacheOnAppLaunch by boolSetting(
        name = "App啟動時清除快取",
        default = false
    )

    companion object {
        val themeModeOptions = AppThemeMode.entries.map { it to it.label }
        val themeSchemeOptions = AppThemeScheme.entries.map { it to it.label }
    }
}
