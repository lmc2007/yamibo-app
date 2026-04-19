package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject
import kotlinx.cinterop.ObjCSignatureOverride

class PlatformNavigationDelegate(
    private val syncAuthCookies: Boolean,
    private val captureHtml: Boolean,
    private val onPageFinished: (String) -> Unit,
    private val onLoadingChanged: (Boolean) -> Unit,
    private val onUrlChanged: (String) -> Unit,
    private val onTitleChanged: (String) -> Unit,
    private val onHtmlAvailable: (String, String) -> Unit,
    private val authCookieSync: () -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onLoadingChanged(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onLoadingChanged(false)
        val currentUrl = webView.URL?.absoluteString ?: return
        onUrlChanged(currentUrl)
        webView.title?.let { onTitleChanged(it) }
        if (syncAuthCookies) {
            authCookieSync()
        }
        onPageFinished(currentUrl)
        if (captureHtml) {
            webView.evaluateJavaScript("document.documentElement.outerHTML") { result, _ ->
                val html = result as? String ?: return@evaluateJavaScript
                if (html.isNotBlank()) {
                    onHtmlAvailable(currentUrl, html)
                }
            }
        }
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
    syncAuthCookies: Boolean,
    captureHtml: Boolean,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onBack: (() -> Unit) -> Unit,
    onForward: (() -> Unit) -> Unit,
    onReload: (() -> Unit) -> Unit,
    onPageFinished: (String) -> Unit,
    onHtmlAvailable: (url: String, html: String) -> Unit,
    shouldOverrideUrlLoading: (String) -> Boolean,
) {
    val navigator = LocalNavigator.current
    val authRepo = LocalAuthRepository.current
    var webViewInstance by remember { mutableStateOf<WKWebView?>(null) }

    val delegate = remember {
        PlatformNavigationDelegate(
            syncAuthCookies = syncAuthCookies,
            captureHtml = captureHtml,
            onPageFinished = onPageFinished,
            onLoadingChanged = onLoadingChanged,
            onUrlChanged = onUrlChanged,
            onTitleChanged = onTitleChanged,
            onHtmlAvailable = onHtmlAvailable,
            authCookieSync = { authRepo.syncCookieFromWebView() },
        )
    }

    LaunchedEffect(webViewInstance) {
        onBack { if (webViewInstance?.canGoBack() == true) webViewInstance?.goBack() }
        onForward { if (webViewInstance?.canGoForward() == true) webViewInstance?.goForward() }
        onReload { webViewInstance?.reload() }
    }

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
