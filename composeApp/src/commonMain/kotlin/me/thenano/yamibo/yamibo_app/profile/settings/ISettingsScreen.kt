package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot
@RestorableScreenEntry
class ISettingsScreen : RestorableNavigatable {
    override val id = buildId("settings")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        SettingsScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ISettingsScreen>(ISettingsScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = ISettingsScreen()
    }
}
