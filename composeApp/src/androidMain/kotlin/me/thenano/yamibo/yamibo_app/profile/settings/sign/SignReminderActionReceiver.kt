package me.thenano.yamibo.yamibo_app.profile.settings.sign

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.SignReminderFrequency
import me.thenano.yamibo.yamibo_app.store.settings.AndroidSettingsStore

class SignReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MUTE_REMINDERS) {
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notificationId != -1) {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(notificationId)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsStore = AndroidSettingsStore(context)
                    val settings = AppSettingsRepository(settingsStore)
                    settings.signInReminderFrequency.setValue(SignReminderFrequency.OFF)

                    val workManager = WorkManager.getInstance(context)
                    workManager.cancelAllWorkByTag(AndroidSignReminderScheduler.WORK_TAG)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_MUTE_REMINDERS = "me.thenano.yamibo.yamibo_app.ACTION_MUTE_SIGN_REMINDERS"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
