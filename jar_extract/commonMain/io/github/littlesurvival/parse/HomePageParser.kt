package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.page.ForumCategory
import io.github.littlesurvival.dto.page.HomePage
import io.github.littlesurvival.dto.page.YearlySummary
import io.github.littlesurvival.parse.util.ParseUtils

class HomePageParser : Parser<HomePage> {

    override suspend fun parse(html: String): ParseResult<HomePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))
            val categories = mutableListOf<ForumCategory>()

            val categoryHeaders = doc.select(".forumlist .subforumshow")
            for (header in categoryHeaders) {
                val title = header.select("h2 a").text().trim()
                val subForumId = header.attr("href").removePrefix("#")
                val subForumDiv = doc.select("#$subForumId")
                if (subForumDiv.isEmpty()) continue

                val forums = mutableListOf<ForumSummary>()
                val items = subForumDiv.select("li")
                for (li in items) {
                    val linkEl = li.select("a.murl").first() ?: continue
                    val url = linkEl.attr("href")
                    val fid = ParseUtils.extractFid(url) ?: continue
                    val nameEl = li.select(".mtit")
                    val numSpan = nameEl.select(".mnum")
                    val todayCount = numSpan.text().trim().removePrefix("今日").trim().toIntOrNull()
                    numSpan.remove()
                    val name = nameEl.text().trim()
                    val description = li.select(".mtxt").text().trim().ifEmpty { null }
                    val iconUrl = li.select(".micon img").attr("src").ifEmpty { null }

                    forums.add(
                        ForumSummary(
                            fid = fid,
                            name = name,
                            url = url,
                            description = description,
                            todayCount = todayCount,
                            iconUrl = iconUrl
                        )
                    )
                }
                if (title.isNotEmpty()) {
                    categories.add(ForumCategory(title = title, forums = forums))
                }
            }

            // --- Yearly Summary ---
            val summaryImg = doc.select("img[alt*=年度总结]").firstOrNull()
            val yearlySummary =
                summaryImg?.let { img ->
                    val link = img.parent()?.attr("href") ?: ""
                    YearlySummary(
                        name = img.attr("alt"),
                        imageLink = img.attr("src"),
                        activityLink = link
                    )
                }

            ParseResult.Success(HomePage(categories = categories, yearlySummary = yearlySummary))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse home page", e)
        }
    }
}
