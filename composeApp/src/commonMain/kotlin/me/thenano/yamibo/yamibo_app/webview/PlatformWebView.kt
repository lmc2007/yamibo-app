package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import me.thenano.yamibo.yamibo_app.profile.LoadingOverlay
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Platform-specific WebView content implementation */
@Composable
expect fun PlatformWebViewContent(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onBack: (() -> Unit) -> Unit,
    onForward: (() -> Unit) -> Unit,
    onReload: (() -> Unit) -> Unit,
)

@Composable
internal fun PlatformWebViewScreen(initialUrl: String) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val colors = YamiboTheme.colors

    var currentTitle by remember { mutableStateOf("WebView") }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var loading by remember { mutableStateOf(true) }
    
    var backFunc by remember { mutableStateOf<(() -> Unit)?>(null) }
    var forwardFunc by remember { mutableStateOf<(() -> Unit)?>(null) }
    var reloadFunc by remember { mutableStateOf<(() -> Unit)?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(colors.brownDeep)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            WebViewTopBar(
                title = currentTitle,
                url = currentUrl,
                onCloseClick = { navigator.pop() },
                onBackClick = { backFunc?.invoke() },
                onForwardClick = { forwardFunc?.invoke() },
                onRefreshClick = { reloadFunc?.invoke() },
                onOpenBrowserClick = {
                    try {
                        uriHandler.openUri(currentUrl)
                    } catch (_: Exception) {}
                }
            )
            Box(modifier = Modifier.weight(1f)) {
                PlatformWebViewContent(
                    url = initialUrl,
                    onTitleChanged = { currentTitle = it },
                    onUrlChanged = { currentUrl = it },
                    onLoadingChanged = { loading = it },
                    onBack = { backFunc = it },
                    onForward = { forwardFunc = it },
                    onReload = { reloadFunc = it }
                )
                LoadingOverlay(visible = loading)
            }
        }
    }
}

class IPlatformWebView(val link: String = "https://bbs.yamibo.com/") : Navigatable {
    override val id = buildId(link)

    @Composable
    override fun Content() {
        PlatformWebViewScreen(link)
    }
}