package me.thenano.yamibo.yamibo_app.repository.settings

import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

@Serializable
data class NovelReaderSettings(
    val fontSize: Int = 16,
    val lineSpacing: Float = 1.5f,
    val contentWidthFraction: Float = 1.0f
)

class NovelReaderSettingsRepository(store: SettingsStore) : BaseSettingsRepository<NovelReaderSettings>(
    store = store,
    serializer = NovelReaderSettings.serializer(),
    prefix = "novelreadersettings",
    default = NovelReaderSettings()
)
