package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.value.ThreadId

/**
 * Forum page model.
 *
 * Represents a forum board page (forumdisplay), including:
 * - pinned items (announcements / pinned threads)
 * - sub forums
 * - normal thread list
 *
 * This model is a read-only snapshot parsed from the forum page.
 */
data class ForumPage(
    /** Forum basic information. */
    val forum: ForumSummary,

    /**
     * Pinned items shown at the top of the thread list.
     *
     * Includes announcements and pinned threads. Order matches the page display order.
     */
    val pinnedItems: List<PinnedItem>,

    /**
     * Sub forums under this forum.
     *
     * Example: 轻小说/译文区, TXT小说区
     */
    val subForums: List<ForumSummary>,

    /**
     * Normal thread list (non-pinned).
     *
     * Ordered as displayed on the page.
     */
    val threads: List<ThreadSummary>,

    /**
     * Page navigation URLs and page info, if present.
     *
     * Useful when the forum has multiple pages.
     */
    val pageNav: PageNav? = null
)

/** Item pinned at the top of a forum page. */
sealed interface PinnedItem {

    /**
     * Forum announcement.
     *
     * Example: 欢迎光临。
     */
    data class Announcement(
        /** Announcement title. */
        val title: String,

        /** Link to the announcement page. */
        val url: String
    ) : PinnedItem

    /** Pinned thread. */
    data class Thread(
        /** Thread id (tid). */
        val tid: ThreadId,

        /** Thread title. */
        val title: String,

        /** Link to the thread page. */
        val url: String
    ) : PinnedItem
}
