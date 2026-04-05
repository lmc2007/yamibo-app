package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class ISettingsCategoryScreen(
    private val category: String
) : Navigatable {
    override val id = buildId("settings", category)

    @Composable
    override fun Content() {
        SettingsCategoryScreen(category = category)
    }
}
