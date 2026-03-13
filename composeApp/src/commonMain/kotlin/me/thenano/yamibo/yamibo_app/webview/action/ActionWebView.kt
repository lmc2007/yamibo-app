package me.thenano.yamibo.yamibo_app.webview.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import me.thenano.yamibo.yamibo_app.auth.LoadingOverlay
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.webview.WebViewTopBar

@Composable
expect fun ActionPlatformWebView(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onSuccessDetected: () -> Unit,
    successCondition: (url: String) -> Boolean,
)

@Composable
internal fun ActionWebViewScreen(
    title: String,
    initialUrl: String,
    successCondition: (url: String) -> Boolean,
    onSuccess: () -> Unit,
) {
    val navigator = LocalNavigator.current

    var currentTitle by remember { mutableStateOf(title) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var loading by remember { mutableStateOf(true) }
    val colors = YamiboTheme.colors

    Box(modifier = Modifier.fillMaxSize().background(colors.brownDeep)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            WebViewTopBar(
                title = currentTitle,
                url = currentUrl,
                onCloseClick = { navigator.pop() },
                showNavigation = false,
                useBackIcon = true,
            )
            Box(modifier = Modifier.weight(1f)) {
                ActionPlatformWebView(
                    url = initialUrl,
                    onTitleChanged = { currentTitle = it },
                    onUrlChanged = { currentUrl = it },
                    onLoadingChanged = { loading = it },
                    onSuccessDetected = {
                        onSuccess()
                        navigator.pop()
                    },
                    successCondition = successCondition,
                )
                LoadingOverlay(visible = loading)
            }
        }
    }
}
