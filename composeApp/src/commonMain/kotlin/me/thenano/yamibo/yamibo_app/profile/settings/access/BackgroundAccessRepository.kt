package me.thenano.yamibo.yamibo_app.profile.settings.access

import kotlinx.coroutines.flow.StateFlow

interface BackgroundAccessRepository {
    val state: StateFlow<SetupState>

    suspend fun refresh()

    fun runAction(action: SetupAction)

    data class I18nText(
        val source: String,
        val args: List<Any?> = emptyList(),
    )

    data class SetupState(
        val summary: I18nText,
        val items: List<SetupItem>,
        val platformNote: I18nText? = null,
    )

    data class SetupItem(
        val title: I18nText,
        val subtitle: I18nText,
        val status: SetupStatus,
        val actionLabel: I18nText? = null,
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
