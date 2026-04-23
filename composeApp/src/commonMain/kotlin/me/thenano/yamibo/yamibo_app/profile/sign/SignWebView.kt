package me.thenano.yamibo.yamibo_app.profile.sign

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
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.webview.PlatformWebViewScreen

internal class ISignWebView(
    private val semiAutomatic: Boolean,
    private val allowRepair: Boolean = false,
    private val onSemiAutoCompleted: (YamiboResult<SignRepository.ActionResult>) -> Unit = {},
    private val onResultObserved: () -> Unit = {},
    private val onMaintenanceObserved: () -> Unit = {},
) : Navigatable {
    override val id = buildId("sign-webview", semiAutomatic)

    @Composable
    override fun Content() {
        SignWebViewScreen(
            semiAutomatic = semiAutomatic,
            allowRepair = allowRepair,
            onSemiAutoCompleted = onSemiAutoCompleted,
            onResultObserved = onResultObserved,
            onMaintenanceObserved = onMaintenanceObserved,
        )
    }
}

@Composable
private fun SignWebViewScreen(
    semiAutomatic: Boolean,
    allowRepair: Boolean,
    onSemiAutoCompleted: (YamiboResult<SignRepository.ActionResult>) -> Unit,
    onResultObserved: () -> Unit,
    onMaintenanceObserved: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val authRepository = LocalAuthRepository.current
    val signRepository = LocalSignRepository.current
    val scope = rememberCoroutineScope()
    var handledMaintenancePage by remember(semiAutomatic) { mutableStateOf(false) }
    var autoSignStarted by remember(semiAutomatic) { mutableStateOf(false) }
    var autoSignChecking by remember(semiAutomatic) { mutableStateOf(false) }

    fun maybeStartSemiAutoSign() {
        if (!semiAutomatic || autoSignStarted || autoSignChecking) return
        authRepository.syncCookieFromWebView()
        autoSignChecking = true
        scope.launch {
            /** This when confirms the current WebView page is really past Cloudflare before auto-sign starts. */
            when (val pageInfoResult = signRepository.fetchPageInfo()) {
                is YamiboResult.Success -> {
                    autoSignStarted = true
                    autoSignChecking = false
                    /** This when feeds the semi-automatic WebView flow back into the caller callbacks/navigation. */
                    when (val result = signRepository.runAutoSign(allowRepair)) {
                        is YamiboResult.Success -> {
                            onSemiAutoCompleted(result)
                            navigator.pop()
                        }
                        is YamiboResult.Maintenance -> {
                            onMaintenanceObserved()
                            navigator.pop()
                        }
                        else -> {
                            onSemiAutoCompleted(result)
                            navigator.pop()
                        }
                    }
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
        initialTitle = "每日簽到",
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
    )
}
