package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId

class AndroidNovelThreadCacheRepository : NovelThreadCacheRepository {

    /** Cache key for full-view pages */
    private data class FullPageKey(val tid: Int, val page: Int)

    /** Cache key for per-post comments */
    private data class CommentKey(val tid: Int, val postId: Int)

    private val fullPageCache = mutableMapOf<FullPageKey, ThreadPage>()
    private val commentCache = mutableMapOf<CommentKey, List<Post>>()
    private val commentCompleteFlags = mutableMapOf<CommentKey, Boolean>()

    override fun getCachedFullPage(tid: ThreadId, page: Int): ThreadPage? =
        fullPageCache[FullPageKey(tid.value, page)]

    override fun setCachedFullPage(tid: ThreadId, page: Int, threadPage: ThreadPage) {
        fullPageCache[FullPageKey(tid.value, page)] = threadPage
    }

    override fun getCachedComments(tid: ThreadId, postId: PostId): List<Post>? =
        commentCache[CommentKey(tid.value, postId.value)]

    override fun setCachedComments(tid: ThreadId, postId: PostId, comments: List<Post>) {
        commentCache[CommentKey(tid.value, postId.value)] = comments
    }

    override fun isCommentComplete(tid: ThreadId, postId: PostId): Boolean =
        commentCompleteFlags[CommentKey(tid.value, postId.value)] ?: false

    override fun setCommentComplete(tid: ThreadId, postId: PostId, complete: Boolean) {
        commentCompleteFlags[CommentKey(tid.value, postId.value)] = complete
    }

    override fun clearCache(tid: ThreadId) {
        fullPageCache.keys.removeAll { it.tid == tid.value }
        commentCache.keys.removeAll { it.tid == tid.value }
        commentCompleteFlags.keys.removeAll { it.tid == tid.value }
    }
}
