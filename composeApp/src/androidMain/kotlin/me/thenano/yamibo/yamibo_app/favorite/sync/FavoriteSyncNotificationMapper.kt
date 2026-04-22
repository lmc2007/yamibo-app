package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncSnapshot
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState
import me.thenano.yamibo.yamibo_app.repository.SystemNotificationRepository

internal fun FavoriteSyncSnapshot.toNotificationModel(
    state: FavoriteSyncState,
): SystemNotificationRepository.ProgressNotificationModel {
    val progressUi = toProgressUi()
    val summaryText = when (phase) {
        me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncPhase.FETCHING_REMOTE ->
            progressUi.lines.firstOrNull()?.second ?: progressUi.label
        else -> progressUi.lines.joinToString("，") { "${it.first} ${it.second}" }.ifBlank { progressUi.label }
    }
    return SystemNotificationRepository.ProgressNotificationModel(
        notificationId = FavoriteSyncForegroundService.notificationIdFor(runId),
        title = state.title(),
        text = summaryText,
        progress = (progressUi.progress * 100).toInt().coerceIn(0, 100),
        indeterminate = false,
        ongoing = state is FavoriteSyncState.Running,
        canCancel = state is FavoriteSyncState.Running,
        runId = runId,
    )
}
