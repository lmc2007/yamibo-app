package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore
import kotlin.time.Duration.Companion.hours

import me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory

class IOSThreadRepository(
    private val cookieStore: CookieStore,
    private val yamiboClient: YamiboClient,
    diskCacheFactory: DiskCacheFactory
) : ThreadRepository {
    private val threadCache = diskCacheFactory.create<ThreadPage>("thread_page", maxSize = 50, expiration = 24.hours)

    override suspend fun fetchThread(
        tid: ThreadId,
        authorId: UserId?,
        page: Int
    ): YamiboResult<ThreadPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchThreadById(tid, authorId, page)

        if (result is YamiboResult.Success) {
            val key = ThreadRepository.ThreadCacheKey(tid.value, page, authorId?.value).toCacheKey()
            threadCache.set(key, result.value)
        }
        return result
    }

    override suspend fun fetchFindPost(
        tid: ThreadId,
        postId: PostId
    ): YamiboResult<ThreadPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchFindPost(threadId = tid, postId = postId)
    }

    override suspend fun addFavorite(tid: ThreadId, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchAddFavorite(tid, formHash)
    }

    override suspend fun votePoll(fId: ForumId, tId: ThreadId, pollOptionIds: List<PollOptionId>, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.votePoll(fId, tId, pollOptionIds, formHash)
    }

    override suspend fun ratePost(tId: ThreadId, pId: PostId, score: Int, reason: String, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchRatePost(tId, pId, score, reason, formHash)
    }

    override suspend fun commentPost(tId: ThreadId, pId: PostId, message: String, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchCommentPost(tId, pId, message, formHash)
    }

    override fun getCachedThread(tid: ThreadId, authorId: UserId?, page: Int): ThreadPage? {
        val key = ThreadRepository.ThreadCacheKey(tid.value, page, authorId?.value).toCacheKey()
        return threadCache.get(key)
    }

    override fun setCachedThread(tid: ThreadId, authorId: UserId?, page: Int, threadPage: ThreadPage) {
        val key = ThreadRepository.ThreadCacheKey(tid.value, page, authorId?.value).toCacheKey()
        threadCache.set(key, threadPage)
    }

    override fun clearCachedThread(tid: ThreadId) {
        threadCache.removeByPrefix(ThreadRepository.ThreadCacheKey.keyPrefix(tid.value))
    }
}
