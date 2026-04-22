package me.thenano.yamibo.yamibo_app.favorite.sync

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow
import me.thenano.yamibo.yamibo_app.repository.BackgroundTaskRepository

class AndroidBackgroundTaskRepository(
    context: Context,
) : BackgroundTaskRepository {
    private val appContext = context.applicationContext

    override val runningFavoriteSyncRunIds: StateFlow<Set<String>> =
        FavoriteSyncForegroundService.runningFavoriteSyncRunIds

    override suspend fun startFavoriteSync(runId: String): BackgroundTaskRepository.StartResult {
        if (!AndroidAppForegroundTracker.isForeground()) {
            return BackgroundTaskRepository.StartResult.Rejected("請保持 App 在前景後再開始同步。")
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return BackgroundTaskRepository.StartResult.Rejected("請先允許通知權限，背景同步才會顯示在通知欄。")
        }

        return try {
            val intent = FavoriteSyncForegroundService.createStartIntent(appContext, runId)
            ContextCompat.startForegroundService(appContext, intent)
            BackgroundTaskRepository.StartResult.Started
        } catch (_: Throwable) {
            BackgroundTaskRepository.StartResult.Rejected("目前無法啟動背景同步，請在 App 前景時重試。")
        }
    }

    override suspend fun cancelFavoriteSync(runId: String) {
        val intent = FavoriteSyncForegroundService.createCancelIntent(appContext, runId)
        ContextCompat.startForegroundService(appContext, intent)
    }
}
