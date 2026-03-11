package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId

interface ThreadRepository {
    data class ThreadCacheKey(val tid: Int, val page: Int)

    suspend fun fetchThread(
        tid: ThreadId,
        authorId: UserId? = null,
        page: Int = 1
    ): YamiboResult<ThreadPage>

    /** Locate a specific post in the full-view thread page */
    suspend fun fetchFindPost(
        tid: ThreadId,
        postId: PostId
    ): YamiboResult<ThreadPage>

    suspend fun addFavorite(tid: ThreadId, formHash: FormHash): YamiboResult<String>
    
    suspend fun votePoll(fId: ForumId, tId: ThreadId, pollOptionIds: List<PollOptionId>, formHash: FormHash): YamiboResult<String>
    suspend fun ratePost(tId: ThreadId, pId: PostId, score: Int, reason: String, formHash: FormHash): YamiboResult<String>
    suspend fun commentPost(tId: ThreadId, pId: PostId, message: String, formHash: FormHash): YamiboResult<String>

    fun getCachedThread(tid: ThreadId, page: Int = 1): ThreadPage?
    fun setCachedThread(tid: ThreadId, page: Int, threadPage: ThreadPage)
    fun clearCachedThread(tid: ThreadId)
}
