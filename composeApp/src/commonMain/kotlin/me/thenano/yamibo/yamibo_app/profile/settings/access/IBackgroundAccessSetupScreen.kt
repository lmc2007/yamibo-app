package me.thenano.yamibo.yamibo_app.profile.settings.access

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot

class IBackgroundAccessSetupScreen : RestorableNavigatable {
    override val id = buildId("background-access-setup")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        BackgroundAccessSetupScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IBackgroundAccessSetupScreen>(IBackgroundAccessSetupScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = IBackgroundAccessSetupScreen()
    }
}
