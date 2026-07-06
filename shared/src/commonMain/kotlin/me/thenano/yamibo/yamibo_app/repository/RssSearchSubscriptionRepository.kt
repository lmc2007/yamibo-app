package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.value.ForumId
import kotlinx.coroutines.flow.StateFlow

interface RssSearchSubscriptionRepository {
    val subscriptions: StateFlow<List<SubscriptionSummary>>
    val unreadCount: StateFlow<Int>

    data class SubscriptionSummary(
        val id: Long,
        val title: String,
        val query: String,
        val forumId: ForumId?,
        val forumName: String?,
        val enabled: Boolean,
        val createdAt: Long,
        val updatedAt: Long,
        val lastRefreshStartedAt: Long?,
        val lastRefreshFinishedAt: Long?,
        val lastRefreshStatus: RefreshStatus?,
        val lastRefreshMessage: String?,
        val lastTotalCount: Int,
        val unreadCount: Int,
    )

    data class CatalogPage(
        val subscription: SubscriptionSummary,
        val tagPage: TagPage,
        val readThreadIds: Set<Long>,
        val totalResults: Int,
    )

    data class RefreshSummary(
        val subscriptionId: Long,
        val fetchedCount: Int,
        val newCount: Int,
        val totalCount: Int,
    )

    enum class RefreshStatus {
        Running,
        Success,
        Failed,
    }

    suspend fun createFromSearch(
        title: String,
        query: String,
        forumId: ForumId?,
        forumName: String?,
        searchPage: SearchPage,
    ): YamiboResult<Long>

    suspend fun ensureSubscription(
        query: String,
        forumId: ForumId?,
        forumName: String?,
    ): YamiboResult<Long>

    suspend fun findBySearch(query: String, forumId: ForumId?): SubscriptionSummary?
    suspend fun refresh(subscriptionId: Long): YamiboResult<RefreshSummary>
    suspend fun refreshAllEnabled(): List<YamiboResult<RefreshSummary>>
    suspend fun getSubscription(subscriptionId: Long): SubscriptionSummary?
    suspend fun getCatalogPage(subscriptionId: Long, page: Int, pageSize: Int = 30): CatalogPage?
    suspend fun getCachedCatalogPage(subscriptionId: Long, page: Int, pageSize: Int = 30): CatalogPage?
    suspend fun markRead(subscriptionId: Long, threadId: Long)
    suspend fun markUnread(subscriptionId: Long, threadId: Long)
    suspend fun rename(subscriptionId: Long, title: String)
    suspend fun setEnabled(subscriptionId: Long, enabled: Boolean)
    suspend fun delete(subscriptionId: Long)
}
