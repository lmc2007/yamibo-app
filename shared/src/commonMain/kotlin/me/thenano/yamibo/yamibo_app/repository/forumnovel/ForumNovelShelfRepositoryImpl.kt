package me.thenano.yamibo.yamibo_app.repository.forumnovel

import me.thenano.yamibo.yamibo_app.Database

class ForumNovelShelfRepositoryImpl(
    db: Database,
) : ForumNovelShelfRepository {
    private val queries = db.forumNovelShelfQueries

    override suspend fun getAll(): List<ForumNovelShelfEntry> =
        queries.getAll().executeAsList().map { it.toEntry() }

    override suspend fun getById(id: Long): ForumNovelShelfEntry? =
        queries.getById(id).executeAsOneOrNull()?.toEntry()

    override suspend fun getByTid(tid: Long, authorId: Long?): ForumNovelShelfEntry? =
        queries.getByTidAndAuthor(tid, authorId).executeAsOneOrNull()?.toEntry()

    override suspend fun insert(entry: ForumNovelShelfEntry): Long {
        queries.insert(
            source = entry.source.name.lowercase(),
            tid = entry.tid,
            authorId = entry.authorId,
            title = entry.title,
            contentDir = entry.contentDir,
            createdAt = entry.createdAt,
        )
        return queries.getAll().executeAsList().maxOfOrNull { it.id } ?: 0L
    }

    override suspend fun delete(id: Long) {
        queries.deleteById(id)
    }
}

private fun me.thenano.yamibo.yamibo_app.ForumNovelShelf.toEntry(): ForumNovelShelfEntry =
    ForumNovelShelfEntry(
        id = id,
        source = ForumNovelShelfSource.fromStorage(source),
        tid = tid,
        authorId = authorId,
        title = title,
        contentDir = contentDir,
        createdAt = createdAt,
    )
