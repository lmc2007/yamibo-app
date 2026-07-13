package me.thenano.yamibo.yamibo_app.userspace.blog

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.BlogId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class BlogReaderRestorePayload(
    val blogId: Int,
    val userId: Int? = null,
    val titleHint: String? = null,
)
@RestorableScreenEntry
class IBlogReaderScreen(
    val blogId: BlogId,
    val userId: UserId? = null,
    val titleHint: String? = null,
) : RestorableNavigatable {
    override val id = buildId(blogId.value, userId?.value ?: "self")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = BlogReaderRestorePayload(
            blogId = blogId.value,
            userId = userId?.value,
            titleHint = titleHint,
        ),
    )

    @Composable
    override fun Content() {
        BlogReaderScreen(blogId = blogId, userId = userId, titleHint = titleHint)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IBlogReaderScreen>(IBlogReaderScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<BlogReaderRestorePayload>(payload)
            return IBlogReaderScreen(
                blogId = BlogId(data.blogId),
                userId = data.userId?.let(::UserId),
                titleHint = data.titleHint,
            )
        }
    }
}
