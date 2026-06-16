package me.thenano.yamibo.yamibo_app.repository.chapterstate

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.LocalChapterStateRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamiboapp.LocalChapterState

class LocalChapterStateRepositoryImpl(
    db: Database,
) : LocalChapterStateRepository {
    private val queries = db.localChapterStateQueries

    override suspend fun getEntry(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
    ): LocalChapterStateRepository.Entry? {
        return queries.getByTarget(targetType.name, parentId, targetId).executeAsOneOrNull()?.toEntry()
    }

    override suspend fun getEntriesByParent(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
    ): List<LocalChapterStateRepository.Entry> {
        return queries.getByParent(targetType.name, parentId).executeAsList().map { it.toEntry() }
    }

    override suspend fun getAllEntries(): List<LocalChapterStateRepository.Entry> {
        return queries.getAll().executeAsList().map { it.toEntry() }
    }

    override suspend fun upsertProgress(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        progressPercent: Int,
        read: Boolean,
        lastPageIndex: Int?,
        totalPages: Int?,
    ) {
        val clampedProgress = progressPercent.coerceIn(0, 100)
        val nextRead = read || clampedProgress >= 100
        val existing = getEntry(targetType, parentId, targetId)
        if (
            existing != null &&
            existing.read == nextRead &&
            existing.progressPercent == clampedProgress &&
            existing.lastPageIndex == lastPageIndex &&
            existing.totalPages == totalPages
        ) {
            return
        }
        queries.upsert(
            targetType = targetType.name,
            parentId = parentId,
            targetId = targetId,
            title = title,
            read = if (nextRead) 1L else 0L,
            progressPercent = if (nextRead) 100L else clampedProgress.toLong(),
            lastPageIndex = lastPageIndex?.toLong(),
            totalPages = totalPages?.toLong(),
            updatedAt = currentTimeMillis(),
        )
    }

    override suspend fun setRead(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        read: Boolean,
    ) {
        val existing = getEntry(targetType, parentId, targetId)
        upsertProgress(
            targetType = targetType,
            parentId = parentId,
            targetId = targetId,
            title = title,
            progressPercent = if (read) 100 else 0,
            read = read,
            lastPageIndex = if (read) existing?.lastPageIndex else null,
            totalPages = if (read) existing?.totalPages else null,
        )
    }

    override suspend fun clearTarget(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
    ) {
        queries.deleteByTarget(targetType.name, parentId, targetId)
    }

    override suspend fun clearParent(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
    ) {
        queries.deleteByParent(targetType.name, parentId)
    }

    private fun LocalChapterState.toEntry(): LocalChapterStateRepository.Entry {
        return LocalChapterStateRepository.Entry(
            targetType = LocalChapterStateRepository.TargetType.fromStorage(targetType),
            parentId = parentId,
            targetId = targetId,
            title = title,
            read = read != 0L,
            progressPercent = progressPercent.toInt(),
            lastPageIndex = lastPageIndex?.toInt(),
            totalPages = totalPages?.toInt(),
            updatedAt = updatedAt,
        )
    }
}
