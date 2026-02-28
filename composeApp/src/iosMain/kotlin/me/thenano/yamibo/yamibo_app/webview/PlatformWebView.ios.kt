package me.thenano.yamibo.yamibo_app.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(url: String) {
    UIKitView(
        factory = {
            WKWebView().apply {
                loadRequest(NSURLRequest(NSURL(string = url)))
            }
        }
    )
}