package me.thenano.yamibo.yamibo_app.repository

interface LocalChapterStateRepository {
    enum class TargetType {
        ThreadPost,
        TagMangaThread;

        companion object {
            fun fromStorage(value: String): TargetType {
                return entries.firstOrNull { it.name == value } ?: ThreadPost
            }
        }
    }

    data class Entry(
        val targetType: TargetType,
        val parentId: Long,
        val targetId: Long,
        val title: String,
        val read: Boolean,
        val progressPercent: Int,
        val lastPageIndex: Int?,
        val totalPages: Int?,
        val updatedAt: Long,
    ) {
        val hasProgress: Boolean
            get() = !read && progressPercent > 0
    }

    suspend fun getEntry(targetType: TargetType, parentId: Long, targetId: Long): Entry?

    suspend fun getEntriesByParent(targetType: TargetType, parentId: Long): List<Entry>

    suspend fun getAllEntries(): List<Entry>

    suspend fun upsertProgress(
        targetType: TargetType,
        parentId: Long,
        targetId: Long,
        title: String,
        progressPercent: Int,
        read: Boolean = progressPercent >= 100,
        lastPageIndex: Int? = null,
        totalPages: Int? = null,
    )

    suspend fun setRead(targetType: TargetType, parentId: Long, targetId: Long, title: String, read: Boolean)

    suspend fun clearTarget(targetType: TargetType, parentId: Long, targetId: Long)

    suspend fun clearParent(targetType: TargetType, parentId: Long)
}
