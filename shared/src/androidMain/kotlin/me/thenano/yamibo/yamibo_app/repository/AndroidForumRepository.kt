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
import kotlin.time.Duration.Companion.hours

import me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory

class AndroidForumRepository(
    private val cookieStore: CookieStore,
    private val yamiboClient: YamiboClient,
    diskCacheFactory: DiskCacheFactory
) : ForumRepository {

    private val homeCache = diskCacheFactory.create<HomePage>("home_page", maxSize = 1, expiration = 12.hours)
    private val forumCache = diskCacheFactory.create<ForumPage>("forum_page", maxSize = 20, expiration = 24.hours)

    companion object {
        private const val HOME_CACHE_KEY = "main"
    }

    override suspend fun fetchHomePage(): YamiboResult<HomePage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchHomePage()

        if (result is YamiboResult.Success) {
            homeCache.set(HOME_CACHE_KEY, result.value)
        }
        return result
    }

    override suspend fun fetchForum(fid: ForumId, page: Int): YamiboResult<ForumPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchForumById(fid, page)

        if (result is YamiboResult.Success) {
            forumCache.set(ForumRepository.ForumCacheKey(fid.value, page).toCacheKey(), result.value)
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

    override suspend fun addFavorite(forumId: ForumId, formHash: FormHash): YamiboResult<String> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchAddFavorite(forumId, formHash)
    }

    override fun getCachedHomePage(): HomePage? = homeCache.get(HOME_CACHE_KEY)

    override fun getCachedForumPage(fid: ForumId, page: Int): ForumPage? =
        forumCache.get(ForumRepository.ForumCacheKey(fid.value, page).toCacheKey())

    override fun setCachedForumPage(fid: ForumId, page: Int, forumPage: ForumPage) {
        forumCache.set(ForumRepository.ForumCacheKey(fid.value, page).toCacheKey(), forumPage)
    }

    override fun clearCachedForum(fid: ForumId) {
        forumCache.removeByPrefix(ForumRepository.ForumCacheKey.keyPrefix(fid.value))
    }
}
