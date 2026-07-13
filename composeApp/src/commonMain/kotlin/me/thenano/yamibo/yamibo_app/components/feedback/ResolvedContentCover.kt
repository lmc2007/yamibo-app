package me.thenano.yamibo.yamibo_app.components.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import me.thenano.yamibo.yamibo_app.LocalContentCoverRepository
import me.thenano.yamibo.yamibo_app.repository.ContentCoverRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteStoreRepository

@Composable
fun resolvedContentCoverUrl(
    targetType: FavoriteStoreRepository.FavoriteTargetType,
    targetId: Long,
    fallback: String?,
): String? {
    val coverTargetType = remember(targetType) {
        ContentCoverRepository.TargetType.entries.firstOrNull { it.name == targetType.name }
    } ?: return fallback
    val repository = LocalContentCoverRepository.current
    val key = remember(coverTargetType, targetId) {
        ContentCoverRepository.Key(
            targetType = coverTargetType,
            targetId = targetId,
        )
    }
    val cover by repository.observeCover(key).collectAsState(null)
    return cover?.resolvedUrl ?: fallback
}

