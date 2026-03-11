package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

/** Navigatable screen for reading a novel thread in continuous mode. */
class IThreadReaderScreen(
    private val tid: ThreadId,
    private val title: String,
    private val authorId: UserId? = null,
    private val initialPage: Int = 1,
    private val targetPid: PostId? = null,
    private val isAuthorOnly: Boolean = false
) : Navigatable {
    override val id = "ReaderScreen_${tid.value}_${authorId?.value ?: "all"}"

    @Composable
    override fun Content() {
        ThreadReaderScreen(
            tid = tid,
            title = title,
            authorId = authorId,
            initialPage = initialPage,
            targetPid = targetPid,
            isAuthorOnly = isAuthorOnly
        )
    }
}
