package me.thenano.yamibo.yamibo_app.profile.settings.backup

import me.thenano.yamibo.yamibo_app.repository.settings.BackupInterval

interface BackupScheduler {
    suspend fun schedule(interval: BackupInterval)
    suspend fun runNow()
    suspend fun cancel()
}
