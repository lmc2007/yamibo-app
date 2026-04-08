package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.*
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.parse.util.ParseUtils

class ForumPageParser : Parser<ForumPage> {

    override suspend fun parse(html: String): ParseResult<ForumPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            // --- Forum info ---
            val headerH2 = doc.selectFirst(".forumdisplay-top h2")
            val forumName = headerH2?.ownText()?.trim() ?: ""
            val newThreadLink = doc.selectFirst("#a_newthread")?.attr("href") ?: ""
            val fid = ParseUtils.extractFid(newThreadLink) ?: ForumId(0)

            val forumP = doc.selectFirst(".forumdisplay-top p")
            var todayCount: Int? = null
            var themeCount: Int? = null
            var rank: Int? = null
            if (forumP != null) {
                val spans = forumP.select("span")
                if (spans.size >= 3) {
                    todayCount = spans[0].text().trim().toIntOrNull()
                    themeCount = spans[1].text().trim().toIntOrNull()
                    rank = spans[2].text().trim().toIntOrNull()
                } else if (spans.size > 0) {
                    todayCount = spans[0].text().trim().toIntOrNull()
                }
            }

            val iconUrl = headerH2?.selectFirst("img")?.attr("src")?.ifEmpty { null }

            val forum =
                ForumSummary(
                    fid = fid,
                    name = forumName,
                    url = newThreadLink,
                    todayCount = todayCount,
                    themeCount = themeCount,
                    rank = rank,
                    iconUrl = iconUrl
                )

            // --- Sub forums ---
            val subForums = mutableListOf<ForumSummary>()
            val subForumItems = doc.select(".forumlist .sub-forum li")
            for (li in subForumItems) {
                val linkEl = li.selectFirst("a[href*=forumdisplay]") ?: continue
                val url = linkEl.attr("href")
                val sfid = ParseUtils.extractFid(url) ?: continue
                val sfName = li.selectFirst(".mtit")?.text()?.trim() ?: ""
                val sfIcon = li.selectFirst(".micon img")?.attr("src")?.ifEmpty { null }
                subForums.add(ForumSummary(fid = sfid, name = sfName, url = url, iconUrl = sfIcon))
            }

            // --- Pinned items ---
            val pinnedItems = mutableListOf<PinnedItem>()
            val pinnedEls = doc.select(".threadlist li.list_top")
            for (el in pinnedEls) {
                val linkEl = el.selectFirst("a") ?: continue
                val url = linkEl.attr("href")
                val iconSpan = el.selectFirst(".micon")
                val iconText = iconSpan?.text()?.trim() ?: ""

                if (iconText == "公告" || iconSpan?.hasClass("gonggao") == true) {
                    val title = linkEl.text().replace(iconText, "").trim()
                    pinnedItems.add(PinnedItem.Announcement(title = title, url = url))
                } else {
                    val tid = ParseUtils.extractTid(url)
                    val title = el.selectFirst("em")?.text()?.trim() ?: ""
                    if (tid != null) {
                        pinnedItems.add(PinnedItem.Thread(tid = tid, title = title, url = url))
                    }
                }
            }

            // --- Thread list ---
            val threads = mutableListOf<ThreadSummary>()
            val threadEls = doc.select(".threadlist li.list")
            for (el in threadEls) {
                val titleLink = el.selectFirst("a[href*=viewthread]") ?: continue
                val url = titleLink.attr("href")
                val tid = ParseUtils.extractTid(url) ?: continue
                val title = el.selectFirst(".threadlist_tit em")?.text()?.trim() ?: ""
                val hasPoll = el.selectFirst(".threadlist_tit .micon")?.text()?.trim() == "投票"

                // Author
                val authorLink = el.selectFirst(".threadlist_top .muser a")
                val authorUid = authorLink?.attr("href")?.let { ParseUtils.extractUid(it) }
                val authorName = authorLink?.text()?.trim() ?: ""
                val avatarUrl =
                    el.selectFirst(".threadlist_top .mimg img")?.attr("src")?.ifEmpty { null }
                val author =
                    if (authorUid != null) {
                        User(uid = authorUid, name = authorName, avatarUrl = avatarUrl)
                    } else null

                // Description
                val description =
                    el.selectFirst(".threadlist_mes")?.text()?.trim()?.ifEmpty { null }

                // Footer stats — single loop
                val footItems = el.select(".threadlist_foot li")
                var viewCount: Int? = null
                var replyCount: Int? = null
                var tag: String? = null
                for (footItem in footItems) {
                    if (footItem.hasClass("mr")) {
                        tag = footItem.text().trim().removePrefix("#").ifEmpty { null }
                    } else if (viewCount == null && footItem.selectFirst(".dm-eye-fill") != null) {
                        viewCount =
                            footItem.ownText().trim().toIntOrNull()
                                ?: footItem.text()
                                    .trim()
                                    .filter { it.isDigit() }
                                    .toIntOrNull()
                    } else if (replyCount == null && footItem.selectFirst(".dm-chat-s-fill") != null
                    ) {
                        replyCount =
                            footItem.ownText().trim().toIntOrNull()
                                ?: footItem.text()
                                    .trim()
                                    .filter { it.isDigit() }
                                    .toIntOrNull()
                    }
                }

                // Last update time
                val lastUpdateText =
                    el.selectFirst(".muser .mtime")?.text()?.trim()?.ifEmpty { null }

                threads.add(
                    ThreadSummary(
                        tid = tid,
                        title = title,
                        hasPoll = hasPoll,
                        url = url,
                        author = author,
                        description = description,
                        replyCount = replyCount,
                        viewCount = viewCount,
                        tag = tag,
                        lastUpdateText = lastUpdateText
                    )
                )
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(
                ForumPage(
                    forum = forum,
                    pinnedItems = pinnedItems,
                    subForums = subForums,
                    threads = threads,
                    pageNav = pageNav
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse forum page", e)
        }
    }
}
