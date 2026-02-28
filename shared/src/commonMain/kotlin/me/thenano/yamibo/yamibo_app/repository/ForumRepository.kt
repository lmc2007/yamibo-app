package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.value.ForumId

interface ForumRepository {
    suspend fun fetchHomePage(): YamiboResult<HomePage>
    suspend fun fetchForum(fid: ForumId, page: Int = 1): YamiboResult<ForumPage>
    fun getCachedHomePage(): HomePage?
    fun getCachedForumPage(fid: ForumId): ForumPage?
}
