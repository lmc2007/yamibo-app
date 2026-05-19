package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.scheme.YamiboColorScheme
import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

enum class AppThemeMode(val label: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
}

enum class AppThemeScheme(val label: String) {
    DEFAULT("default"),
    DEFAULT_DARK("default_dark"),
    CLASSIC_BLACK("classic_black"),
    CLASSIC_WHITE("classic_white"),
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

    fun toScheme(): YamiboColorScheme = when (this) {
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

enum class FavoriteGridMode(val label: String) {
    FIXED_GRID("fixed_grid"),
    STAGGERED("staggered"),
    ROW_CARD("row_card"),
    ROW_CARD_TEXT("row_card_text"),
}

enum class FavoriteSortMode(val label: String) {
    DEFAULT("default"),
    UPDATED_AT("updated_at"),
    FAVORITED_ORDER("favorited_order"),
    NAME("name"),
    FORUM_NAME("forum_name"),
    LAST_READ("last_read"),
}

enum class FavoriteUpdateInterval(val label: String, val hours: Long?, val smart: Boolean = false) {
    MANUAL("manual", null),
    HOURS_6("6h", 6L),
    HOURS_12("12h", 12L),
    HOURS_24("24h", 24L),
    DAYS_3("3d", 72L),
    DAYS_7("7d", 168L),
    SMART("smart_todo", null, smart = true),
}

enum class SignInMode(val label: String) {
    SEMI_AUTOMATIC("semi_automatic"),
    FULL_MANUAL("full_manual"),
}

enum class AppLanguage(val label: String, val languageTag: String) {
    TRADITIONAL_CHINESE("zh-TW", "zh-TW"),
    SIMPLIFIED_CHINESE("zh-CN", "zh-CN"),
    ENGLISH("English", "en"),
}

class AppSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "appsettings") {

    val themeMode by enumSetting(
        name = "theme_mode",
        default = AppThemeMode.SYSTEM,
    )

    val themeScheme by enumSetting(
        name = "theme_scheme",
        default = AppThemeScheme.DEFAULT,
    )

    val language by enumSetting(
        name = "language",
        default = AppLanguage.TRADITIONAL_CHINESE,
    )

    val isMangaMode by boolSetting(
        name = "manga_mode",
        default = false,
    )

    val clearCacheOnAppLaunch by boolSetting(
        name = "clear_cache_on_app_launch",
        default = false,
    )

    val skipFavoriteRemovalConfirm by boolSetting(
        name = "skip_favorite_removal_confirm",
        default = false,
    )

    val favoriteAddSyncPromptEnabled by boolSetting(
        name = "favorite_add_sync_prompt_enabled",
        default = true,
    )

    val favoriteAddSyncDefault by boolSetting(
        name = "favorite_add_sync_default",
        default = true,
    )

    val favoriteRemoveSyncPromptEnabled by boolSetting(
        name = "favorite_remove_sync_prompt_enabled",
        default = true,
    )

    val favoriteRemoveSyncDefault by boolSetting(
        name = "favorite_remove_sync_default",
        default = false,
    )

    val favoriteGridMode by enumSetting(
        name = "favorite_grid_mode",
        default = FavoriteGridMode.FIXED_GRID,
    )

    val favoriteSortMode by enumSetting(
        name = "favorite_sort_mode",
        default = FavoriteSortMode.DEFAULT,
    )

    val favoriteSortDescending by boolSetting(
        name = "favorite_sort_descending",
        default = true,
    )

    val favoriteLastCategoryId by intSetting(
        name = "favorite_last_category_id",
        default = 0,
    )

    val favoriteUpdateInterval by enumSetting(
        name = "favorite_update_interval",
        default = FavoriteUpdateInterval.MANUAL,
    )

    val favoriteUpdateHiddenRunId by stringSetting(
        name = "favorite_update_hidden_run_id",
        default = "",
    )

    val signInMode by enumSetting(
        name = "sign_in_mode",
        default = SignInMode.SEMI_AUTOMATIC,
    )

    val signInAllowRepair by boolSetting(
        name = "sign_in_allow_repair",
        default = false,
    )

    val signPageHtmlCache by stringSetting(
        name = "sign_page_html_cache",
        default = "",
    )

    val signPageHtmlCacheUpdatedAt by stringSetting(
        name = "sign_page_html_cache_updated_at",
        default = "",
    )


    companion object {
        @Suppress("unused")
        val themeModeOptions = AppThemeMode.entries.map { it to it.label }
        @Suppress("unused")
        val themeSchemeOptions = AppThemeScheme.entries.map { it to it.label }
        val favoriteGridModeOptions = FavoriteGridMode.entries.map { it to it.label }
        val favoriteSortModeOptions = FavoriteSortMode.entries.map { it to it.label }
        val favoriteUpdateIntervalOptions = FavoriteUpdateInterval.entries.map { it to it.label }
        val signInModeOptions = SignInMode.entries.map { it to it.label }
        val languageOptions = AppLanguage.entries.map { it to it.label }
    }
}

