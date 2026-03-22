package me.thenano.yamibo.yamibo_app.thread.detail.tag

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.TagId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class ITagDetailScreen(
    private val tagId: TagId,
    private val title: String,
    private val page: Int? = null
) : Navigatable {
    override val id: String = buildId(tagId, page)

    @Composable
    override fun Content() {
        TagDetailScreen(tagId = tagId, tagName = title, initialPage = page)
    }
}