package me.thenano.yamibo.yamibo_app.webview.action

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

/** Navigatable screen for action-based WebView (reply, etc.) */
class IActionWebView(
    private val title: String,
    private val initialUrl: String,
    private val successCondition: (url: String) -> Boolean = { false },
    private val onSuccess: () -> Unit = {},
) : Navigatable {
    override val id = buildId(initialUrl.hashCode())

    @Composable
    override fun Content() {
        ActionWebViewScreen(
            title = title,
            initialUrl = initialUrl,
            successCondition = successCondition,
            onSuccess = onSuccess,
        )
    }
}
