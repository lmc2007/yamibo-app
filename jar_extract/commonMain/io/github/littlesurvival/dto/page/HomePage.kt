package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.ForumSummary

/**
 * Forum home page model.
 *
 * Represents the data shown on the forum index page, including top-level forum categories (e.g. 庙堂
 * / 江湖) and their forum entries.
 *
 * This model is read-only and intended to be a snapshot of the parsed page.
 */
data class HomePage(
    /**
     * Ordered list of forum categories shown on the home page.
     *
     * Each category is a logical grouping label and does not necessarily correspond to a
     * navigable forum page.
     */
    val categories: List<ForumCategory>,

    /**
     * Optional yearly summary / event banner.
     *
     * Shown during special periods like Lunar New Year.
     */
    val yearlySummary: YearlySummary? = null
)

/** A yearly summary or event banner info. */
data class YearlySummary(
    /** Display name (e.g. "2025年度总结"). */
    val name: String,

    /** URL to the banner image. */
    val imageLink: String,

    /** URL to the event page or thread. */
    val activityLink: String
)

/**
 * A forum category on the home page.
 *
 * A category is a visual grouping used by the forum (e.g. 庙堂, 江湖), containing multiple forums that
 * users can enter.
 */
data class ForumCategory(
    /**
     * Display title of the category.
     *
     * Example values:
     * - 庙堂
     * - 江湖
     */
    val title: String,

    /**
     * Forums belonging to this category.
     *
     * The order of this list matches the order displayed on the page.
     */
    val forums: List<ForumSummary>
)
