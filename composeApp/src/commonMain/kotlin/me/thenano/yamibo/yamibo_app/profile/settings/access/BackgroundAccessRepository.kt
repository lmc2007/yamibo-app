package me.thenano.yamibo.yamibo_app.profile.settings.access

import kotlinx.coroutines.flow.StateFlow

interface BackgroundAccessRepository {
    val state: StateFlow<SetupState>

    suspend fun refresh()

    fun runAction(action: SetupAction)

    data class SetupState(
        val summary: String,
        val items: List<SetupItem>,
        val platformNote: String? = null,
    )

    data class SetupItem(
        val title: String,
        val subtitle: String,
        val status: SetupStatus,
        val actionLabel: String? = null,
        val action: SetupAction? = null,
    )

    enum class SetupStatus {
        Granted,
        Required,
        Recommended,
        Info,
    }

    enum class SetupAction {
        RequestNotificationPermission,
        OpenNotificationSettings,
        OpenBatteryOptimizationSettings,
        OpenAppSettings,
        OpenDontKillMyApp,
    }
}
