package me.thenano.yamibo.yamibo_app.webview

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.thenano.yamibo.yamibo_app.__error_tag
import me.thenano.yamibo.yamibo_app.__info__tag
import me.thenano.yamibo.yamibo_app.component.ReturnButtonTopBar
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun PlatformWebView(url: String) {
    val navigator = LocalNavigator.current
    Column(modifier = Modifier.fillMaxSize()) {
        ReturnButtonTopBar("WebView", onBackClick = { navigator.pop() })
        AndroidView(
            factory = {
                WebView(it).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            Log.e(__error_tag("WebView"), error?.description.toString())
                        }
                    }
                    webChromeClient = WebChromeClient()
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        javaScriptCanOpenWindowsAutomatically = true

                        cacheMode = WebSettings.LOAD_DEFAULT
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"


                        setSupportZoom(true)
                    }
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    loadUrl(url)
                }
            }
        )
    }

    Log.i(__info__tag("WebView"), "Navigator Size : ${navigator.stack.size}, canBack : ${navigator.canGoBack()}")
}