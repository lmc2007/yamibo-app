package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class ISettingsScreen : Navigatable {
    override val id = buildId("settings")

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}
