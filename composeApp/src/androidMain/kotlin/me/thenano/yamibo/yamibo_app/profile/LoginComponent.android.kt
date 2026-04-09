package me.thenano.yamibo.yamibo_app.profile

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.LoginSuccessEvent

private const val MobileUserAgent =
    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun LoginWebView(onLoadingChanged: (Boolean) -> Unit) {
    val authRepo = LocalAuthRepository.current
    val navigator = LocalNavigator.current
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        factory = { context ->
            WebView(context).apply {
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?
                        ) {
                            onLoadingChanged(true)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            me.thenano.yamibo.yamibo_app.Logger.e(
                                "LoginWebView",
                                error?.description.toString()
                            )
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            onLoadingChanged(false)
                            authRepo.syncCookieFromWebView()
                        }
                    }
                webChromeClient = WebChromeClient()

                settings.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = MobileUserAgent
                }
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                loadUrl(YamiboRoute.Login.build())
            }
        },
        update = {}
    )
    LaunchedEffect(Unit) {
        authRepo.startLoginDetect(
            onSuccess = {
                val status = authRepo.fetchStatus()
                if (status !is YamiboResult.Success) {
                    Toast.makeText(context, status.message(), Toast.LENGTH_LONG).show()
                } else {
                    AppEventBus.emit(LoginSuccessEvent)
                }
                navigator.pop()
            },
            onTimeOut = {
                Toast.makeText(context, "登入超時", Toast.LENGTH_LONG).show()
                navigator.pop()
            }
        )
    }
}
