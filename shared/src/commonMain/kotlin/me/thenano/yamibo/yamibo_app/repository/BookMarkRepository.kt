package me.thenano.yamibo.yamibo_app.repository

interface BookMarkRepository {
    enum class TargetType {
        ThreadPost,
        TagMangaThread,
        RssSearchThread;

        companion object {
            fun fromStorage(value: String): TargetType {
                return when (value) {
                    "NovelPost" -> ThreadPost
                    else -> entries.firstOrNull { it.name == value } ?: ThreadPost
                }
            }
        }
    }

    data class Entry(
        val targetType: TargetType,
        val parentId: Long,
        val targetId: Long,
        val title: String,
        val bookmarked: Boolean,
        val read: Boolean,
        val createdAt: Long,
        val updatedAt: Long,
    )

    suspend fun getEntry(targetType: TargetType, parentId: Long, targetId: Long): Entry?
    suspend fun getEntriesByParent(targetType: TargetType, parentId: Long): List<Entry>
    suspend fun getAllEntries(): List<Entry>
    suspend fun setBookmarked(targetType: TargetType, parentId: Long, targetId: Long, title: String, bookmarked: Boolean)
    suspend fun setRead(targetType: TargetType, parentId: Long, targetId: Long, title: String, read: Boolean)
    suspend fun clearTarget(targetType: TargetType, parentId: Long, targetId: Long)
    suspend fun clearParent(targetType: TargetType, parentId: Long)
}
