package me.thenano.yamibo.yamibo_app.profile

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot

@RestorableScreenEntry
class IProfileStatisticsScreen : RestorableNavigatable {
    override val id = buildId("profile_statistics")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        ProfileStatisticsModule()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IProfileStatisticsScreen>(IProfileStatisticsScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = IProfileStatisticsScreen()
    }
}
