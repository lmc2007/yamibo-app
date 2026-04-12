package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository

/** Navigatable screen for reading a novel thread in continuous mode. */
class IThreadReaderScreen(
    private val tid: ThreadId,
    private val title: String,
    private val threadType: ReadHistoryRepository.ThreadEntryType = ReadHistoryRepository.ThreadEntryType.Normal,
    private val authorId: UserId? = null,
    private val initialPage: Int = 1,
    private val targetPid: PostId? = null
) : Navigatable {
    override val id = buildId(tid.value, threadType.name, authorId?.value ?: "all")

    @Composable
    override fun Content() {
        ThreadReaderScreen(
            tid = tid,
            title = title,
            threadType = threadType,
            authorId = authorId,
            initialPage = initialPage,
            targetPid = targetPid
        )
    }
}
