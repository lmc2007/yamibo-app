package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId

/**
 * Repository for tracking reading history and precise scroll positions.
 *
 * Uses content-anchor positioning for accurate reading position restoration.
 */
interface ReadHistoryRepository {

    /** Full reading history entry with anchor-based positioning data */
    data class ThreadReadingHistory(
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

        val lastVisitTime: Long,
    )

    /** Save or update a reading position */
    suspend fun savePosition(history: ThreadReadingHistory)

    /** Get saved position for a given thread */
    suspend fun getPosition(tid: ThreadId): ThreadReadingHistory?

    /** Get a page of history entries (newest first) */
    suspend fun getHistoryPage(page: Int, pageSize: Int = 20): List<ThreadReadingHistory>

    /** Total number of history entries */
    suspend fun getHistoryCount(): Long

    /** Delete a single history entry */
    suspend fun deleteHistory(tid: ThreadId)

    /** Delete all history entries */
    suspend fun deleteAll()

    /** Search history entries by thread name (newest first) */
    suspend fun searchHistory(query: String, page: Int, pageSize: Int = 20): List<ThreadReadingHistory>

    /** Count search results */
    suspend fun searchHistoryCount(query: String): Long

    /** Delete multiple history entries by thread IDs */
    suspend fun deleteHistoryBatch(tids: List<ThreadId>)

    /** Full reading history entry specifically for image posts (manga forum) */
    data class ImageReadingHistory(
        val postId: PostId,
        val threadId: ThreadId,
        val pageIndex: Int,
        val totalPages: Int,
        val firstVisibleItemIndex: Int? = null,
        val firstVisibleItemOffset: Int? = null,
        val lastVisitTime: Long
    )

    /** Save or update a position for an image reading session */
    suspend fun saveImagePosition(history: ImageReadingHistory)

    /** Get saved position for a given post ID */
    suspend fun getImagePosition(postId: PostId): ImageReadingHistory?
}
