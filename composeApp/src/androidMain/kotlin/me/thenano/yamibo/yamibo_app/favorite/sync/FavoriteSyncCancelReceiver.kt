package me.thenano.yamibo.yamibo_app.favorite.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteSyncCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val runId = intent.getStringExtra(EXTRA_RUN_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AndroidFavoriteSyncSupport.createRepository(context).interruptRun(runId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_CANCEL = "me.thenano.yamibo.yamibo_app.favorite.sync.CANCEL"
        private const val EXTRA_RUN_ID = "run_id"

        fun createPendingIntent(context: Context, runId: String): PendingIntent {
            val intent = Intent(context, FavoriteSyncCancelReceiver::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_RUN_ID, runId)
            }
            return PendingIntent.getBroadcast(
                context,
                runId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
