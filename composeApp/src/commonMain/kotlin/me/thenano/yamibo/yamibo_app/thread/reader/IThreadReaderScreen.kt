package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository

@Serializable
private data class ThreadReaderRestorePayload(
    val tid: Int,
    val title: String,
    val threadTypeName: String = ReadHistoryRepository.ThreadEntryType.Normal.name,
    val authorId: Int? = null,
    val initialPage: Int = 1,
    val targetPid: Int? = null,
)

/** Navigatable screen for reading a novel thread in continuous mode. */
class IThreadReaderScreen(
    val tid: ThreadId,
    val title: String,
    val threadType: ReadHistoryRepository.ThreadEntryType = ReadHistoryRepository.ThreadEntryType.Normal,
    val authorId: UserId? = null,
    val initialPage: Int = 1,
    val targetPid: PostId? = null
) : RestorableNavigatable {
    override val id = buildId(tid.value, threadType.name, authorId?.value ?: "all")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = ThreadReaderRestorePayload(
            tid = tid.value,
            title = title,
            threadTypeName = threadType.name,
            authorId = authorId?.value,
            initialPage = initialPage,
            targetPid = targetPid?.value,
        ),
    )

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

    companion object Decoder : TypedRestorableNavigatableDecoder<IThreadReaderScreen>(IThreadReaderScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<ThreadReaderRestorePayload>(payload)
            return IThreadReaderScreen(
                tid = ThreadId(data.tid),
                title = data.title,
                threadType = ReadHistoryRepository.ThreadEntryType.valueOf(data.threadTypeName),
                authorId = data.authorId?.let(::UserId),
                initialPage = data.initialPage,
                targetPid = data.targetPid?.let(::PostId),
            )
        }
    }
}
