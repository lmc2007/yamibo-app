package me.thenano.yamibo.yamibo_app.webview.action

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.thenano.yamibo.yamibo_app.__error_tag
import me.thenano.yamibo.yamibo_app.LocalAuthRepository

private const val MobileUserAgent =
    "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun ActionPlatformWebView(
    url: String,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onSuccessDetected: () -> Unit,
    successCondition: (url: String) -> Boolean,
) {
    val authRepo = LocalAuthRepository.current
    var successFired by remember { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                    }

                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                        super.onPageFinished(view, pageUrl)
                        onLoadingChanged(false)
                        pageUrl?.let { onUrlChanged(it) }

                        if (!successFired && pageUrl != null && successCondition(pageUrl)) {
                            successFired = true
                            onSuccessDetected()
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(__error_tag("ActionWebView"), error?.description.toString())
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUrl = request?.url?.toString() ?: return false
                        if (!successFired && successCondition(targetUrl)) {
                            successFired = true
                            onSuccessDetected()
                            return true
                        }
                        return false
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }
                }
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true

                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = MobileUserAgent
                }

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                loadUrl(url)
            }
        },
        update = {}
    )
}
