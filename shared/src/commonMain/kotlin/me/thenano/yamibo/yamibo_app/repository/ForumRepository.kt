package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.SearchId

interface ForumRepository {
    data class ForumCacheKey(val fid: Int, val page: Int) {
        fun toCacheKey(): String = "${fid}_${page}"

        companion object {
            fun keyPrefix(fid: Int): String = "${fid}_"
        }
    }

    suspend fun fetchHomePage(): YamiboResult<HomePage>
    suspend fun fetchForum(fid: ForumId, page: Int = 1): YamiboResult<ForumPage>
    suspend fun fetchSearch(
        query: String,
        forumId: ForumId? = null,
        formHash: FormHash
    ): YamiboResult<SearchPage>

    suspend fun fetchSearchById(
        query: String,
        searchId: SearchId,
        page: Int = 1
    ): YamiboResult<SearchPage>

    suspend fun addFavorite(forumId: ForumId, formHash: FormHash): YamiboResult<String>

    fun getCachedHomePage(): HomePage?
    fun getCachedForumPage(fid: ForumId, page: Int = 1): ForumPage?
    fun setCachedForumPage(fid: ForumId, page: Int, forumPage: ForumPage)
    fun clearCachedForum(fid: ForumId)
}
