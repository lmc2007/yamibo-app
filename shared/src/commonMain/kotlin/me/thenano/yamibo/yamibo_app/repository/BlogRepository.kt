package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.BlogPage
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId

interface BlogRepository {
    data class BlogPageCacheKey(val blogId: Int, val userId: Int?, val page: Int) {
        fun toCacheKey(): String = "${blogId}_${userId ?: "self"}_$page"

        companion object {
            fun keyPrefix(blogId: Int): String = "${blogId}_"
        }
    }

    suspend fun fetchBlogPage(blogId: BlogId, userId: UserId? = null, page: Int = 1): YamiboResult<BlogPage>

    suspend fun postBlogComment(
        blogId: BlogId,
        userId: UserId,
        message: String,
        formHash: FormHash
    ): YamiboResult<String>

    fun getCachedBlogPage(blogId: BlogId, userId: UserId? = null, page: Int = 1): BlogPage?
    fun clearCachedBlog(blogId: BlogId)
}
