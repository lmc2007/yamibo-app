package me.thenano.yamibo.yamibo_app.thread.detail.novel

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class NovelThreadDetailRestorePayload(
    val tid: Int,
    val title: String,
    val authorId: Int? = null,
)

/** Navigatable screen for viewing a novel thread. */
class INovelThreadDetailScreen(
    val tid: ThreadId,
    val title: String,
    val authorId: UserId? = null
) : RestorableNavigatable {
    override val id = buildId(tid.value, "Novel", authorId?.value ?: "none")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = NovelThreadDetailRestorePayload(
            tid = tid.value,
            title = title,
            authorId = authorId?.value,
        ),
    )

    @Composable
    override fun Content() {
        NovelThreadDetailScreen(tid = tid, title = title, authorId = authorId)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<INovelThreadDetailScreen>(INovelThreadDetailScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<NovelThreadDetailRestorePayload>(payload)
            return INovelThreadDetailScreen(
                tid = ThreadId(data.tid),
                title = data.title,
                authorId = data.authorId?.let(::UserId),
            )
        }
    }
}
