package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot

@Serializable
private data class SettingsCategoryRestorePayload(
    val category: String,
)
@RestorableScreenEntry
class ISettingsCategoryScreen(
    val category: String
) : RestorableNavigatable {
    override val id = buildId("settings", category)
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = SettingsCategoryRestorePayload(category = category),
    )

    @Composable
    override fun Content() {
        SettingsCategoryScreen(category = category)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ISettingsCategoryScreen>(ISettingsCategoryScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<SettingsCategoryRestorePayload>(payload)
            return ISettingsCategoryScreen(category = data.category)
        }
    }
}
