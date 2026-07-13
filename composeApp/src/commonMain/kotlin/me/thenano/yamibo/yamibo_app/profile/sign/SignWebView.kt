package me.thenano.yamibo.yamibo_app.profile.sign

import me.thenano.yamibo.yamibo_app.i18n.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.webview.PlatformWebViewScreen

internal class ISignWebView(
    private val semiAutomatic: Boolean,
    private val onCfCleared: () -> Unit = {},
    private val onResultObserved: () -> Unit = {},
    private val onMaintenanceObserved: () -> Unit = {},
    private val onLoadFailed: (String) -> Unit = {},
) : Navigatable {
    override val id = buildId("sign-webview", semiAutomatic)

    @Composable
    override fun Content() {
        SignWebViewScreen(
            semiAutomatic = semiAutomatic,
            onCfCleared = onCfCleared,
            onResultObserved = onResultObserved,
            onMaintenanceObserved = onMaintenanceObserved,
            onLoadFailed = onLoadFailed,
        )
    }
}

@Composable
private fun SignWebViewScreen(
    semiAutomatic: Boolean,
    onCfCleared: () -> Unit,
    onResultObserved: () -> Unit,
    onMaintenanceObserved: () -> Unit,
    onLoadFailed: (String) -> Unit,
) {
    val navigator = LocalNavigator.current
    val authRepository = LocalAuthRepository.current
    val signRepository = LocalSignRepository.current
    val scope = rememberCoroutineScope()
    var handledMaintenancePage by remember(semiAutomatic) { mutableStateOf(false) }
    var handledLoadFailure by remember(semiAutomatic) { mutableStateOf(false) }
    var autoSignStarted by remember(semiAutomatic) { mutableStateOf(false) }
    var autoSignChecking by remember(semiAutomatic) { mutableStateOf(false) }

    fun maybeStartSemiAutoSign() {
        if (!semiAutomatic || autoSignStarted || autoSignChecking) return
        authRepository.syncCookieFromWebView()
        autoSignChecking = true
        scope.launch {
            /** This when confirms the current WebView page is really past Cloudflare before exiting. */
            when (signRepository.fetchPageInfo()) {
                is YamiboResult.Success -> {
                    autoSignStarted = true
                    autoSignChecking = false
                    navigator.pop()
                    onCfCleared()
                }
                is YamiboResult.Maintenance -> {
                    autoSignChecking = false
                    onMaintenanceObserved()
                    navigator.pop()
                }
                else -> {
                    autoSignChecking = false
                }
            }
        }
    }

    PlatformWebViewScreen(
        initialUrl = YamiboRoute.Sign.build(),
        initialTitle = i18n("每日簽到"),
        useBackIcon = true,
        captureHtml = true,
        onPageFinished = { currentUrl ->
            if (currentUrl.contains("plugin.php?id=zqlj_sign")) {
                maybeStartSemiAutoSign()
            }
        },
        onHtmlAvailable = { _, html ->
            if (isCloudflareChallengeHtml(html)) {
                return@PlatformWebViewScreen
            }
            val pageInfo = signRepository.cacheObservedHtml(html)
            val isResolvedResultPage = isSignResultPageHtml(html)
            if (semiAutomatic && !handledMaintenancePage && isMaintenancePageHtml(html)) {
                handledMaintenancePage = true
                onMaintenanceObserved()
                navigator.pop()
                return@PlatformWebViewScreen
            }
            if (pageInfo != null || isResolvedResultPage) {
                maybeStartSemiAutoSign()
            }
            if (isResolvedResultPage) {
                onResultObserved()
            }
        },
        onLoadError = { _, description ->
            if (semiAutomatic && !handledLoadFailure) {
                handledLoadFailure = true
                onLoadFailed(description.ifBlank { i18n("簽到頁載入失敗") })
                navigator.pop()
            }
        },
    )
}

