package me.thenano.yamibo.yamibo_app.components.tracking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKeyAt
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

/**
 * Records foreground reader dwell time into the local reading statistics table.
 *
 * Use this once near the top of a native reader screen. The tracker stores only
 * aggregated duration by local date, so Profile statistics can draw reading-time
 * charts without knowing which exact thread was open. Very short sessions are
 * ignored to avoid counting accidental opens or transient recompositions.
 */
@Composable
fun ReadingTimeTracker(active: Boolean = true) {
    if (!active) return

    val readHistoryRepository = LocalReadHistoryRepository.current

    DisposableEffect(readHistoryRepository) {
        val startedAt = currentTimeMillis()
        val dateKey = currentLocalDateKeyAt(startedAt)
        onDispose {
            val durationMillis = currentTimeMillis() - startedAt
            if (durationMillis >= MIN_READING_SESSION_MILLIS) {
                ReadingTimeWriteDispatcher.record(readHistoryRepository, dateKey, durationMillis)
            }
        }
    }
}

private object ReadingTimeWriteDispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun record(
        readHistoryRepository: ReadHistoryRepository,
        dateKey: String,
        durationMillis: Long,
    ) {
        scope.launch {
            readHistoryRepository.recordReadingDuration(dateKey, durationMillis)
        }
    }
}

private const val MIN_READING_SESSION_MILLIS = 5_000L
