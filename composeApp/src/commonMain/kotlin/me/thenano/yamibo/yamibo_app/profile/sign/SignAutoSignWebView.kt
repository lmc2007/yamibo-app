package me.thenano.yamibo.yamibo_app.profile.sign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.littlesurvival.YamiboRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.Logger
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.SignStatusChangedEvent
import me.thenano.yamibo.yamibo_app.webview.PlatformBackgroundWebViewContent

@Composable
internal fun SignAutoSignWebView(
    attemptKey: String,
    allowRepair: Boolean,
    isAttemptCurrent: () -> Boolean,
    onCompleted: () -> Unit,
) {
    val signRepository = LocalSignRepository.current
    val scope = rememberCoroutineScope()
    var actionStarted by remember(attemptKey) { mutableStateOf(false) }
    val session = remember(attemptKey) { SilentSignSession() }

    fun finish() {
        if (!session.active) return
        session.active = false
        onCompleted()
    }

    DisposableEffect(session) {
        onDispose { session.active = false }
    }

    LaunchedEffect(session) {
        delay(60_000L)
        finish()
    }

    fun observePage(html: String) {
        if (!session.active || actionStarted) return
        if (!isAttemptCurrent()) {
            finish()
            return
        }
        if (isCloudflareChallengeHtml(html)) return
        if (isMaintenancePageHtml(html)) {
            finish()
            return
        }
        val pageInfo = signRepository.cacheObservedHtml(html) ?: return
        actionStarted = true
        scope.launch {
            try {
                if (pageInfo.hasSignedToday) {
                    AppEventBus.emit(SignStatusChangedEvent)
                } else if (isAttemptCurrent()) {
                    val result = signRepository.runAutoSignSilently(allowRepair, pageInfo)
                    if (session.active && isAttemptCurrent() && shouldEmitSignStatusChanged(result)) {
                        AppEventBus.emit(SignStatusChangedEvent)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Logger.w("AutomaticSignIn", "Silent sign-in failed", error)
            } finally {
                finish()
            }
        }
    }

    PlatformBackgroundWebViewContent(
        url = YamiboRoute.Sign.build(),
        captureHtml = true,
        onPageFinished = {},
        onHtmlAvailable = { _, html -> observePage(html) },
        onLoadError = { _, _ -> finish() },
    )
}

private class SilentSignSession {
    var active = true
}
