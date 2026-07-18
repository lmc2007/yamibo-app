package me.thenano.yamibo.yamibo_app.repository

enum class LocalNovelFileType {
    TXT,
    EPUB;

    companion object {
        fun fromString(value: String): LocalNovelFileType =
            try {
                valueOf(value.uppercase())
            } catch (_: IllegalArgumentException) {
                TXT
            }
    }
}

data class LocalNovelInfo(
    val id: Long = 0,
    val fileType: LocalNovelFileType,
    val title: String,
    val author: String = "",
    val fileUri: String,
    val coverPath: String? = null,
    val totalChars: Long = 0,
    val encoding: String = "UTF-8",
    val totalChapters: Int = 0,
    val epubExtractDir: String? = null,
    val createdAt: Long,
    val lastReadAt: Long = 0,
)

data class LocalNovelChapterInfo(
    val id: Long = 0,
    val novelId: Long,
    val title: String,
    val chapterIndex: Int,
    val startOffset: Long = 0,
    val endOffset: Long = 0,
    val internalPath: String = "",
)

data class LocalNovelProgressInfo(
    val novelId: Long,
    val chapterId: Long = 0,
    val charOffset: Long = 0,
)

interface LocalNovelRepository {
    suspend fun getAllNovels(): List<LocalNovelInfo>
    suspend fun getNovelById(id: Long): LocalNovelInfo?
    suspend fun insertNovel(novel: LocalNovelInfo): Long
    suspend fun updateNovelMeta(
        novelId: Long,
        title: String,
        author: String,
        totalChars: Long,
        totalChapters: Int,
    )
    suspend fun updateCoverPath(novelId: Long, coverPath: String)
    suspend fun updateEpubExtractDir(novelId: Long, dir: String)
    suspend fun updateLastReadAt(novelId: Long, time: Long)
    suspend fun deleteNovel(novelId: Long)

    suspend fun getChaptersByNovelId(novelId: Long): List<LocalNovelChapterInfo>
    suspend fun getChapterById(chapterId: Long): LocalNovelChapterInfo?
    suspend fun insertChapter(chapter: LocalNovelChapterInfo): Long
    suspend fun insertChapters(chapters: List<LocalNovelChapterInfo>)
    suspend fun deleteChaptersByNovelId(novelId: Long)

    suspend fun getProgress(novelId: Long): LocalNovelProgressInfo?
    suspend fun saveProgress(progress: LocalNovelProgressInfo)
    suspend fun deleteProgress(novelId: Long)
}
