package me.thenano.yamibo.yamibo_app.favorite.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState

class FavoriteSyncRunner(
    private val repository: FavoriteSyncRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runningJobs = linkedMapOf<String, Job>()
    private val dismissedFavoritePageRuns = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<FavoriteSyncState> = repository.state
    val hiddenFavoritePageRuns: StateFlow<Set<String>> = dismissedFavoritePageRuns.asStateFlow()

    suspend fun startImport(targetCategoryId: Long): String {
        val runningState = state.value as? FavoriteSyncState.Running
        if (runningState != null) {
            return runningState.snapshot.runId
        }

        val runId = repository.startRemoteImport(targetCategoryId)
        dismissedFavoritePageRuns.value -= runId
        runningJobs.remove(runId)?.cancel()
        runningJobs[runId] = scope.launch {
            repository.runImport(runId)
        }
        return runId
    }

    suspend fun resumeInterruptedImport(): String? {
        val previousRunningRunId = (state.value as? FavoriteSyncState.Running)?.snapshot?.runId
        val runId = repository.resumeInterruptedRun() ?: return null
        if (previousRunningRunId != runId) {
            dismissedFavoritePageRuns.value -= runId
            runningJobs.remove(runId)?.cancel()
            runningJobs[runId] = scope.launch {
                repository.runImport(runId)
            }
        }
        return runId
    }

    suspend fun interruptImport(runId: String) {
        repository.interruptRun(runId)
        runningJobs.remove(runId)?.cancel()
    }

    fun dismissFavoritePageCard(runId: String) {
        dismissedFavoritePageRuns.value += runId
    }
}
