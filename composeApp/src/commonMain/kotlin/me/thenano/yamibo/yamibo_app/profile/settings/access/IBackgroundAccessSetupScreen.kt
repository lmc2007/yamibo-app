package me.thenano.yamibo.yamibo_app.profile.settings.access

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class IBackgroundAccessSetupScreen : Navigatable {
    override val id = buildId("background-access-setup")

    @Composable
    override fun Content() {
        BackgroundAccessSetupScreen()
    }
}
