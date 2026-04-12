package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId

/**
 * Repository for tracking reading history and precise scroll positions.
 *
 * Uses content-anchor positioning for accurate reading position restoration.
 */
interface ReadHistoryRepository {

    /** Common wrapper/interface for merging multiple types of reading history */
    sealed interface AnyReadingHistory {
        val lastVisitTime: Long
    }

    enum class ThreadEntryType {
        Normal,
        Novel;

        companion object {
            fun fromStorage(value: String?): ThreadEntryType {
                return entries.firstOrNull { it.name == value } ?: Normal
            }
        }
    }

    /** Full reading history entry with anchor-based positioning data */
    data class ThreadReadingHistory(
        val threadType: ThreadEntryType,
        val threadName: String,
        val threadId: ThreadId,
        val threadCover: String?,
        val forumName: String?,
        val forumId: ForumId?,
        val authorId: UserId?,
        val page: Int,
        val postId: PostId,
        val postTitle: String,

        val anchorPostId: Long,
        val anchorPostRatio: Float? = null,

        val anchorBlockId: String? = null,
        val anchorBlockType: String? = null,
        val anchorBlockRatio: Float? = null,

        val globalScrollY: Int? = null,
        val viewportHeight: Int? = null,
        val firstVisibleItemIndex: Int? = null,
        val firstVisibleItemOffset: Int? = null,

        override val lastVisitTime: Long,
    ) : AnyReadingHistory

    /** Save or update a reading position */
    suspend fun savePosition(history: ThreadReadingHistory)

    /** Get saved position for a given thread */
    suspend fun getPosition(
        tid: ThreadId,
        threadType: ThreadEntryType,
        authorId: UserId? = null,
    ): ThreadReadingHistory?

    /** Get a page of history entries (newest first) */
    suspend fun getHistoryPage(page: Int, pageSize: Int = 20): List<ThreadReadingHistory>

    /** Total number of history entries */
    suspend fun getHistoryCount(): Long

    /** Delete a single history entry */
    suspend fun deleteHistory(
        tid: ThreadId,
        threadType: ThreadEntryType,
        authorId: UserId? = null,
    )

    /** Delete all history entries */
    suspend fun deleteAll()

    /** Search history entries by thread name (newest first) */
    suspend fun searchHistory(query: String, page: Int, pageSize: Int = 20): List<ThreadReadingHistory>

    /** Count search results */
    suspend fun searchHistoryCount(query: String): Long

    /** Delete multiple history entries by thread IDs */
    suspend fun deleteHistoryBatch(items: List<ThreadReadingHistory>)

    // Combined History Queries

    /** Get a combined page of history entries mixing Thread and TagManga (newest first) */
    suspend fun getCombinedHistoryPage(page: Int, pageSize: Int = 20): List<AnyReadingHistory>

    /** Total number of combined history entries */
    suspend fun getCombinedHistoryCount(): Long

    /** Search combined history entries by title/tag (newest first) */
    suspend fun searchCombinedHistory(query: String, page: Int, pageSize: Int = 20): List<AnyReadingHistory>

    /** Count combined search results */
    suspend fun searchCombinedHistoryCount(query: String): Long

    /** Batch delete combined history entries (based on their type-specific IDs) */
    suspend fun deleteCombinedHistoryBatch(items: List<AnyReadingHistory>)

    /** Delete all history entries across all types */
    suspend fun deleteAllCombinedHistory()

    /** Full reading history entry specifically for image posts (manga forum) */
    data class ImageReadingHistory(
        val postId: PostId,
        val threadId: ThreadId,
        val pageIndex: Int,
        val totalPages: Int,
        val firstVisibleItemIndex: Int? = null,
        val firstVisibleItemOffset: Int? = null,
        override val lastVisitTime: Long
    ) : AnyReadingHistory

    /** Save or update a position for an image reading session */
    suspend fun saveImagePosition(history: ImageReadingHistory)

    /** Get saved position for a given post ID */
    suspend fun getImagePosition(postId: PostId): ImageReadingHistory?

    /** Reading history entry for manga tag reading mode (keyed by tagId) */
    data class TagMangaReadingHistory(
        val tagId: TagId,
        val tagName: String,
        val tagPage: Int,
        val threadId: ThreadId,
        val threadTitle: String,
        val threadImagePageIndex: Int,
        val threadImageTotalPages: Int,
        val firstVisibleItemIndex: Int? = null,
        val firstVisibleItemOffset: Int? = null,
        override val lastVisitTime: Long,
        val coverUrl: String? = null
    ) : AnyReadingHistory

    /** Save or update manga tag reading position */
    suspend fun saveTagMangaReaderModeHistory(history: TagMangaReadingHistory)

    /** Get saved manga tag position for a given tag ID */
    suspend fun getTagMangaReaderModeHistoryPosition(tagId: TagId): TagMangaReadingHistory?

    /** Delete manga tag reading history */
    suspend fun deleteMangaTagHistory(tagId: TagId)
}
