package me.thenano.yamibo.yamibo_app.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.__error_tag
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator

@SuppressLint("SetJavaScriptEnabled")
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
    val authRepo = LocalAuthRepository.current
    val cookies = authRepo.cookieStore.load() ?: ""
    
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

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

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewInstance = this
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChanged(false)
                        url?.let { onUrlChanged(it) }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.e(__error_tag("WebView"), error?.description.toString())
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUrl = request?.url?.toString() ?: return false
                        if (targetUrl.contains("mod=viewthread") && targetUrl.contains("tid=")) {
                            navigator.pop()
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
                    builtInZoomControls = true
                    displayZoomControls = false
                    javaScriptCanOpenWindowsAutomatically = true

                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString =
                        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                    setSupportZoom(true)
                }

                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                if (cookies.isNotEmpty()) {
                    cookies.split(";").forEach {
                        cookieManager.setCookie(url, it.trim())
                    }
                    cookieManager.flush()
                }

                loadUrl(url)
            }
        },
        update = {
            webViewInstance = it
        }
    )
}
