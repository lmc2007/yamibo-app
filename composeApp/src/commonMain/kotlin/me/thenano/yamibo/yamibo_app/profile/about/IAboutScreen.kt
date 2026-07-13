package me.thenano.yamibo.yamibo_app.profile.about

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot

@RestorableScreenEntry
class IAboutScreen : RestorableNavigatable {
    override val id = buildId("about")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        AboutScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IAboutScreen>(IAboutScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = IAboutScreen()
    }
}
