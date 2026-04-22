package me.thenano.yamibo.yamibo_app.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FavoriteSyncRepository {
    enum class FavoriteSyncStatus {
        RUNNING,
        INTERRUPTED,
        FAILED,
        COMPLETED,
    }

    enum class FavoriteSyncPhase {
        PREPARING,
        FETCHING_REMOTE,
        IMPORTING_REMOTE,
        UPLOADING_LOCAL,
        RECONCILING_REMOTE,
        INTERRUPTED,
        FAILED,
        COMPLETED,
    }

    data class FavoriteSyncSnapshot(
        val runId: String,
        val status: FavoriteSyncStatus,
        val phase: FavoriteSyncPhase,
        val targetCategoryId: Long,
        val startedAt: Long,
        val updatedAt: Long,
        val lastCompletedAt: Long?,
        val elapsedDurationMs: Long,
        val currentPage: Int,
        val totalPages: Int?,
        val scannedCount: Int,
        val importedCount: Int,
        val uploadedCount: Int,
        val uploadTargetCount: Int,
        val skippedCount: Int,
        val failedCount: Int,
        val logMessage: String?,
        val warningMessage: String?,
        val errorMessage: String?,
    )

    sealed interface FavoriteSyncState {
        data object Idle : FavoriteSyncState
        data class Running(val snapshot: FavoriteSyncSnapshot) : FavoriteSyncState
        data class Interrupted(val snapshot: FavoriteSyncSnapshot) : FavoriteSyncState
        data class Failed(val snapshot: FavoriteSyncSnapshot) : FavoriteSyncState
        data class Completed(val snapshot: FavoriteSyncSnapshot) : FavoriteSyncState
    }

    data class FavoriteSyncDeleteResult(
        val success: Boolean,
        val message: String? = null,
    )

    data class FavoriteSyncActionResult(
        val success: Boolean,
        val message: String? = null,
    )

    data class FavoriteSyncBulkDeleteResult(
        val deletedCount: Int,
        val failedCount: Int,
        val messages: List<String>,
    )
    val state: StateFlow<FavoriteSyncState>

    suspend fun startRemoteImport(targetCategoryId: Long): String
    fun observeRun(runId: String): Flow<FavoriteSyncState>
    suspend fun resumeInterruptedRun(): String?
    suspend fun cancelUiAttachment(runId: String)
    suspend fun interruptRun(runId: String)
    suspend fun markRunInterrupted(runId: String, reason: String)
    suspend fun getLatestSnapshot(): FavoriteSyncSnapshot?
    suspend fun runImport(runId: String)
    suspend fun syncLocalFavoriteItem(itemId: Long): FavoriteSyncActionResult
    suspend fun hasRemoteFavorite(itemId: Long): Boolean
    suspend fun getRemoteFavoriteOrderMap(itemIds: Set<Long>): Map<Long, Long>
    suspend fun removeLocalFavoriteItem(itemId: Long, removeRemote: Boolean = true): FavoriteSyncDeleteResult
    suspend fun removeLocalFavoriteItems(
        itemIds: Set<Long>,
        removeRemote: Boolean = true,
    ): FavoriteSyncBulkDeleteResult
}
