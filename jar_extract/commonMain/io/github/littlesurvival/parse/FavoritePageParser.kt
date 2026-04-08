package io.github.littlesurvival.parse

import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.Parser
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.dto.page.FavoriteItem
import io.github.littlesurvival.dto.page.FavoritePage
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.parse.util.ParseUtils

class FavoritePageParser : Parser<FavoritePage> {

    override suspend fun parse(html: String): ParseResult<FavoritePage> {
        return try {
            val doc = Ksoup.parse(html)
            if (ParseUtils.isMaintenance(doc)) return ParseResult.Maintenance
            if (ParseUtils.isNotLoggedIn(doc)) return ParseResult.NotLoggedIn
            if (ParseUtils.isNoPermission(doc)) return ParseResult.NoPermission(ParseUtils.parsePromptMessage(doc))

            // --- Favorite type ---
            val activeTab = doc.selectFirst("#dhnav_li a.mon")
            val activeTabText = activeTab?.text()?.trim() ?: ""
            val type =
                FavoriteType.entries.firstOrNull { it.typeName == activeTabText }
                    ?: FavoriteType.Thread

            // --- Favorite items ---
            val items = mutableListOf<FavoriteItem>()
            val itemEls = doc.select(".findbox ul li.sclist")
            for (li in itemEls) {
                // Delete link
                val deleteLink = li.selectFirst("a.mdel") ?: continue
                val deleteUrl = deleteLink.attr("href")
                val fvIdValue = Regex("[?&]favid=(\\d+)").find(deleteUrl)?.groupValues?.get(1)?.toIntOrNull() ?: continue
                val favId = io.github.littlesurvival.dto.value.FavoriteId(fvIdValue)

                // Content link (the second <a>, not the delete one)
                val contentLink = li.selectFirst("a:not(.mdel)") ?: continue
                val url = contentLink.attr("href")
                val name = contentLink.text().trim()

                items.add(FavoriteItem(name = name, url = url, favId = favId))
            }

            // --- Pagination ---
            val pageNav = ParseUtils.parsePageNav(doc)

            ParseResult.Success(FavoritePage(type = type, items = items, pageNav = pageNav))
        } catch (e: Exception) {
            ParseResult.Failure("Failed to parse favorite page", e)
        }
    }
}
