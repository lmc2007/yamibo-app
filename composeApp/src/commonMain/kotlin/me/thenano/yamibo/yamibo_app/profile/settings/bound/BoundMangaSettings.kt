package me.thenano.yamibo.yamibo_app.profile.settings.bound

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.LocalMangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.repository.settings.ReadingMode
import me.thenano.yamibo.yamibo_app.repository.settings.TouchZoneLayout
import me.thenano.yamibo.yamibo_app.util.state

@Composable
fun MangaReadingModeSetting() {
    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val readingMode = mangaSettingsRepo.readingMode.state()

    SettingsChipRow(
        options = ReadingMode.entries.map { it to it.localizedLabel() },
        selectedValue = readingMode,
        onSelect = { mangaSettingsRepo.readingMode.setValue(it) }
    )
}

@Composable
fun MangaTouchZoneSetting() {
    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val touchZone = mangaSettingsRepo.touchZone.state()

    SettingsChipRow(
        options = TouchZoneLayout.entries.map { it to it.localizedLabel() },
        selectedValue = touchZone,
        onSelect = { mangaSettingsRepo.touchZone.setValue(it) }
    )
}
