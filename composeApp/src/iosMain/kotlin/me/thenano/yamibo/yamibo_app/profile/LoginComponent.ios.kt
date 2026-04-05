package me.thenano.yamibo.yamibo_app.profile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.LoginSuccessEvent
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.darwin.NSObject

private const val MobileUserAgent =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

class YamiboLoginNavigationDelegate(
    val onLoadingChanged: (Boolean) -> Unit,
    val onCookieSync: () -> Unit
) : NSObject(), WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onLoadingChanged(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onLoadingChanged(false)
        onCookieSync()
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
actual fun LoginWebView(onLoadingChanged: (Boolean) -> Unit) {
    val authRepo = LocalAuthRepository.current
    val navigator = LocalNavigator.current

    val delegate = remember {
        YamiboLoginNavigationDelegate(
            onLoadingChanged = onLoadingChanged,
            onCookieSync = { authRepo.syncCookieFromWebView() }
        )
    }

    // Since we need internet permission and webkit loading
    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            val urlString = YamiboRoute.Login.build()
            val request = NSURLRequest(NSURL(string = urlString))
            WKWebView().apply {
                this.navigationDelegate = delegate
                this.customUserAgent = MobileUserAgent
                loadRequest(request)
            }
        },
        update = {}
    )

    LaunchedEffect(Unit) {
        authRepo.startLoginDetect(
            onSuccess = {
                val status = authRepo.fetchStatus()
                if (status is YamiboResult.Success) {
                    AppEventBus.emit(LoginSuccessEvent)
                }
                navigator.pop()
            },
            onTimeOut = {
                // Do something on timeout
                navigator.pop()
            }
        )
    }
}
