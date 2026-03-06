package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlin.Int

interface ThreadRepository {
    data class ThreadCacheKey(val tid: Int, val page: Int)

    suspend fun fetchThread(
        tid: ThreadId,
        authorId: UserId? = null,
        page: Int = 1
    ): YamiboResult<ThreadPage>

    suspend fun addFavorite(tid: ThreadId, formHash: FormHash): YamiboResult<String>
    fun getCachedThread(tid: ThreadId, page: Int = 1): ThreadPage?
    fun clearCachedThread(tid: ThreadId)
}
