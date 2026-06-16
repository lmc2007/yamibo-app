package me.thenano.yamibo.yamibo_app.userspace

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.IMainScreen
import me.thenano.yamibo.yamibo_app.MainTab
import me.thenano.yamibo.yamibo_app.message.IMessageCenterScreen
import me.thenano.yamibo.yamibo_app.message.MessageCenterTab
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class UserSpaceScreenRestorePayload(
    val userId: Int? = null,
    val titleHint: String? = null,
    val groupName: String = UserSpaceSection.Space.name,
    val initialTabName: String = UserSpaceSubPage.Profile.name,
)
@RestorableScreenEntry
class IUserSpaceScreen(
    val userId: UserId? = null,
    val titleHint: String? = null,
    val group: UserSpaceSection = UserSpaceSection.Space,
    val initialTab: UserSpaceSubPage = UserSpaceSubPage.Profile,
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
        UserSpaceScreen(userId = userId, titleHint = titleHint, group = group, initialTab = initialTab)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IUserSpaceScreen>(IUserSpaceScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<UserSpaceScreenRestorePayload>(payload)
            if (data.groupName == "Messages") {
                if (data.initialTabName == "Updates") {
                    return IMainScreen(initialTab = MainTab.Updates)
                }
                return IMessageCenterScreen(
                    initialTab = when (data.initialTabName) {
                        "Notices" -> MessageCenterTab.Notices
                        else -> MessageCenterTab.PrivateMessages
                    },
                )
            }
            return IUserSpaceScreen(
                userId = data.userId?.let(::UserId),
                titleHint = data.titleHint,
                group = UserSpaceSection.valueOf(data.groupName),
                initialTab = UserSpaceSubPage.valueOf(data.initialTabName),
            )
        }
    }
}
