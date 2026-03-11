package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

/** Navigatable screen for viewing comments of a specific author post. */
class ICommentReaderScreen(
    private val tid: ThreadId,
    private val postTitle: String,
    private val oPostId: PostId,
    private val authorId: UserId
) : Navigatable {
    override val id = "CommentReader_${tid.value}_${oPostId.value}"

    @Composable
    override fun Content() {
        CommentReaderScreen(
            tid = tid,
            postTitle = postTitle,
            oPostId = oPostId,
            authorId = authorId
        )
    }
}
