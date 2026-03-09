package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(url: String) {
    val navigator = LocalNavigator.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var webView by remember { mutableStateOf<WKWebView?>(null) }
    var currentUrl by remember { mutableStateOf(url) }
    
    // Prioritize webview back navigation
    DisposableEffect(webView) {
        val handler: () -> Boolean = {
            if (webView?.canGoBack() == true) {
                webView?.goBack()
                true
            } else {
                false
            }
        }
        navigator.backHandlers.add(handler)
        onDispose {
            navigator.backHandlers.remove(handler)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        WebViewTopBar(
            title = "WebView", // iOS is trickier to get real-time title without KVO, sticking to placeholder or URL
            url = currentUrl,
            onCloseClick = { navigator.pop() },
            onBackClick = {
                if (webView?.canGoBack() == true) {
                    webView?.goBack()
                }
            },
            onForwardClick = {
                if (webView?.canGoForward() == true) {
                    webView?.goForward()
                }
            },
            onRefreshClick = {
                webView?.reload()
            },
            onOpenBrowserClick = {
                try {
                    uriHandler.openUri(currentUrl)
                } catch (_: Exception) {}
            }
        )
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                WKWebView().apply {
                    webView = this
                    loadRequest(NSURLRequest(NSURL(string = url)))
                }
            },
            update = {
                webView = it
                it.URL?.absoluteString?.let { currentUrl = it }
            }
        )
    }
}