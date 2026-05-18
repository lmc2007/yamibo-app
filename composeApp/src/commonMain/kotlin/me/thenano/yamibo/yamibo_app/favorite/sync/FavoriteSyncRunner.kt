package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.repository.BackgroundTaskRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.time.Duration.Companion.seconds

class FavoriteSyncRunner(
    private val repository: FavoriteSyncRepository,
    private val backgroundTaskRepository: BackgroundTaskRepository,
) {
    sealed interface LaunchResult {
        data class Started(val runId: String) : LaunchResult
        data class Rejected(val reason: String, val runId: String? = null) : LaunchResult
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dismissedFavoritePageRuns = MutableStateFlow<Set<String>>(emptySet())
    private val pendingActivationStartedAt = linkedMapOf<String, Long>()
    private val stateFlow = MutableStateFlow<FavoriteSyncState>(FavoriteSyncState.Idle)

    val state: StateFlow<FavoriteSyncState> = stateFlow.asStateFlow()
    val hiddenFavoritePageRuns: StateFlow<Set<String>> = dismissedFavoritePageRuns.asStateFlow()

    init {
        scope.launch {
            while (true) {
                refreshStateFromRepository()
                delay(1.seconds)
            }
        }
    }

    suspend fun startImport(targetCategoryId: Long): LaunchResult {
        val runningState = state.value as? FavoriteSyncState.Running
        if (runningState != null) {
            return LaunchResult.Started(runningState.snapshot.runId)
        }

        val runId = repository.startRemoteImport(targetCategoryId)
        dismissedFavoritePageRuns.value -= runId
        pendingActivationStartedAt[runId] = currentTimeMillis()
        when (val result = backgroundTaskRepository.startFavoriteSync(runId)) {
            BackgroundTaskRepository.StartResult.Started -> {
                refreshStateFromRepository()
                return LaunchResult.Started(runId)
            }
            is BackgroundTaskRepository.StartResult.Rejected -> {
                pendingActivationStartedAt.remove(runId)
                repository.markRunInterrupted(runId, result.reason)
                refreshStateFromRepository()
                return LaunchResult.Rejected(result.reason, runId)
            }
        }
    }

    suspend fun resumeInterruptedImport(): LaunchResult? {
        val previousRunningRunId = (state.value as? FavoriteSyncState.Running)?.snapshot?.runId
        val runId = repository.resumeInterruptedRun() ?: return null
        if (previousRunningRunId != runId) {
            dismissedFavoritePageRuns.value -= runId
            pendingActivationStartedAt[runId] = currentTimeMillis()
            when (val result = backgroundTaskRepository.startFavoriteSync(runId)) {
                BackgroundTaskRepository.StartResult.Started -> {
                    refreshStateFromRepository()
                    return LaunchResult.Started(runId)
                }
                is BackgroundTaskRepository.StartResult.Rejected -> {
                    pendingActivationStartedAt.remove(runId)
                    repository.markRunInterrupted(runId, result.reason)
                    refreshStateFromRepository()
                    return LaunchResult.Rejected(result.reason, runId)
                }
            }
        }
        refreshStateFromRepository()
        return LaunchResult.Started(runId)
    }

    suspend fun interruptImport(runId: String) {
        backgroundTaskRepository.cancelFavoriteSync(runId)
    }

    fun dismissFavoritePageCard(runId: String) {
        dismissedFavoritePageRuns.value += runId
    }

    private suspend fun refreshStateFromRepository() {
        val runningRunIds = backgroundTaskRepository.runningFavoriteSyncRunIds.value
        pendingActivationStartedAt.entries.removeAll { (runId, startedAt) ->
            runId in runningRunIds || currentTimeMillis() - startedAt > 5000L
        }

        val latest = repository.getLatestSnapshot()
        if (latest?.status == FavoriteSyncRepository.FavoriteSyncStatus.RUNNING &&
            latest.runId !in runningRunIds &&
            latest.runId !in pendingActivationStartedAt.keys
        ) {
            repository.markRunInterrupted(latest.runId, appString(Res.string.ui_the_background_synchronization_task_has_interrupted_can))
        }

        stateFlow.value = repository.getLatestSnapshot()?.let { snapshot ->
            when (snapshot.status) {
                FavoriteSyncRepository.FavoriteSyncStatus.RUNNING -> FavoriteSyncState.Running(snapshot)
                FavoriteSyncRepository.FavoriteSyncStatus.INTERRUPTED -> FavoriteSyncState.Interrupted(snapshot)
                FavoriteSyncRepository.FavoriteSyncStatus.FAILED -> FavoriteSyncState.Failed(snapshot)
                FavoriteSyncRepository.FavoriteSyncStatus.COMPLETED -> FavoriteSyncState.Completed(snapshot)
            }
        } ?: FavoriteSyncState.Idle
    }
}

