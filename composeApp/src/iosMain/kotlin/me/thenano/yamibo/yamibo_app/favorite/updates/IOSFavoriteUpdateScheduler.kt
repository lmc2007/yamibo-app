package me.thenano.yamibo.yamibo_app.favorite.updates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval

class IOSFavoriteUpdateScheduler(
    private val repository: FavoriteUpdateRepository,
) : FavoriteUpdateScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = linkedMapOf<String, Job>()

    override suspend fun startFavoriteUpdate(runId: String): FavoriteUpdateScheduler.StartResult {
        if (jobs.containsKey(runId)) return FavoriteUpdateScheduler.StartResult.Started
        jobs[runId] = scope.launch {
            try {
                repository.runUpdate(runId)
            } finally {
                jobs.remove(runId)
            }
        }
        return FavoriteUpdateScheduler.StartResult.Started
    }

    override suspend fun cancelFavoriteUpdate(runId: String) {
        repository.interruptRun(runId)
        jobs.remove(runId)?.cancel()
    }

    override suspend fun schedulePeriodicFavoriteUpdate(interval: FavoriteUpdateInterval) {
        // iOS background execution is best-effort and requires app-side BGTaskScheduler wiring.
        // The repository/runner remains resumable when such a task wakes the app.
    }
}
