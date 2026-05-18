package me.thenano.yamibo.yamibo_app.favorite.updates

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.thenano.yamibo.yamibo_app.MainActivity
import me.thenano.yamibo.yamibo_app.R
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository.RunSnapshot

internal class AndroidFavoriteUpdateNotificationRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    init {
        ensureChannel()
    }

    fun buildProgressNotification(snapshot: RunSnapshot): Notification {
        val total = snapshot.totalCount.coerceAtLeast(0)
        val processed = (snapshot.completedCount + snapshot.skippedCount + snapshot.failedCount).coerceAtMost(total)
        val progress = if (total > 0) ((processed * 100f) / total).toInt().coerceIn(0, 100) else 0
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(appString(Res.string.ui_favorite_updates))
            .setContentText(snapshot.currentItem?.let(::localizedAppMessage) ?: appString(Res.string.ui_checking_for_favorites_updates))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, total == 0)
            .setContentIntent(createOpenAppPendingIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, appString(Res.string.common_cancel), FavoriteUpdateCancelReceiver.createPendingIntent(appContext, snapshot.runId))
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showCompleted(snapshot: RunSnapshot) {
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(appString(Res.string.ui_favorite_update_completed))
                .setContentText(if (snapshot.detectedCount > 0) appString(Res.string.favorite_update_notification_detected, snapshot.detectedCount) else appString(Res.string.ui_no_updates_detected))
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(createOpenAppPendingIntent())
                .setProgress(0, 0, false)
                .build(),
        )
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showFailed(title: String, text: String) {
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(localizedAppMessage(text))
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(createOpenAppPendingIntent())
                .setProgress(0, 0, false)
                .build(),
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appString(Res.string.ui_favorite_updates),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = appString(Res.string.ui_display_favorite_update_check_progress_results)
            },
        )
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "favorite_update_channel"
        const val NOTIFICATION_ID = 228120
    }
}


