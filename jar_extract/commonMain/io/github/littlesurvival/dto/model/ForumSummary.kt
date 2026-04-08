package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.ForumId

/**
 * A forum entry shown on the home page.
 *
 * This represents a clickable forum block that leads to a forumdisplay page.
 */
data class ForumSummary(
    /**
     * Forum id (fid) extracted from the forumdisplay URL.
     *
     * Example: forum.php?mod=forumdisplay&fid=49 -> fid = 49
     */
    val fid: ForumId,

    /**
     * Display name of the forum.
     *
     * Example: 文學區
     */
    val name: String,

    /**
     * URL to enter this forum.
     *
     * Usually a relative link to forumdisplay with mobile=2.
     */
    val url: String,

    /**
     * Short description text shown under the forum name on the home page.
     *
     * May be null if the forum does not provide a description.
     */
    val description: String? = null,

    /**
     * Number of threads created today, if available.
     *
     * Parsed from text such as "今日 20". Null if the value is not present on the page.
     */
    val todayCount: Int? = null,

    /** Number of total themes, if available. */
    val themeCount: Int? = null,

    /** Rank of the forum, if available. */
    val rank: Int? = null,

    /**
     * Icon image URL representing the forum.
     *
     * May be null if no icon is provided.
     */
    val iconUrl: String? = null
)
