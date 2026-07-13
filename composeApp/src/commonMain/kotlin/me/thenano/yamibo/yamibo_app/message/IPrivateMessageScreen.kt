package me.thenano.yamibo.yamibo_app.message

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class PrivateMessageRestorePayload(
    val toUser: Int,
    val titleHint: String? = null,
)
@RestorableScreenEntry
class IPrivateMessageScreen(
    val toUser: UserId,
    val titleHint: String? = null,
) : RestorableNavigatable {
    override val id = buildId(toUser.value)
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = PrivateMessageRestorePayload(
            toUser = toUser.value,
            titleHint = titleHint,
        ),
    )

    @Composable
    override fun Content() {
        PrivateMessageScreen(toUser = toUser, titleHint = titleHint)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IPrivateMessageScreen>(IPrivateMessageScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<PrivateMessageRestorePayload>(payload)
            return IPrivateMessageScreen(
                toUser = UserId(data.toUser),
                titleHint = data.titleHint,
            )
        }
    }
}
