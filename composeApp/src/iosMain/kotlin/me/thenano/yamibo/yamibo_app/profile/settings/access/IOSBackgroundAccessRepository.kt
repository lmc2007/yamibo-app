package me.thenano.yamibo.yamibo_app.profile.settings.access

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IOSBackgroundAccessRepository : BackgroundAccessRepository {
    private val _state = MutableStateFlow(
        BackgroundAccessRepository.SetupState(
            summary = appString(Res.string.ui_ios_does_not_have_persistent_foreground_notifications_long_term),
            items = listOf(
                BackgroundAccessRepository.SetupItem(
                    title = appString(Res.string.ui_platform_restrictions),
                    subtitle = appString(Res.string.ui_ios_can_only_maintain_execution_for_short_period_time_entering),
                    status = BackgroundAccessRepository.SetupStatus.Info,
                ),
            ),
            platformNote = appString(Res.string.ui_this_page_mainly_explains_restrictions_on_ios_does_not_provide),
        ),
    )

    override val state: StateFlow<BackgroundAccessRepository.SetupState> = _state

    override suspend fun refresh() = Unit

    override fun runAction(action: BackgroundAccessRepository.SetupAction) = Unit
}

