package me.thenano.yamibo.yamibo_app.profile.settings.access

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IOSBackgroundAccessRepository : BackgroundAccessRepository {
    private val _state = MutableStateFlow(
        BackgroundAccessRepository.SetupState(
            summary = text("iOS 目前沒有 Android 那種常駐前景通知與長時間背景同步能力。"),
            items = listOf(
                BackgroundAccessRepository.SetupItem(
                    title = text("平台限制"),
                    subtitle = text("iOS 最多只能在進入背景後維持一小段時間執行。若系統掛起 App，收藏同步仍可能中斷。"),
                    status = BackgroundAccessRepository.SetupStatus.Info,
                ),
            ),
            platformNote = text("這個頁面在 iOS 主要是說明限制，不提供 Android 式的系統授權捷徑。"),
        ),
    )

    override val state: StateFlow<BackgroundAccessRepository.SetupState> = _state

    override suspend fun refresh() = Unit

    override fun runAction(action: BackgroundAccessRepository.SetupAction) = Unit

    private companion object {
        fun text(source: String, vararg args: Any?): BackgroundAccessRepository.I18nText =
            BackgroundAccessRepository.I18nText(source = source, args = args.toList())
    }
}

