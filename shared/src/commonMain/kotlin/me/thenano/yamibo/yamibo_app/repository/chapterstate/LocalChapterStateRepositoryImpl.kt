package me.thenano.yamibo.yamibo_app.repository.chapterstate

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.LocalChapterStateRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamiboapp.LocalChapterState

class LocalChapterStateRepositoryImpl(
    private val db: Database,
) : LocalChapterStateRepository {
    private val queries = db.localChapterStateQueries

    override suspend fun getEntry(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
    ): LocalChapterStateRepository.Entry? = withContext(Dispatchers.IO) {
        queries.getByTarget(targetType.name, parentId, targetId).executeAsOneOrNull()?.toEntry()
    }

    override suspend fun getEntriesByParent(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
    ): List<LocalChapterStateRepository.Entry> = withContext(Dispatchers.IO) {
        queries.getByParent(targetType.name, parentId).executeAsList().map { it.toEntry() }
    }

    override suspend fun getAllEntries(): List<LocalChapterStateRepository.Entry> = withContext(Dispatchers.IO) {
        queries.getAll().executeAsList().map { it.toEntry() }
    }

    override fun observeEntriesByParent(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
    ): Flow<Map<Long, LocalChapterStateRepository.Entry>> =
        queries.getByParent(targetType.name, parentId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entries -> entries.associate { it.targetId to it.toEntry() } }
            .distinctUntilChanged()

    override suspend fun upsertProgress(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        progressPercent: Int,
        read: Boolean,
        lastPageIndex: Int?,
        totalPages: Int?,
    ): Unit = withContext(Dispatchers.IO) {
        val clampedProgress = progressPercent.coerceIn(0, 100)
        val nextRead = read || clampedProgress >= 100
        val existing = queries.getByTarget(targetType.name, parentId, targetId).executeAsOneOrNull()
        val existingEntry = existing?.toEntry()
        if (
            existingEntry != null &&
            existingEntry.read == nextRead &&
            existingEntry.progressPercent == clampedProgress &&
            existingEntry.lastPageIndex == lastPageIndex &&
            existingEntry.totalPages == totalPages
        ) {
            return@withContext
        }
        queries.upsert(
            targetType = targetType.name,
            parentId = parentId,
            targetId = targetId,
            title = title.ifBlank { existing?.title.orEmpty() },
            read = if (nextRead) 1L else 0L,
            progressPercent = if (nextRead) 100L else clampedProgress.toLong(),
            lastPageIndex = lastPageIndex?.toLong(),
            totalPages = totalPages?.toLong(),
            updatedAt = currentTimeMillis(),
        )
        Unit
    }

    override suspend fun setRead(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        read: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val existing = queries.getByTarget(targetType.name, parentId, targetId).executeAsOneOrNull()?.toEntry()
        val nextLastPageIndex = if (read) existing?.lastPageIndex else null
        val nextTotalPages = if (read) existing?.totalPages else null
        queries.upsert(
            targetType = targetType.name,
            parentId = parentId,
            targetId = targetId,
            title = title.ifBlank { existing?.title.orEmpty() },
            read = if (read) 1L else 0L,
            progressPercent = if (read) 100L else 0L,
            lastPageIndex = nextLastPageIndex?.toLong(),
            totalPages = nextTotalPages?.toLong(),
            updatedAt = currentTimeMillis(),
        )
        Unit
    }

    override suspend fun applyProgressUpdates(
        updates: List<LocalChapterStateRepository.ProgressUpdate>,
    ): Unit = withContext(Dispatchers.IO) {
        if (updates.isEmpty()) return@withContext
        db.transaction {
            updates.forEach { update ->
                val clampedProgress = update.progressPercent.coerceIn(0, 100)
                val existing = queries.getByTarget(
                    update.targetType.name,
                    update.parentId,
                    update.targetId,
                ).executeAsOneOrNull()
                val existingEntry = existing?.toEntry()
                val nextRead = existingEntry?.read == true || update.read || clampedProgress >= 100
                val storedProgress = if (nextRead) 100 else clampedProgress
                if (
                    existingEntry?.read == nextRead &&
                    existingEntry.progressPercent == storedProgress &&
                    existingEntry.lastPageIndex == update.lastPageIndex &&
                    existingEntry.totalPages == update.totalPages
                ) {
                    return@forEach
                }
                queries.upsert(
                    targetType = update.targetType.name,
                    parentId = update.parentId,
                    targetId = update.targetId,
                    title = update.title.ifBlank { existing?.title.orEmpty() },
                    read = if (nextRead) 1L else 0L,
                    progressPercent = storedProgress.toLong(),
                    lastPageIndex = update.lastPageIndex?.toLong(),
                    totalPages = update.totalPages?.toLong(),
                    updatedAt = currentTimeMillis(),
                )
            }
        }
    }

    override suspend fun clearTarget(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
        targetId: Long,
    ): Unit = withContext(Dispatchers.IO) {
        queries.deleteByTarget(targetType.name, parentId, targetId)
        Unit
    }

    override suspend fun clearParent(
        targetType: LocalChapterStateRepository.TargetType,
        parentId: Long,
    ): Unit = withContext(Dispatchers.IO) {
        queries.deleteByParent(targetType.name, parentId)
        Unit
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
