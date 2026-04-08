package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId

/**
 * Thread page model.
 *
 * Represents a single thread page (viewthread), including:
 * - thread header info
 * - a list of posts shown on the current page
 *
 * This model is a read-only snapshot parsed from the thread page.
 */
data class ThreadPage(
    /** Thread header information. */
    val thread: ThreadInfo,

    /**
     * Posts shown on the current page.
     *
     * Order matches the page display order (normally ascending floor).
     */
    val posts: List<Post>,

    /**
     * Page navigation URLs, if present.
     *
     * Useful when the thread has multiple pages.
     */
    val pageNav: PageNav? = null
)

/** Basic thread information extracted from the thread page. */
data class ThreadInfo(
    /** Thread id (tid). */
    val tid: ThreadId,

    /** Thread title (without forum name). */
    val title: String,

    /**
     * The forum this thread belongs to.
     *
     * Usually parsed from the breadcrumb/title link back to forumdisplay.
     */
    val forum: ForumSummary,

    /**
     * Optional thread category label shown in the title area.
     *
     * Example: "[其它]" (as displayed on the page).
     */
    val categoryTag: String? = null,

    /**
     * Total number of replies in the thread.
     *
     * Nullable because this is only present on page 1 of the thread.
     */
    val totalReplies: Int? = null,

    /**
     * Total number of views for the thread.
     *
     * Nullable because this may not be present on all pages.
     */
    val totalViews: Int? = null,

    /**
     * URL for posting a reply to this thread.
     *
     * Extracted from the "发表回复" link at the bottom of the page.
     */
    val replyUrl: String? = null
)

/** A single post (a floor) within a thread page. */
data class Post(
    /** Post id (pid). */
    val pid: PostId,

    /**
     * Floor number shown on the page (1-based).
     *
     * Example: 1 for "1#".
     */
    val floor: Int,

    /**
     * Post Title.
     *
     * By parsing tons of examples, found a rule for getting the post title.
     *
     * This feature is basically only for novel forum.
     *
     * Example: 不共戴天的银发千金
     */
    val title: String,

    /**
     * Post author.
     *
     * Should include uid/name/avatarUrl when available.
     */
    val author: User,

    /**
     * Post time text as displayed on the page.
     *
     * Example: "2026-2-6 01:54"
     */
    val timeText: String,

    /**
     * Optional edit info text shown inside the post body.
     *
     * Example: "本帖最后由 XXX 于 ... 编辑"
     */
    val editedText: String? = null,

    /**
     * Raw HTML of the post content area.
     *
     * Keep it as HTML to preserve formatting, line breaks, quotes, etc.
     */
    val contentHtml: String,

    /**
     * Images attached/embedded in the post.
     * Especial for manga view mode.
     * */
    val images: List<PostImage> = emptyList(),

    /**
     * Tags, usually appear in manga thread, made for catalog.
     */
    val tags: Tags,

    /**
     * Poll attached to the post, it's unique in the whole thread and forced to be at first floor.
     */
    val poll: Poll?,

    /** File attachments in the post. */
    val attachments: List<Attachment> = emptyList(),

    /** Comments (点评) attached to the post. */
    val comments: List<PostComment> = emptyList(),

    /** Rates (评分) attached to the post. */
    val rateBlock: RateBlock = RateBlock()
)

/** An image referenced in a post. */
data class PostImage(
    /**
     * Full image URL (href or src).
     *
     * Usually a relative path like "data/attachment/forum/....jpg".
     */
    val url: String,

    /**
     * Optional alt text shown by the page.
     *
     * Example: "1.jpg"
     */
    val alt: String? = null
)

/** A comment (点评) left on a post. */
data class PostComment(
    /** The user who left the comment. */
    val user: User,

    /** Time text of the comment. */
    val timeText: String,

    /** The comment message text. */
    val message: String
)

/** A rate entry (评分) on a post. */
data class RateBlock(
    /** Total number of people who participated in rating this post. */
    val rateParticipatePeople: Int = 0,

    /** Total score given by all rate participants. */
    val rateTotalScore: Int = 0,

    /** The list of all rates */
    val rates: List<PostRate> = emptyList(),
)

data class PostRate(
    /** The user who rated. */
    val userName: String,

    /** The score given (e.g. 10). */
    val score: Int,

    /** Optional reason text for the rating. */
    val reason: String? = null
)

/** An attachment on the post. */
data class Attachment(
    /** The name of attachment. */
    val name: String,
    /** The download link of attachment. */
    val url: String,
    /** The time it was uploaded (e.g. 2026-1-10 21:08) */
    val timeUpload: String,
    /** The size of the attachment (e.g. 17.93 KB) */
    val fileSize: String,
    /** The times it has been downloaded (e.g. 122) */
    val downloadTimes: Int,
)

data class Poll(
    val status: PollStatus,
    val type: PollType,
    /** How long did the poll end (e.g. 距结束还有: 101 天 17 小时 11 分钟) */
    val endTime: String,
    /**
     * The information about the poll, contains poll type, maximum choices, turnout, etc
     *
     * (e.g. 多选投票: ( 最多可选 3 项 ), 共有 264 人参与投票)
     * */
    val pollInfo: String,
    val option: List<PollOption>
)

data class PollOption(
    val option: PollOptionId,
    val optionName: String,
    /** The percentage of the option was voted, format with ".2f" . (e.g. 3.02%) */
    val percentage: Float?,
    val totalVoted: Int?,
)

enum class PollStatus {
    NotVoted,
    Voted,
}

enum class PollType {
    SingleChoice,
    MultipleChoice,
}
