package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.parse.util.ParseUtils

class SearchPageParser : Parser<SearchPage> {

    override suspend fun parse(html: String): ParseResult<SearchPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            // Query
            val query = doc.selectFirst(".threadlist_box h2 em .emfont")?.text() ?: ""

            // Total result count
            val totalCount =
                doc.selectFirst(".threadlist_box h2 em")?.text()?.let {
                    TOTAL_COUNT_RE.find(it)?.groupValues?.get(1)?.toIntOrNull()
                }
                    ?: 0

            // Search ID — try pagination links first,
            // then fall back to raw HTML scan.
            val searchIdValue =
                doc.select(".pg a").firstNotNullOfOrNull {
                    SEARCH_ID_RE.find(it.attr("href"))?.groupValues?.get(1)?.toIntOrNull()
                }
                    ?: SEARCH_ID_RE.find(html)?.groupValues?.get(1)?.toIntOrNull()
            val searchId = searchIdValue?.let { SearchId(it) }

            // Thread list
            val threads = mutableListOf<ThreadSummary>()
            val items = doc.select(".threadlist li.list")
            for (item in items) {
                // Thread URL and tid
                val threadLink = item.selectFirst("a[href*=viewthread]") ?: continue
                val threadUrl = threadLink.attr("href")
                val tid = ParseUtils.extractTid(threadUrl) ?: continue

                // Title
                val title = item.selectFirst(".threadlist_tit em")?.text()?.trim() ?: ""

                // Special tag (e.g. "投票")
                val tag = item.selectFirst(".threadlist_tit .micon")?.text()?.trim()
                val hasPoll = tag == "投票"

                // Author info
                val authorLink = item.selectFirst(".threadlist_top .muser h3 a.mmc")
                val authorName = authorLink?.text()?.trim() ?: ""
                val authorUid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) }
                val avatarUrl = item.selectFirst(".threadlist_top .mimg img")?.attr("src")

                val author =
                    if (authorUid != null) {
                        User(uid = authorUid, name = authorName, avatarUrl = avatarUrl)
                    } else null

                // Time
                val timeText = item.selectFirst(".muser .mtime")?.text()?.trim()

                // Description / preview
                val description =
                    item.selectFirst(".threadlist_mes")?.text()?.trim()?.ifEmpty { null }

                // Footer stats — query once, not twice
                val footItems = item.select(".threadlist_foot li")

                // Forum tag (e.g. "動漫區")
                val forumTag =
                    footItems
                        .firstOrNull { it.hasClass("mr") }
                        ?.selectFirst("a")
                        ?.text()
                        ?.trim()
                        ?.removePrefix("#")

                // View count
                var viewCount: Int? = null
                var replyCount: Int? = null
                for (li in footItems) {
                    if (viewCount == null && li.selectFirst("i.dm-eye-fill") != null) {
                        viewCount = li.text().trim().replace(NON_DIGIT_RE, "").toIntOrNull()
                    } else if (replyCount == null && li.selectFirst("i.dm-chat-s-fill") != null) {
                        replyCount = li.text().trim().replace(NON_DIGIT_RE, "").toIntOrNull()
                    }
                    if (viewCount != null && replyCount != null) break
                }

                threads.add(
                    ThreadSummary(
                        tid = tid,
                        title = title,
                        hasPoll = hasPoll,
                        url = threadUrl,
                        author = author,
                        description = description,
                        replyCount = replyCount,
                        viewCount = viewCount,
                        tag = forumTag ?: tag,
                        lastUpdateText = timeText
                    )
                )
            }

            // Pagination
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(
                SearchPage(
                    query = query,
                    searchId = searchId,
                    threads = threads,
                    totalCount = totalCount,
                    pageNav = pageNav
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse search page", e)
        }
    }

    companion object {
        private val TOTAL_COUNT_RE = Regex("(\\d+)\\s*个")
        private val SEARCH_ID_RE = Regex("searchid=(\\d+)")
        private val NON_DIGIT_RE = Regex("[^0-9]")
    }
}
