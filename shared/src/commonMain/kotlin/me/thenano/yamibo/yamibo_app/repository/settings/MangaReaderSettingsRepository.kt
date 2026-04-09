package me.thenano.yamibo.yamibo_app.repository.settings

import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingsRegistry
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

/** Reading mode configuration for manga reader. */
enum class ReadingMode(val label: String) {
    SINGLE_LTR("單頁(左至右)"),
    SINGLE_RTL("單頁(右至左)"),
    SINGLE_TTB("單頁(上至下)"),
    SCROLL_CONTINUOUS("捲動(連續)"),
    SCROLL_GAP("捲動(留空)")
}

/** Touch zone layout options for manga reader navigation. */
enum class TouchZoneLayout(val label: String) {
    L_SHAPE("L式"),
    KINDLE("Kindle式"),
    EDGE("邊緣式"),
    LEFT_RIGHT("左右式"),
    DISABLED("停用")
}

class MangaReaderSettingsRepository(store: SettingsStore) : SettingsRegistry(store, prefix = "mangareadersettings") {

    val readingMode by enumSetting(
        name = "閱讀模式",
        default = ReadingMode.SINGLE_LTR
    )

    val touchZone by enumSetting(
        name = "輕觸區域",
        default = TouchZoneLayout.L_SHAPE
    )

    companion object {
        val readingModeOptions = ReadingMode.entries.map { it to it.label }
        val touchZoneOptions = TouchZoneLayout.entries.map { it to it.label }
    }
}
