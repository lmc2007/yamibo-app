package me.thenano.yamibo.yamibo_app.profile.settings.backup

import android.content.Context
import me.thenano.yamibo.yamibo_app.AppVersion
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.AndroidBackupStorageProvider
import me.thenano.yamibo.yamibo_app.repository.backup.BackupRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.store.settings.AndroidSettingsStore

internal object AndroidBackupSupport {
    fun createRepository(context: Context): BackupRepositoryImpl {
        val appContext = context.applicationContext
        val settingsStore = AndroidSettingsStore(appContext)
        val appSettingsRepository = AppSettingsRepository(settingsStore)
        val novelSettingsRepository = NovelReaderSettingsRepository(settingsStore)
        val mangaSettingsRepository = MangaReaderSettingsRepository(settingsStore)
        val db = Database(DatabaseFactory(appContext).createDriver())
        return BackupRepositoryImpl(
            db = db,
            settingsStore = settingsStore,
            settingsRegistries = listOf(appSettingsRepository, novelSettingsRepository, mangaSettingsRepository),
            storageProvider = AndroidBackupStorageProvider(appContext, appSettingsRepository),
            appVersionCode = AppVersion.VersionCode.toInt(),
        )
    }
}
