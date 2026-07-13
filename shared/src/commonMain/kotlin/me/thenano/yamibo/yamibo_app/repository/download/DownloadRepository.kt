package me.thenano.yamibo.yamibo_app.repository.download

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    val queue: StateFlow<List<DownloadQueueEntry>>

    suspend fun isStorageReady(): Boolean
    suspend fun getSummary(): DownloadQueueSummary
    suspend fun getDownloadedContentSummary(): DownloadedContentSummary
    suspend fun getDownloadedContentGroups(): List<DownloadedContentGroup>
    suspend fun getStatus(key: ThreadPageDownloadKey): DownloadStatus
    suspend fun getStatus(key: TagMangaChapterDownloadKey): DownloadStatus
    suspend fun getStatus(key: RssMangaChapterDownloadKey): DownloadStatus
    suspend fun getDownloadedPage(key: ThreadPageDownloadKey): ThreadPage?
    suspend fun getManifest(key: ThreadPageDownloadKey): ThreadPageDownloadManifest?
    suspend fun getTagMangaChapterImages(key: TagMangaChapterDownloadKey): List<String>?
    suspend fun getTagMangaManifest(key: TagMangaChapterDownloadKey): TagMangaChapterManifest?
    suspend fun getRssMangaChapterImages(key: RssMangaChapterDownloadKey): List<String>?
    suspend fun getRssMangaManifest(key: RssMangaChapterDownloadKey): RssMangaChapterManifest?
    suspend fun enqueuePage(tid: ThreadId, title: String, authorId: UserId?, page: Int): Result<Unit>
    suspend fun enqueueThread(tid: ThreadId, title: String, authorId: UserId?): Result<Unit>
    suspend fun enqueueThreadExceptLastPage(tid: ThreadId, title: String, authorId: UserId?): Result<Unit>
    suspend fun enqueueTagMangaChapter(tagId: TagId, tagName: String, thread: ThreadSummary, tagPage: Int): Result<Unit>
    suspend fun enqueueTagMangaCurrentPage(tagId: TagId, tagName: String, threads: List<ThreadSummary>, tagPage: Int): Result<Unit>
    suspend fun enqueueTagMangaAllPages(tagId: TagId, tagName: String): Result<Unit>
    suspend fun enqueueRssMangaChapter(subscriptionId: Long, title: String, query: String, thread: ThreadSummary, page: Int): Result<Unit>
    suspend fun enqueueRssMangaCurrentPage(subscriptionId: Long, title: String, query: String, threads: List<ThreadSummary>, page: Int): Result<Unit>
    suspend fun enqueueRssMangaAllPages(subscriptionId: Long, title: String, query: String): Result<Unit>
    suspend fun refreshPage(tid: ThreadId, title: String, authorId: UserId?, page: Int): YamiboResult<ThreadPage>
    suspend fun refreshTagMangaChapter(key: TagMangaChapterDownloadKey): YamiboResult<List<String>>
    suspend fun refreshRssMangaChapter(key: RssMangaChapterDownloadKey): YamiboResult<List<String>>
    suspend fun markThreadUpdateAvailable(tid: ThreadId, authorId: UserId? = null)
    suspend fun clearPage(key: ThreadPageDownloadKey): Result<Unit>
    suspend fun clearThread(key: ThreadPageDownloadKey): Result<Unit>
    suspend fun clearTagMangaChapter(key: TagMangaChapterDownloadKey): Result<Unit>
    suspend fun clearTagManga(tagId: TagId): Result<Unit>
    suspend fun clearRssMangaChapter(key: RssMangaChapterDownloadKey): Result<Unit>
    suspend fun clearRssManga(subscriptionId: Long): Result<Unit>
    suspend fun retry(key: DownloadTaskKey): Result<Unit>
    fun pauseAll()
    fun resumeAll()
}
