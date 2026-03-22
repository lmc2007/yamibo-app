package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore

class IOSTagRepository(
    private val cookieStore: CookieStore,
    private val yamiboClient: YamiboClient
) : TagRepository {
    private val cachedTagPages = mutableMapOf<TagRepository.TagCacheKey, TagPage>()

    override suspend fun fetchTagPage(tagId: TagId, page: Int): YamiboResult<TagPage> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        val result = yamiboClient.fetchTagPageById(tagId, page)
        if (result is YamiboResult.Success) {
            cachedTagPages[TagRepository.TagCacheKey(tagId.value, page)] = result.value
        }
        return result
    }

    override suspend fun fetchExtractTags(tid: ThreadId): YamiboResult<Tags> {
        yamiboClient.setCookie(cookieStore.load() ?: "")
        return yamiboClient.fetchExtractTagsInThreadById(tid)
    }

    override fun getCachedTagPage(tagId: TagId, page: Int): TagPage? =
        cachedTagPages[TagRepository.TagCacheKey(tagId.value, page)]

    override fun setCachedTagPage(tagId: TagId, page: Int, tagPage: TagPage) {
        cachedTagPages[TagRepository.TagCacheKey(tagId.value, page)] = tagPage
    }

    override fun clearCachedTagPage(tagId: TagId) {
        cachedTagPages.keys.removeAll { it.tagId == tagId.value }
    }
}
