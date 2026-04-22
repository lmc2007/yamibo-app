package me.thenano.yamibo.yamibo_app.favorite.sync

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
        FavoriteSyncState.Idle -> "尚未開始同步"
        is FavoriteSyncState.Running -> "背景同步中"
        is FavoriteSyncState.Interrupted -> "同步已中斷"
        is FavoriteSyncState.Failed -> "同步失敗"
        is FavoriteSyncState.Completed -> "同步完成"
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
            label = "準備同步任務",
            lines = listOf("狀態" to "正在建立同步任務"),
        )

        FavoriteSyncPhase.FETCHING_REMOTE -> SyncProgressUi(
            progress = progress,
            label = "開始同步",
            lines = buildList {
                add("頁數" to if (currentPage <= 0) "正在取得收藏頁" else "${currentPage}/${totalPages ?: "?"} 頁")
                add("已取得" to "$scannedCount 項收藏")
            },
        )

        FavoriteSyncPhase.IMPORTING_REMOTE,
        FavoriteSyncPhase.UPLOADING_LOCAL,
        FavoriteSyncPhase.RECONCILING_REMOTE,
        FavoriteSyncPhase.COMPLETED,
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> SyncProgressUi(
            progress = progress,
            label = "匯入網站帖子",
            lines = buildList {
                add("已匯入到本地" to "$importedCount/${scannedCount.coerceAtLeast(importedCount)}")
                add("已同步至百合會" to uploadedCount.toString())
                add("同步失敗" to failedCount.toString())
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
