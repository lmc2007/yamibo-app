package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId

/**
 * Cache repository for novel thread reading.
 *
 * Manages both full-view ThreadPage cache and per-post comment cache
 * to minimize redundant fetches when browsing novel threads.
 */
interface NovelPrePostCommentsCacheRepository {

    /** Full-view ThreadPage cache (key: tid + page) */
    fun getCachedFullPage(tid: ThreadId, page: Int): ThreadPage?
    fun setCachedFullPage(tid: ThreadId, page: Int, threadPage: ThreadPage)

    /** Per-post comment cache — stores comments belonging to a specific author post */
    fun getCachedComments(tid: ThreadId, postId: PostId): List<Post>?
    fun setCachedComments(tid: ThreadId, postId: PostId, comments: List<Post>)

    /** Whether a post's comments are fully loaded (no more pages to fetch) */
    fun isCommentComplete(tid: ThreadId, postId: PostId): Boolean
    fun setCommentComplete(tid: ThreadId, postId: PostId, complete: Boolean)

    /** Clear all cache for a given thread */
    fun clearCache(tid: ThreadId)
}
