package me.thenano.yamibo.yamibo_app.repository

interface DetailNoteRepository {
    enum class TargetType {
        NovelThread,
        TagManga,
        RssSearch,
    }

    data class DetailNote(
        val targetType: TargetType,
        val targetId: Long,
        val authorId: Long,
        val content: String,
        val createdAt: Long,
        val updatedAt: Long,
    )

    suspend fun getNote(targetType: TargetType, targetId: Long, authorId: Long = 0L): DetailNote?
    suspend fun saveNote(targetType: TargetType, targetId: Long, authorId: Long = 0L, content: String)
    suspend fun deleteNote(targetType: TargetType, targetId: Long, authorId: Long = 0L)
}
