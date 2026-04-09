package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.LocalMangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.util.state

@Composable
fun MangaReadingModeSetting() {
    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val readingMode = mangaSettingsRepo.readingMode.state()

    SettingsChipRow(
        options = MangaReaderSettingsRepository.readingModeOptions,
        selectedValue = readingMode,
        onSelect = { mangaSettingsRepo.readingMode.setValue(it) }
    )
}

@Composable
fun MangaTouchZoneSetting() {
    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val touchZone = mangaSettingsRepo.touchZone.state()

    SettingsChipRow(
        options = MangaReaderSettingsRepository.touchZoneOptions,
        selectedValue = touchZone,
        onSelect = { mangaSettingsRepo.touchZone.setValue(it) }
    )
}
