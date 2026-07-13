package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class CommentReaderRestorePayload(
    val tid: Int,
    val postTitle: String,
    val oPostId: Int,
    val authorId: Int,
    val targetCommentPid: Int? = null,
)

/** Navigatable screen for viewing comments of a specific author post. */
@RestorableScreenEntry
class ICommentReaderScreen(
    val tid: ThreadId,
    val postTitle: String,
    val oPostId: PostId,
    val authorId: UserId,
    val targetCommentPid: PostId? = null,
) : RestorableNavigatable {
    override val id = buildId(tid.value, oPostId.value, targetCommentPid?.value ?: "top")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = CommentReaderRestorePayload(
            tid = tid.value,
            postTitle = postTitle,
            oPostId = oPostId.value,
            authorId = authorId.value,
            targetCommentPid = targetCommentPid?.value,
        ),
    )

    @Composable
    override fun Content() {
        CommentReaderScreen(
            tid = tid,
            postTitle = postTitle,
            oPostId = oPostId,
            authorId = authorId,
            targetCommentPid = targetCommentPid,
        )
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ICommentReaderScreen>(ICommentReaderScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<CommentReaderRestorePayload>(payload)
            return ICommentReaderScreen(
                tid = ThreadId(data.tid),
                postTitle = data.postTitle,
                oPostId = PostId(data.oPostId),
                authorId = UserId(data.authorId),
                targetCommentPid = data.targetCommentPid?.let(::PostId),
            )
        }
    }
}
