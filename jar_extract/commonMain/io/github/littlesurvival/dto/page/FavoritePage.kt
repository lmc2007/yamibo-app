package io.github.littlesurvival.dto.page

import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.parse.util.ParseUtils

data class FavoritePage(
    val type: FavoriteType,
    val items: List<FavoriteItem>,
    val pageNav: PageNav? = null
)

data class FavoriteItem(
    val name: String,
    val url: String,
    val favId: FavoriteId
) {
    fun toThreadId(): ThreadId? {
        return ParseUtils.extractTid(this@FavoriteItem.url)
    }
}
enum class FavoriteType(val typeName: String, val typeId: String) {
    Thread("帖子", "thread"),
    Forum("版块", "forum"),
    Group("群组", "group"),
    Blog("日志", "blog"),
    Album("相册", "album")
}