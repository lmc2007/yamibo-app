package me.thenano.yamibo.yamibo_app.webview

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.thenano.yamibo.yamibo_app.__error_tag
import me.thenano.yamibo.yamibo_app.__info__tag
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator

import androidx.compose.ui.platform.LocalUriHandler

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(url: String) {
    val navigator = LocalNavigator.current
    val authRepo = LocalAuthRepository.current
    val uriHandler = LocalUriHandler.current
    val cookies = authRepo.cookieStore.load() ?: ""
    
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf("WebView") }
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
            title = currentTitle,
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { currentUrl = it }
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
                            return false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { currentTitle = it }
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
                webView = it
            }
        )
    }
}