package me.thenano.yamibo.yamibo_app.repository.forumnovel

enum class ForumNovelShelfSource {
    Downloaded,
    Imported;

    companion object {
        fun fromStorage(value: String?): ForumNovelShelfSource =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: Imported
    }
}

data class ForumNovelShelfEntry(
    val id: Long = 0,
    val source: ForumNovelShelfSource,
    val tid: Long,
    val authorId: Long?,
    val title: String,
    val contentDir: String?,
    val createdAt: Long,
)

interface ForumNovelShelfRepository {
    suspend fun getAll(): List<ForumNovelShelfEntry>
    suspend fun getById(id: Long): ForumNovelShelfEntry?
    suspend fun getByTid(tid: Long, authorId: Long?): ForumNovelShelfEntry?
    suspend fun insert(entry: ForumNovelShelfEntry): Long
    suspend fun delete(id: Long)
}
