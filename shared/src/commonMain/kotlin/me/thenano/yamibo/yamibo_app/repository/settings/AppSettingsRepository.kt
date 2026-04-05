package me.thenano.yamibo.yamibo_app.repository.settings

import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

@Serializable
data class ThemeSettings(
    val mode: String = "SYSTEM",
    val scheme: String = "百合會"
)

@Serializable
data class AppSettings(
    val theme: ThemeSettings = ThemeSettings(),
    val isMangaMode: Boolean = false
)

class AppSettingsRepository(store: SettingsStore) : BaseSettingsRepository<AppSettings>(
    store = store,
    serializer = AppSettings.serializer(),
    prefix = "appsettings",
    default = AppSettings()
)
