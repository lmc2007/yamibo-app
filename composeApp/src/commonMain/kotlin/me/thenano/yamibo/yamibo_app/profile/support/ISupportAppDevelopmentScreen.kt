package me.thenano.yamibo.yamibo_app.profile.support

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot

@RestorableScreenEntry
class ISupportAppDevelopmentScreen : RestorableNavigatable {
    override val id = buildId("support_app_development")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        SupportAppDevelopmentScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ISupportAppDevelopmentScreen>(ISupportAppDevelopmentScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = ISupportAppDevelopmentScreen()
    }
}
