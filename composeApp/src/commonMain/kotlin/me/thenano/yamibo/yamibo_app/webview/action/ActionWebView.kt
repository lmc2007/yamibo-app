package me.thenano.yamibo.yamibo_app.webview.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.webview.PlatformWebViewScreen

@Composable
internal fun ActionWebViewScreen(
    title: String,
    initialUrl: String,
    successCondition: (url: String) -> Boolean,
    onSuccess: () -> Unit,
) {
    val navigator = LocalNavigator.current
    var successHandled by remember(initialUrl) { mutableStateOf(false) }

    fun handleSuccess(url: String): Boolean {
        if (!successHandled && successCondition(url)) {
            successHandled = true
            onSuccess()
            navigator.pop()
            return true
        }
        return false
    }

    PlatformWebViewScreen(
        initialUrl = initialUrl,
        initialTitle = title,
        showNavigation = false,
        useBackIcon = true,
        shouldOverrideUrlLoading = { handleSuccess(it) },
        onPageFinished = { handleSuccess(it) },
    )
}
