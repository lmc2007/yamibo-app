package io.github.littlesurvival.dto.model

/**
 * Pagination navigation URLs for multipage listings.
 *
 * Used by both forum pages and thread pages.
 */
data class PageNav(
    /** URL to the next page, if available. */
    val nextUrl: String? = null,

    /** URL to the previous page, if available. */
    val prevUrl: String? = null,

    /**
     * Current page number (1-based).
     *
     * Parsed from the highlighted `<strong>` element or the input value in the pagination
     * widget.
     */
    val currentPage: Int? = null,

    /**
     * Total number of pages.
     *
     * Parsed from the `<span title="共 N 页">` element in the pagination widget.
     */
    val totalPages: Int? = null
)