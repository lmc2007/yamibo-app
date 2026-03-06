package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore

class IOSThreadRepository(
        private val cookieStore: CookieStore,
        private val yamiboClient: YamiboClient
) : ThreadRepository {
    /** Thread pages cache — keyed by ThreadCacheKey */
    private val cachedThreadPages = mutableMapOf<ThreadRepository.ThreadCacheKey, ThreadPage>()

    override suspend fun fetchThread(
            tid: ThreadId,
            authorId: UserId?,
            page: Int
    ): YamiboResult<ThreadPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchThreadById(tid, authorId, page)

        if (result is YamiboResult.Success) {
            cachedThreadPages[ThreadRepository.ThreadCacheKey(tid.value, page)] = result.value
        }
        return result
    }

    override suspend fun addFavorite(tid: ThreadId, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchAddFavorite(tid, formHash)
    }

    override fun getCachedThread(tid: ThreadId, page: Int): ThreadPage? =
            cachedThreadPages[ThreadRepository.ThreadCacheKey(tid.value, page)]

    override fun clearCachedThread(tid: ThreadId) {
        cachedThreadPages.keys.removeAll { it.tid == tid.value }
    }
}
