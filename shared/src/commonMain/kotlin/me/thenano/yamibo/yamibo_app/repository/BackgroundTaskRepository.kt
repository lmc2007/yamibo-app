package me.thenano.yamibo.yamibo_app.repository

import kotlinx.coroutines.flow.StateFlow

interface BackgroundTaskRepository {
    sealed interface StartResult {
        data object Started : StartResult
        data class Rejected(
            val reason: String,
            val requiresBackgroundAccessSetup: Boolean = false,
        ) : StartResult
    }

    val runningFavoriteSyncRunIds: StateFlow<Set<String>>

    suspend fun startFavoriteSync(runId: String): StartResult

    suspend fun cancelFavoriteSync(runId: String)
}
