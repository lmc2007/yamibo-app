package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

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
            return BackgroundTaskRepository.StartResult.Rejected(appString(Res.string.ui_please_keep_app_in_foreground_starting_sync))
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return BackgroundTaskRepository.StartResult.Rejected(appString(Res.string.ui_please_allow_notification_permission_first_background_synchronization))
        }

        return try {
            val intent = FavoriteSyncForegroundService.createStartIntent(appContext, runId)
            ContextCompat.startForegroundService(appContext, intent)
            BackgroundTaskRepository.StartResult.Started
        } catch (_: Throwable) {
            BackgroundTaskRepository.StartResult.Rejected(appString(Res.string.ui_background_sync_cannot_started_at_time_try_again_with_app))
        }
    }

    override suspend fun cancelFavoriteSync(runId: String) {
        val intent = FavoriteSyncForegroundService.createCancelIntent(appContext, runId)
        ContextCompat.startForegroundService(appContext, intent)
    }
}

