package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.dto.value.ThreadId

/**
 * Repository for tracking reading history and precise scroll positions.
 *
 * Supports both thread reader (novel/forum) and manga reader positions.
 */
interface ReadHistoryRepository {

    /** Precise scroll position for thread reader */
    data class ThreadReadPosition(
        val tid: Int,
        val page: Int,
        val postId: Int,
        val blockIndex: Int,
        val scrollOffset: Int
    )

    /** Get saved thread reading position */
    fun getThreadPosition(tid: ThreadId): ThreadReadPosition?

    /** Save thread reading position */
    fun saveThreadPosition(position: ThreadReadPosition) // TODO: implement persistence

    /** Get manga reading position */
    fun getMangaPosition(tid: ThreadId): Any? // TODO: manga reader

    /** Save manga reading position */
    fun saveMangaPosition(tid: ThreadId, position: Any) // TODO: manga reader
}
