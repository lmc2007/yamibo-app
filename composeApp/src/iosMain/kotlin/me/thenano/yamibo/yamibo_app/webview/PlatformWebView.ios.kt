package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject
import kotlinx.cinterop.ObjCSignatureOverride

class PlatformNavigationDelegate(
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onUrlChanged: (String) -> Unit,
    private val onTitleChanged: (String) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onLoadingChanged(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onLoadingChanged(false)
        webView.URL?.absoluteString?.let { onUrlChanged(it) }
        webView.title?.let { onTitleChanged(it) }
    }

    override fun webView(
        webView: WKWebView,
        didFailNavigation: WKNavigation?,
        withError: platform.Foundation.NSError
    ) {
        onLoadingChanged(false)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebViewContent(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onBack: (() -> Unit) -> Unit,
    onForward: (() -> Unit) -> Unit,
    onReload: (() -> Unit) -> Unit,
) {
    val navigator = LocalNavigator.current
    var webViewInstance by remember { mutableStateOf<WKWebView?>(null) }
    
    val delegate = remember {
        PlatformNavigationDelegate(onLoadingChanged, onUrlChanged, onTitleChanged)
    }

    // Wire up terminal functions
    LaunchedEffect(webViewInstance) {
        onBack { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() }
        onForward { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() }
        onReload { webViewInstance?.reload() }
    }

    // Prioritize webview back navigation
    DisposableEffect(webViewInstance) {
        val handler: () -> Boolean = {
            if (webViewInstance?.canGoBack() == true) {
                webViewInstance?.goBack()
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
    
    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WKWebView().apply {
                webViewInstance = this
                this.navigationDelegate = delegate
                loadRequest(NSURLRequest(NSURL(string = url)))
            }
        },
        update = {
            webViewInstance = it
        }
    )
}