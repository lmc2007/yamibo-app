package me.thenano.yamibo.yamibo_app.thread.novel

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

/** Navigatable screen for viewing a novel thread. */
class INovelThreadDetailScreen(
    private val tid: ThreadId,
    private val title: String,
    private val authorId: UserId? = null
) : Navigatable {
    override val id = "ThreadScreen_${tid.value}_${authorId?.value ?: "none"}"

    @Composable
    override fun Content() {
        NovelThreadDetailScreen(tid = tid, title = title, authorId = authorId)
    }
}
