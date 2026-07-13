package me.thenano.yamibo.yamibo_app.repository.detailnote

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.repository.DetailNoteRepository
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

class DetailNoteRepositoryImpl(
    db: Database,
) : DetailNoteRepository {
    private val queries = db.detailNoteQueries

    override suspend fun getNote(
        targetType: DetailNoteRepository.TargetType,
        targetId: Long,
        authorId: Long,
    ): DetailNoteRepository.DetailNote? {
        return queries.getByTarget(targetType.name, targetId, authorId)
            .executeAsOneOrNull()
            ?.let {
                DetailNoteRepository.DetailNote(
                    targetType = DetailNoteRepository.TargetType.valueOf(it.targetType),
                    targetId = it.targetId,
                    authorId = it.authorId,
                    content = it.content,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            }
    }

    override suspend fun saveNote(
        targetType: DetailNoteRepository.TargetType,
        targetId: Long,
        authorId: Long,
        content: String,
    ) {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) {
            deleteNote(targetType, targetId, authorId)
            return
        }
        val now = currentTimeMillis()
        val createdAt = getNote(targetType, targetId, authorId)?.createdAt ?: now
        queries.upsert(
            targetType = targetType.name,
            targetId = targetId,
            authorId = authorId,
            content = normalizedContent,
            createdAt = createdAt,
            updatedAt = now,
        )
    }

    override suspend fun deleteNote(
        targetType: DetailNoteRepository.TargetType,
        targetId: Long,
        authorId: Long,
    ) {
        queries.deleteByTarget(targetType.name, targetId, authorId)
    }
}
