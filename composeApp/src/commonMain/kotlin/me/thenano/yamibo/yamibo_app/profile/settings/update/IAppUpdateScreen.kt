package me.thenano.yamibo.yamibo_app.profile.settings.update

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot

@RestorableScreenEntry
class IAppUpdateScreen : RestorableNavigatable {
    override val id = buildId("app-update")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        AppUpdateScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IAppUpdateScreen>(IAppUpdateScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = IAppUpdateScreen()
    }
}
