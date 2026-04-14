package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.scheme.YamiboColorScheme
import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

enum class AppThemeMode(val label: String) {
    SYSTEM("跟隨系統"),
    LIGHT("淺色模式"),
    DARK("深色模式"),
}

enum class AppThemeScheme(val label: String) {
    DEFAULT("預設"),
    DEFAULT_DARK("預設（深色）"),
    CLASSIC_BLACK("經典黑"),
    CLASSIC_WHITE("經典白"),
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
    FIXED_GRID("固定網格"),
    STAGGERED("瀑布貼齊"),
    ROW_CARD("橫排卡片"),
    ROW_CARD_TEXT("橫排卡片(無封面)"),
}

enum class FavoriteSortMode(val label: String) {
    DEFAULT("默認"),
    UPDATED_AT("更新時間"),
    FAVORITED_ORDER("收藏順序"),
    NAME("名稱"),
    FORUM_NAME("版區"),
    LAST_READ("最後一次閱讀"),
}

class AppSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "appsettings") {

    val themeMode by enumSetting(
        name = "主題模式",
        default = AppThemeMode.SYSTEM,
    )

    val themeScheme by enumSetting(
        name = "配色方案",
        default = AppThemeScheme.DEFAULT,
    )

    val isMangaMode by boolSetting(
        name = "漫畫模式",
        default = false,
    )

    val clearCacheOnAppLaunch by boolSetting(
        name = "App 啟動時清除緩存",
        default = false,
    )

    val skipFavoriteRemovalConfirm by boolSetting(
        name = "取消收藏時略過確認",
        default = false,
    )

    val favoriteAddSyncPromptEnabled by boolSetting(
        name = "新增收藏時詢問是否同步到百合會",
        default = true,
    )

    val favoriteAddSyncDefault by boolSetting(
        name = "新增收藏預設同步到百合會",
        default = true,
    )

    val favoriteRemoveSyncPromptEnabled by boolSetting(
        name = "完全移除收藏時詢問是否同步刪除網站收藏",
        default = true,
    )

    val favoriteRemoveSyncDefault by boolSetting(
        name = "完全移除收藏預設同步刪除網站收藏",
        default = false,
    )

    val favoriteGridMode by enumSetting(
        name = "收藏排列方式",
        default = FavoriteGridMode.FIXED_GRID,
    )

    val favoriteSortMode by enumSetting(
        name = "收藏排序方式",
        default = FavoriteSortMode.DEFAULT,
    )

    val favoriteSortDescending by boolSetting(
        name = "收藏排序是否降序",
        default = true,
    )

    companion object {
        val themeModeOptions = AppThemeMode.entries.map { it to it.label }
        val themeSchemeOptions = AppThemeScheme.entries.map { it to it.label }
        val favoriteGridModeOptions = FavoriteGridMode.entries.map { it to it.label }
        val favoriteSortModeOptions = FavoriteSortMode.entries.map { it to it.label }
    }
}
