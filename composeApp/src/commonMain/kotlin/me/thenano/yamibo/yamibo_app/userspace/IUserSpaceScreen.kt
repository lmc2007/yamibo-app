package me.thenano.yamibo.yamibo_app.userspace

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class UserSpaceScreenRestorePayload(
    val userId: Int? = null,
    val titleHint: String? = null,
    val groupName: String = UserSpaceGroup.Space.name,
    val initialTabName: String = UserSpaceTab.Profile.name,
)

class IUserSpaceScreen(
    val userId: UserId? = null,
    val titleHint: String? = null,
    val group: UserSpaceGroup = UserSpaceGroup.Space,
    val initialTab: UserSpaceTab = UserSpaceTab.Profile,
) : RestorableNavigatable {
    override val id = buildId(userId?.value ?: "self", group.name, initialTab.name)
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = UserSpaceScreenRestorePayload(
            userId = userId?.value,
            titleHint = titleHint,
            groupName = group.name,
            initialTabName = initialTab.name,
        ),
    )

    @Composable
    override fun Content() {
        UserSpacePage(userId = userId, titleHint = titleHint, group = group, initialTab = initialTab)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IUserSpaceScreen>(IUserSpaceScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<UserSpaceScreenRestorePayload>(payload)
            return IUserSpaceScreen(
                userId = data.userId?.let(::UserId),
                titleHint = data.titleHint,
                group = UserSpaceGroup.valueOf(data.groupName),
                initialTab = UserSpaceTab.valueOf(data.initialTabName),
            )
        }
    }
}
