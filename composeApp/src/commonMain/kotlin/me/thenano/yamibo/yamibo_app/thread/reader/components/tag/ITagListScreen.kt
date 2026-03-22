package me.thenano.yamibo.yamibo_app.thread.reader.components.tag

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.model.TagValue
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

/** Entry point for the tag list screen */
class ITagListScreen(
    private val tid: ThreadId,
    private val initialTags: List<TagValue>
) : Navigatable {
    override val id: String = buildId(tid)

    @Composable
    override fun Content() {
        TagListScreen(tid = tid, initialTags = initialTags)
    }
}