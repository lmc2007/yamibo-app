package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.SearchId
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore

class AndroidForumRepository(
        private val cookieStore: CookieStore,
        private val yamiboClient: YamiboClient
) : ForumRepository {

    /** in-memory cache */
    private var cachedHomePage: HomePage? = null

    /** forum page cache — keyed by ForumCacheKey */
    private val cachedForumPages = mutableMapOf<ForumRepository.ForumCacheKey, ForumPage>()

    override suspend fun fetchHomePage(): YamiboResult<HomePage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchHomePage()

        if (result is YamiboResult.Success) {
            cachedHomePage = result.value
        }
        return result
    }

    override suspend fun fetchForum(fid: ForumId, page: Int): YamiboResult<ForumPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchForumById(fid, page)

        if (result is YamiboResult.Success) {
            cachedForumPages[ForumRepository.ForumCacheKey(fid.value, page)] = result.value
        }
        return result
    }

    override suspend fun fetchSearch(
            query: String,
            forumId: ForumId?,
            formHash: FormHash
    ): YamiboResult<SearchPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchSearch(query, forumId, formHash)
    }

    override suspend fun fetchSearchById(
            query: String,
            searchId: SearchId,
            page: Int
    ): YamiboResult<SearchPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchSearchById(query, searchId, page)
    }

    override fun getCachedHomePage(): HomePage? = cachedHomePage

    override fun getCachedForumPage(fid: ForumId, page: Int): ForumPage? =
            cachedForumPages[ForumRepository.ForumCacheKey(fid.value, page)]

    override fun clearCachedForum(fid: ForumId) {
        cachedForumPages.keys.removeAll { it.fid == fid.value }
    }
}
