package me.thenano.yamibo.yamibo_app.webview.action

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

private const val MobileUserAgent =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

class ActionNavigationDelegate(
    val onLoadingChanged: (Boolean) -> Unit,
    val onUrlChanged: (String) -> Unit,
    val onSuccessDetected: () -> Unit,
    val successCondition: (String) -> Boolean,
) : NSObject(), WKNavigationDelegateProtocol {

    private var successFired = false

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onLoadingChanged(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onLoadingChanged(false)
        val currentUrl = webView.URL?.absoluteString ?: return
        onUrlChanged(currentUrl)

        if (!successFired && successCondition(currentUrl)) {
            successFired = true
            onSuccessDetected()
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
actual fun ActionPlatformWebView(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onSuccessDetected: () -> Unit,
    successCondition: (url: String) -> Boolean,
) {
    val delegate = remember {
        ActionNavigationDelegate(
            onLoadingChanged = onLoadingChanged,
            onUrlChanged = onUrlChanged,
            onSuccessDetected = onSuccessDetected,
            successCondition = successCondition,
        )
    }

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val request = NSURLRequest(NSURL(string = url))
            WKWebView().apply {
                this.navigationDelegate = delegate
                this.customUserAgent = MobileUserAgent
                loadRequest(request)
            }
        },
        update = {
            it.URL?.absoluteString?.let { urlStr -> onUrlChanged(urlStr) }
            it.title?.let { title -> onTitleChanged(title) }
        }
    )
}
