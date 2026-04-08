package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.AttachmentType
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.parse.util.ParseUtils

class TagPagParser : Parser<TagPage> {

    override suspend fun parse(html: String): ParseResult<TagPage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            // Thread list
            // Rows sit inside: div.bm.tl > div.bm_c > table > tr
            val threads = mutableListOf<ThreadSummary>()
            val rows = doc.select("div.tl div.bm_c table tr")
            for (row in rows) {
                val titleLink = row.selectFirst("th > a[href]") ?: continue
                val url = titleLink.attr("href")
                val tid = ParseUtils.extractTid(url) ?: continue
                val title = titleLink.text().trim()

                // Attachment type (icon <i> inside <th>)
                val attachIcon = row.selectFirst("th > i")
                val attachmentType = when (attachIcon?.attr("title")) {
                    "图片附件" -> AttachmentType.Image
                    "附件" -> AttachmentType.Other
                    else -> null
                }

                // Forum info (first td.by contains a link to the forum)
                val byColumns = row.select("td.by")
                val forumLink = byColumns.getOrNull(0)?.selectFirst("a[href]")
                val fid = forumLink?.attr("href")?.let(ParseUtils::extractFid)

                // Author info (second td.by contains cite > a for author)
                val authorColumn = byColumns.getOrNull(1)
                val authorLink = authorColumn?.selectFirst("cite > a")
                val authorUid = authorLink?.attr("href")?.let(ParseUtils::extractUid)
                val authorName = authorLink?.text()?.trim() ?: ""
                val author = if (authorUid != null) {
                    User(uid = authorUid, name = authorName)
                } else null

                // Author date (second td.by > em > span)
                val authorDate = authorColumn?.selectFirst("em > span")?.text()?.trim()?.ifEmpty { null }

                // Reply count & view count (td.num)
                val numColumn = row.selectFirst("td.num")
                val replyCount = numColumn?.selectFirst("a")?.text()?.trim()?.toIntOrNull()
                val viewCount = numColumn?.selectFirst("em")?.text()?.trim()?.toIntOrNull()

                // Last update (second td.by > em > span)
                val lastColumn = byColumns.getOrNull(1)
                val lastUpdateText = lastColumn?.selectFirst("em > span")?.text()?.trim()?.ifEmpty { null }

                threads.add(
                    ThreadSummary(
                        tid = tid,
                        title = title,
                        fid = fid,
                        attachmentType = attachmentType,
                        hasPoll = false,
                        url = url,
                        author = author,
                        replyCount = replyCount,
                        viewCount = viewCount,
                        lastUpdateText = lastUpdateText ?: authorDate,
                    )
                )
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(
                TagPage(
                    threadSummaries = threads,
                    pageNav = pageNav,
                )
            )
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse tag page", e)
        }
    }
}