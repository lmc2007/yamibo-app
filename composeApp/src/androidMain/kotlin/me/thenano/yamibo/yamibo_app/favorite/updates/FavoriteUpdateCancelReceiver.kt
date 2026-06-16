package me.thenano.yamibo.yamibo_app.favorite.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FavoriteUpdateCancelReceiver : BroadcastReceiver() {
    enum class Action(val rawValue: String) {
        Interrupt("interrupt"),
        Cancel("cancel"),
    }

    override fun onReceive(context: Context, intent: Intent) {
        val runId = intent.getStringExtra(EXTRA_RUN_ID) ?: return
        val action = Action.entries.firstOrNull { it.rawValue == intent.getStringExtra(EXTRA_ACTION) } ?: Action.Interrupt
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val repository = AndroidFavoriteUpdateSupport.createRepository(context)
                when (action) {
                    Action.Interrupt -> repository.interruptRun(runId)
                    Action.Cancel -> repository.cancelRun(runId)
                }
                WorkManager.getInstance(context.applicationContext)
                    .cancelAllWorkByTag(AndroidFavoriteUpdateScheduler.WORK_TAG)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_UPDATE_TASK = "me.thenano.yamibo.favorite.update.TASK_ACTION"
        private const val EXTRA_RUN_ID = "run_id"
        private const val EXTRA_ACTION = "action"

        fun createPendingIntent(context: Context, runId: String, action: Action): PendingIntent {
            val intent = Intent(context, FavoriteUpdateCancelReceiver::class.java).apply {
                this.action = ACTION_UPDATE_TASK
                putExtra(EXTRA_RUN_ID, runId)
                putExtra(EXTRA_ACTION, action.rawValue)
            }
            return PendingIntent.getBroadcast(
                context,
                31 * runId.hashCode() + action.rawValue.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
