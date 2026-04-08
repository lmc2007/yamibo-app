package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.page.TagPage

/** Thread item shown in a forum thread list. */
data class ThreadSummary(
    /** Thread id (tid). */
    val tid: ThreadId,

    /** Thread title. */
    val title: String,

    /**
     * Forum Id (fid).
     *  @see TagPage only
     */
    val fid: ForumId? = null,

    /**
     * Attachment type.
     * @see TagPage only
     *
     * 附件類型 (Image : "图片附件", Other : "附件")
     */
    val attachmentType: AttachmentType? = null,

    /**
     * whether the thread is a poll thread.
     * if true, it would have icon "投票" next to title.
     */
    val hasPoll: Boolean,

    /** Link to the thread page. */
    val url: String,

    /** Thread Author Info. */
    val author: User? = null,

    /** Thread preview text shown in the list. */
    val description: String? = null,

    /** Number of replies. */
    val replyCount: Int? = null,

    /** Number of views. */
    val viewCount: Int? = null,

    /** Thread tag text (e.g. 公告, 原创). */
    val tag: String? = null,

    /**
     * Last update time text shown in the list.
     *
     * Usually comes from the forum list timestamp (e.g. "2025-12-2 04:41").
     * For Tag page it's only have y-m-d, the info's position is below author name (e.g. 2025-1-11).
     *
     * Kept as raw text to avoid parsing and timezone issues.
     */
    val lastUpdateText: String? = null,
)

enum class AttachmentType {
    Image,
    Other,
}