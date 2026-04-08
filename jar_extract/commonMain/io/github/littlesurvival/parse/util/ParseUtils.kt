package io.github.littlesurvival.parse.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId

/** Shared parsing utilities used by multiple parsers. */
object ParseUtils {

    // Pre-compiled regex patterns for URL extraction
    private val FID_RE = Regex("[?&]fid=(\\d+)")
    private val FID_PATH_RE = Regex("forum-(\\d+)-")
    private val TID_QUERY_RE = Regex("[?&]tid=(\\d+)")
    private val TID_PATH_RE = Regex("thread-(\\d+)-")
    private val UID_QUERY_RE = Regex("[?&]uid=(\\d+)")
    private val UID_PATH_RE = Regex("uid-(\\d+)")
    private val UID_SCRIPT_RE = Regex("discuz_uid\\s*=\\s*'(\\d+)'")
    private val PAGE_NUMBER_RE = Regex("(\\d+)")

    /** Extract forum id (fid) from a URL query (query param or SEO path). */
    fun extractFid(url: String): ForumId? {
        val queryMatch = FID_RE.find(url)
        if (queryMatch != null) return queryMatch.groupValues[1].toIntOrNull()?.let { ForumId(it) }
        return FID_PATH_RE.find(url)?.groupValues?.get(1)?.toIntOrNull()?.let { ForumId(it) }
    }

    /** Extract thread id (tid) from a URL (query param or SEO path). */
    fun extractTid(url: String): ThreadId? {
        val queryMatch = TID_QUERY_RE.find(url)
        if (queryMatch != null) return queryMatch.groupValues[1].toIntOrNull()?.let { ThreadId(it) }
        return TID_PATH_RE.find(url)?.groupValues?.get(1)?.toIntOrNull()?.let { ThreadId(it) }
    }

    /** Extract user id (uid) from a URL (query param or SEO path). */
    internal fun extractUid(url: String): UserId? {
        val queryMatch = UID_QUERY_RE.find(url)
        if (queryMatch != null) return queryMatch.groupValues[1].toIntOrNull()?.let { UserId(it) }
        return UID_PATH_RE.find(url)?.groupValues?.get(1)?.toIntOrNull()?.let { UserId(it) }
    }

    /** Extract user id (uid) from a script string (e.g., discuz_uid = '123'). */
    internal fun extractUidFromScript(script: String): UserId? {
        return UID_SCRIPT_RE.find(script)?.groupValues?.get(1)?.toIntOrNull()?.let { UserId(it) }
    }

    /** Parse pagination from `.pg` widget. */
    internal fun parsePageNav(doc: Document): PageNav? {
        val pgDiv = doc.selectFirst(".pg") ?: return null

        val nextUrl = pgDiv.selectFirst("a.nxt")?.attr("href")?.ifEmpty { null }
        val prevUrl = pgDiv.selectFirst("a.prev")?.attr("href")?.ifEmpty { null }

        // Current page: <strong>1</strong> inside .pg
        val currentPage =
            pgDiv.selectFirst("strong")?.text()?.trim()?.toIntOrNull()
                ?: pgDiv.selectFirst("input.px")?.attr("value")?.toIntOrNull()

        // Total pages: <span title="共 N 页"> / N 页</span> inside .pg label
        val totalPages =
            pgDiv.selectFirst("label span")?.let { span ->
                val titleAttr = span.attr("title") // e.g. "共 62 页"
                val text = titleAttr.ifEmpty { span.text() }
                PAGE_NUMBER_RE.findAll(text).lastOrNull()?.value?.toIntOrNull()
            }

        return if (nextUrl != null || prevUrl != null || currentPage != null || totalPages != null
        ) {
            PageNav(
                nextUrl = nextUrl,
                prevUrl = prevUrl,
                currentPage = currentPage,
                totalPages = totalPages
            )
        } else null
    }

    /**
     * Detect whether the HTML is a "not logged in" / session-expired page.
     *
     * Works for both mobile and desktop variants:
     * - Mobile: login form page with `pg_logging` body class
     * - Desktop: message page with text "您尚未登录" or "没有权限访问"
     * - Either: `discuz_uid = '0'` combined with login-related indicators
     */
    internal fun isNotLoggedIn(doc: Document): Boolean {
        // Mobile: body has class pg_logging (login page redirect)
        val body = doc.selectFirst("body")
        if (body != null && body.hasClass("pg_logging")) return true

        // Mobile: login form with username placeholder
        if (doc.selectFirst("input[placeholder=请输入用户名/Email/UID]") != null) return true

        // Desktop: message text indicating not logged in
        val messageText = doc.selectFirst("#messagetext")?.text() ?: ""
        if (messageText.contains("尚未登录")) return true

        // Desktop: login form present in #messagelogin area
        if (doc.selectFirst("#messagelogin") != null && doc.selectFirst("#main_message") != null)
            return true

        return false
    }

    /**
     * Detect whether the HTML is a "no permission" page.
     */
    internal fun isNoPermission(doc: Document): Boolean {
        val jumpC = doc.selectFirst(".jump_c")?.text() ?: ""
        if (jumpC.contains("阅读权限高于") || jumpC.contains("閱讀權限高於") ||
            jumpC.contains("没有权限访问") || jumpC.contains("沒有權限訪問")) return true

        val messageText = doc.selectFirst("#messagetext")?.text() ?: ""
        if (messageText.contains("阅读权限高于") || messageText.contains("閱讀權限高於") ||
                messageText.contains("没有权限访问") || messageText.contains("沒有權限訪問")) return true

        return false
    }

    internal fun parsePromptMessage(doc: Document): String {
        return doc.select(".jump_c p")
            .firstOrNull()
            ?.text()
            ?.trim()
            ?.ifEmpty { null }
            ?: "未知權限錯誤"
    }

    /**
     * Detect whether the HTML is a maintenance page.
     *
     * In Yamibo, this page typically has:
     * - Title containing "每日维护" (Daily Maintenance)
     * - A wrapper div with an image alt text "每日维护"
     */
    internal fun isMaintenance(doc: Document): Boolean =
        doc.title().contains("每日维护") ||
            doc.selectFirst("img[alt*=每日维护]") != null ||
            doc.selectFirst("img[src*='backup01.jpg']") != null

    internal fun isMaintenance(html: String?): Boolean {
        return try {
            if (html.isNullOrEmpty()) {
                return false
            }
            isMaintenance(Ksoup.parse(html))
        } catch (_: Exception) {
            false
        }
    }

    /** Detect whether the HTTP status code indicates maintenance. Common for Yamibo is 503. */
    internal fun isMaintenance(statusCode: Int): Boolean = statusCode == 503
}
