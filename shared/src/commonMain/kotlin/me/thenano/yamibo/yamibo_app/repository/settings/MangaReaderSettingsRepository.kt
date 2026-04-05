package me.thenano.yamibo.yamibo_app.repository.settings

import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

@Serializable
data class MangaReaderSettings(
    val readingMode: String = "SINGLE_LTR",
    val touchZone: String = "L_SHAPE"
)

class MangaReaderSettingsRepository(store: SettingsStore) : BaseSettingsRepository<MangaReaderSettings>(
    store = store,
    serializer = MangaReaderSettings.serializer(),
    prefix = "mangareadersettings",
    default = MangaReaderSettings()
)
