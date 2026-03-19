package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamiboapp.ReadingHistory

class IOSReadHistoryRepository(dbFactory: DatabaseFactory) : ReadHistoryRepository {

    private val db = Database(dbFactory.createDriver())
    private val queries = db.readingHistoryQueries

    override suspend fun savePosition(history: ReadHistoryRepository.ThreadReadingHistory) {
        queries.upsert(
            threadId = history.threadId.value.toLong(),
            threadName = history.threadName,
            threadCover = history.threadCover,
            forumName = history.forumName,
            forumId = history.forumId?.value?.toLong(),
            authorId = history.authorId?.value?.toLong(),
            page = history.page.toLong(),
            postId = history.postId.value.toLong(),
            postTitle = history.postTitle,
            anchorPostId = history.anchorPostId,
            anchorPostRatio = history.anchorPostRatio?.toDouble(),
            anchorBlockId = history.anchorBlockId,
            anchorBlockType = history.anchorBlockType,
            anchorBlockRatio = history.anchorBlockRatio?.toDouble(),
            globalScrollY = history.globalScrollY?.toLong(),
            viewportHeight = history.viewportHeight?.toLong(),
            firstVisibleItemIndex = history.firstVisibleItemIndex?.toLong(),
            firstVisibleItemOffset = history.firstVisibleItemOffset?.toLong(),
            lastVisitTime = history.lastVisitTime
        )
    }

    override suspend fun getPosition(tid: ThreadId): ReadHistoryRepository.ThreadReadingHistory? {
        return queries.getByThreadId(tid.value.toLong()).executeAsOneOrNull()?.toHistory()
    }

    override suspend fun getHistoryPage(
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.ThreadReadingHistory> {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        return queries.getPage(pageSize.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toHistory() }
    }

    override suspend fun getHistoryCount(): Long {
        return queries.countAll().executeAsOne()
    }

    override suspend fun deleteHistory(tid: ThreadId) {
        queries.deleteByThreadId(tid.value.toLong())
    }

    override suspend fun deleteAll() {
        queries.deleteAll()
    }

    override suspend fun searchHistory(
        query: String,
        page: Int,
        pageSize: Int
    ): List<ReadHistoryRepository.ThreadReadingHistory> {
        val offset = (page - 1).coerceAtLeast(0) * pageSize
        return queries.searchByName(query, pageSize.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toHistory() }
    }

    override suspend fun searchHistoryCount(query: String): Long {
        return queries.countByName(query).executeAsOne()
    }

    override suspend fun deleteHistoryBatch(tids: List<ThreadId>) {
        if (tids.isEmpty()) return
        queries.deleteByThreadIds(tids.map { it.value.toLong() })
    }

    private fun ReadingHistory.toHistory(): ReadHistoryRepository.ThreadReadingHistory {
        return ReadHistoryRepository.ThreadReadingHistory(
            threadName = threadName,
            threadId = ThreadId(threadId.toInt()),
            threadCover = threadCover,
            forumName = forumName,
            forumId = forumId?.toInt()?.let { ForumId(it) },
            authorId = authorId?.toInt()?.let { UserId(it) },
            page = page.toInt(),
            postId = PostId(postId.toInt()),
            postTitle = postTitle,
            anchorPostId = anchorPostId,
            anchorPostRatio = anchorPostRatio?.toFloat(),
            anchorBlockId = anchorBlockId,
            anchorBlockType = anchorBlockType,
            anchorBlockRatio = anchorBlockRatio?.toFloat(),
            globalScrollY = globalScrollY?.toInt(),
            viewportHeight = viewportHeight?.toInt(),
            firstVisibleItemOffset = firstVisibleItemOffset?.toInt(),
            lastVisitTime = lastVisitTime
        )
    }

    private val imageQueries = db.imageReadingHistoryQueries

    override suspend fun saveImagePosition(history: ReadHistoryRepository.ImageReadingHistory) {
        imageQueries.upsert(
            postId = history.postId.value.toLong(),
            threadId = history.threadId.value.toLong(),
            pageIndex = history.pageIndex.toLong(),
            totalPages = history.totalPages.toLong(),
            firstVisibleItemIndex = history.firstVisibleItemIndex?.toLong(),
            firstVisibleItemOffset = history.firstVisibleItemOffset?.toLong(),
            lastVisitTime = history.lastVisitTime
        )
    }

    override suspend fun getImagePosition(postId: PostId): ReadHistoryRepository.ImageReadingHistory? {
        return imageQueries.getByPostId(postId.value.toLong()).executeAsOneOrNull()?.let {
            ReadHistoryRepository.ImageReadingHistory(
                postId = PostId(it.postId.toInt()),
                threadId = ThreadId(it.threadId.toInt()),
                pageIndex = it.pageIndex.toInt(),
                totalPages = it.totalPages.toInt(),
                firstVisibleItemIndex = it.firstVisibleItemIndex?.toInt(),
                firstVisibleItemOffset = it.firstVisibleItemOffset?.toInt(),
                lastVisitTime = it.lastVisitTime
            )
        }
    }
}
