package me.thenano.yamibo.yamibo_app.repository

import kotlinx.coroutines.flow.Flow

interface ContentCoverRepository {
    enum class TargetType {
        ThreadNormal,
        ThreadNovel,
        TagManga,
        RssSearch,
    }

    data class Key(
        val targetType: TargetType,
        val targetId: Long,
    )

    data class Cover(
        val key: Key,
        val automaticCoverUrl: String?,
        val manualCoverUrl: String?,
        val dynamicEnabled: Boolean,
        val updatedAt: Long,
    ) {
        val resolvedUrl: String?
            get() = if (dynamicEnabled) {
                automaticCoverUrl ?: manualCoverUrl
            } else {
                manualCoverUrl ?: automaticCoverUrl
            }
    }

    fun observeCover(key: Key): Flow<Cover?>

    suspend fun getCover(key: Key): Cover?

    suspend fun setAutomaticCover(key: Key, url: String): Boolean

    suspend fun setManualCover(key: Key, url: String): Boolean

    suspend fun setDynamicEnabled(key: Key, enabled: Boolean)
}