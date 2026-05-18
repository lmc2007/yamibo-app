package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncPhase
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncSnapshot
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState

internal data class SyncProgressUi(
    val progress: Float,
    val label: String,
    val lines: List<Pair<String, String>>,
)

internal fun FavoriteSyncState.snapshotOrNull(): FavoriteSyncSnapshot? {
    return when (this) {
        FavoriteSyncState.Idle -> null
        is FavoriteSyncState.Running -> snapshot
        is FavoriteSyncState.Interrupted -> snapshot
        is FavoriteSyncState.Failed -> snapshot
        is FavoriteSyncState.Completed -> snapshot
    }
}

internal fun FavoriteSyncState.title(): String {
    return when (this) {
        FavoriteSyncState.Idle -> appString(Res.string.ui_sync_has_not_started_yet)
        is FavoriteSyncState.Running -> appString(Res.string.ui_background_sync_in_progress)
        is FavoriteSyncState.Interrupted -> appString(Res.string.ui_sync_interrupted)
        is FavoriteSyncState.Failed -> appString(Res.string.ui_sync_failed)
        is FavoriteSyncState.Completed -> appString(Res.string.ui_synchronization_completed)
    }
}

internal fun FavoriteSyncSnapshot.toProgressUi(): SyncProgressUi {
    val safeScannedCount = scannedCount.coerceAtLeast(1)
    val importedProcessed = (importedCount + failedCount).coerceAtMost(safeScannedCount)
    val uploadProgress = when {
        uploadTargetCount <= 0 && phase != FavoriteSyncPhase.PREPARING && phase != FavoriteSyncPhase.FETCHING_REMOTE -> 1f
        uploadTargetCount > 0 -> (uploadedCount.toFloat() / uploadTargetCount.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    val fetchProgress = totalPages
        ?.takeIf { it > 0 }
        ?.let { (currentPage.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        ?: if (currentPage > 0) 0.15f else 0f

    val progress = when (phase) {
        FavoriteSyncPhase.PREPARING -> 0.03f
        FavoriteSyncPhase.FETCHING_REMOTE -> 0.05f + (0.35f * fetchProgress)
        FavoriteSyncPhase.IMPORTING_REMOTE -> 0.40f + (0.40f * (importedProcessed.toFloat() / safeScannedCount.toFloat()))
        FavoriteSyncPhase.UPLOADING_LOCAL -> 0.80f + (0.15f * uploadProgress)
        FavoriteSyncPhase.RECONCILING_REMOTE -> 0.95f
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> snapshotFrozenProgress()
        FavoriteSyncPhase.COMPLETED -> 1f
    }.coerceIn(0f, 1f)

    return when (phase) {
        FavoriteSyncPhase.PREPARING -> SyncProgressUi(
            progress = progress,
            label = appString(Res.string.ui_prepare_synchronization_tasks),
            lines = listOf(appString(Res.string.ui_state) to appString(Res.string.ui_creating_synchronization_task)),
        )

        FavoriteSyncPhase.FETCHING_REMOTE -> SyncProgressUi(
            progress = progress,
            label = appString(Res.string.ui_start_syncing),
            lines = buildList {
                add(appString(Res.string.ui_number_pages) to if (currentPage <= 0) appString(Res.string.ui_getting_favorites) else appString(Res.string.favorite_sync_page_progress, currentPage.toString(), totalPages?.toString() ?: "?"))
                add(appString(Res.string.ui_obtained) to appString(Res.string.favorite_sync_scanned_count, scannedCount))
            },
        )

        FavoriteSyncPhase.IMPORTING_REMOTE,
        FavoriteSyncPhase.UPLOADING_LOCAL,
        FavoriteSyncPhase.RECONCILING_REMOTE,
        FavoriteSyncPhase.COMPLETED,
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> SyncProgressUi(
            progress = progress,
            label = appString(Res.string.ui_import_website_posts),
            lines = buildList {
                add(appString(Res.string.ui_imported_local) to "$importedCount/${scannedCount.coerceAtLeast(importedCount)}")
                add(appString(Res.string.ui_already_synced_yamibo_2) to uploadedCount.toString())
                add(appString(Res.string.ui_sync_failed) to failedCount.toString())
            },
        )
    }
}

private fun FavoriteSyncSnapshot.snapshotFrozenProgress(): Float {
    return when {
        totalPages != null && totalPages!! > 0 && currentPage > 0 ->
            (0.05f + (0.35f * (currentPage.toFloat() / totalPages!!.toFloat()))).coerceIn(0f, 0.95f)
        scannedCount > 0 -> (0.40f + (0.40f * ((importedCount + failedCount).toFloat() / scannedCount.toFloat()))).coerceIn(0f, 0.95f)
        else -> 0.05f
    }
}


