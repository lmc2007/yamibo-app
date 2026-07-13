package me.thenano.yamibo.yamibo_app.favorite.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.repository.BackgroundTaskRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository

class IOSBackgroundTaskRepository(
    private val favoriteSyncRepository: FavoriteSyncRepository,
) : BackgroundTaskRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runningRunIds = MutableStateFlow<Set<String>>(emptySet())
    private val jobs = linkedMapOf<String, Job>()

    override val runningFavoriteSyncRunIds: StateFlow<Set<String>> = runningRunIds.asStateFlow()

    override suspend fun startFavoriteSync(runId: String): BackgroundTaskRepository.StartResult {
        if (jobs.containsKey(runId)) return BackgroundTaskRepository.StartResult.Started
        runningRunIds.value += runId
        jobs[runId] = scope.launch {
            try {
                favoriteSyncRepository.runImport(runId)
            } finally {
                jobs.remove(runId)
                runningRunIds.value -= runId
            }
        }
        return BackgroundTaskRepository.StartResult.Started
    }

    override suspend fun cancelFavoriteSync(runId: String) {
        favoriteSyncRepository.interruptRun(runId)
        jobs.remove(runId)?.cancel()
        runningRunIds.value -= runId
    }
}
