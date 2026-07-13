package me.thenano.yamibo.yamibo_app.profile.settings.backup

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.emptyRestoreSnapshot

@RestorableScreenEntry
class IBackupSettingsScreen : RestorableNavigatable {
    override val id = buildId("backup_settings")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        BackupSettingsScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IBackupSettingsScreen>(IBackupSettingsScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = IBackupSettingsScreen()
    }
}
