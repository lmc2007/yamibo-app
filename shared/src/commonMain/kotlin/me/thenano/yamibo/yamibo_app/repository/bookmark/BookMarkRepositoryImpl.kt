package me.thenano.yamibo.yamibo_app.repository.bookmark

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.BookMarkRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamiboapp.LocalBookMark

class BookMarkRepositoryImpl(
    db: Database,
) : BookMarkRepository {
    private val queries = db.localBookMarkQueries

    override suspend fun getEntry(
        targetType: BookMarkRepository.TargetType,
        parentId: Long,
        targetId: Long,
    ): BookMarkRepository.Entry? {
        return queries.getByTarget(targetType.name, parentId, targetId).executeAsOneOrNull()?.toEntry()
    }

    override suspend fun getEntriesByParent(
        targetType: BookMarkRepository.TargetType,
        parentId: Long,
    ): List<BookMarkRepository.Entry> {
        return queries.getByParent(targetType.name, parentId).executeAsList().map { it.toEntry() }
    }

    override suspend fun getAllEntries(): List<BookMarkRepository.Entry> {
        return queries.getAll().executeAsList().map { it.toEntry() }
    }

    override suspend fun setBookmarked(
        targetType: BookMarkRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        bookmarked: Boolean,
    ) {
        upsertState(targetType, parentId, targetId, title, bookmarked = bookmarked, read = null)
    }

    override suspend fun setRead(
        targetType: BookMarkRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        read: Boolean,
    ) {
        upsertState(targetType, parentId, targetId, title, bookmarked = null, read = read)
    }

    override suspend fun clearTarget(
        targetType: BookMarkRepository.TargetType,
        parentId: Long,
        targetId: Long,
    ) {
        queries.deleteByTarget(targetType.name, parentId, targetId)
    }

    override suspend fun clearParent(targetType: BookMarkRepository.TargetType, parentId: Long) {
        queries.deleteByParent(targetType.name, parentId)
    }

    private fun upsertState(
        targetType: BookMarkRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        bookmarked: Boolean?,
        read: Boolean?,
    ) {
        val existing = queries.getByTarget(targetType.name, parentId, targetId).executeAsOneOrNull()
        val now = currentTimeMillis()
        val nextBookmarked = bookmarked ?: ((existing?.bookmarked ?: 0L) != 0L)
        val nextRead = read ?: ((existing?.read ?: 0L) != 0L)
        queries.upsert(
            targetType = targetType.name,
            parentId = parentId,
            targetId = targetId,
            title = title.ifBlank { existing?.title.orEmpty() },
            bookmarked = if (nextBookmarked) 1L else 0L,
            read = if (nextRead) 1L else 0L,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        queries.deleteIfEmpty(targetType.name, parentId, targetId)
    }

    private fun LocalBookMark.toEntry(): BookMarkRepository.Entry {
        return BookMarkRepository.Entry(
            targetType = BookMarkRepository.TargetType.fromStorage(targetType),
            parentId = parentId,
            targetId = targetId,
            title = title,
            bookmarked = bookmarked != 0L,
            read = read != 0L,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }
}
