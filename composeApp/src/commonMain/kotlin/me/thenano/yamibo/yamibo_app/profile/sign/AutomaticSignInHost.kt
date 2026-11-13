package me.thenano.yamibo.yamibo_app.profile.sign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalSignReminderScheduler
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.LoginSuccessEvent
import me.thenano.yamibo.yamibo_app.event.events.LogoutSuccessEvent
import me.thenano.yamibo.yamibo_app.event.events.SignStatusChangedEvent
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKey
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

@Composable
internal fun AutomaticSignInHost() {
    val authRepository = LocalAuthRepository.current
    val signRepository = LocalSignRepository.current
    val settings = LocalAppSettingsRepository.current
    val reminderScheduler = LocalSignReminderScheduler.current
    val mode = settings.signInMode.state()
    val allowRepair = settings.signInAllowRepair.state()
    val lifecycleOwner = LocalLifecycleOwner.current
    val policy = remember(authRepository, signRepository) { AutomaticSignInPolicy() }
    var isForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var checkSignal by remember { mutableStateOf(0) }
    var activeKey by remember { mutableStateOf<String?>(null) }

    fun cancelAttempt() {
        activeKey?.let(policy::finish)
        activeKey = null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isForeground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(authRepository, signRepository) {
        AppEventBus.events.collect { event ->
            when (event) {
                LoginSuccessEvent, LogoutSuccessEvent -> {
                    cancelAttempt()
                    policy.reset()
                    checkSignal += 1
                }
                SignStatusChangedEvent -> checkSignal += 1
            }
        }
    }

    // Also catches midnight during long reading sessions, without running while suspended.
    LaunchedEffect(isForeground, mode) {
        if (isForeground && mode == SignInMode.SEMI_AUTOMATIC) {
            while (true) {
                checkSignal += 1
                delay(60_000L)
            }
        }
    }

    LaunchedEffect(isForeground, mode, checkSignal) {
        val uid = authRepository.currentUser()?.uid?.value
        val attemptKey = uid?.let { "$it:${currentLocalDateKey()}" }
        val enabled = isForeground && mode == SignInMode.SEMI_AUTOMATIC
        if (!enabled || attemptKey != activeKey) cancelAttempt()
        if (!enabled || attemptKey == null || activeKey != null) return@LaunchedEffect
        val signedToday = signRepository.getKnownSignedToday()
        if (signedToday == true) reminderScheduler.dismissActiveReminder()
        if (policy.tryStart(attemptKey, enabled, signedToday, currentTimeMillis())) {
            activeKey = attemptKey
        }
    }

    if (isForeground && mode == SignInMode.SEMI_AUTOMATIC) {
        activeKey?.let { attemptKey ->
            key(attemptKey) {
                SignAutoSignWebView(
                    attemptKey = attemptKey,
                    allowRepair = allowRepair,
                    isAttemptCurrent = {
                        val uid = authRepository.currentUser()?.uid?.value
                        uid != null && "$uid:${currentLocalDateKey()}" == attemptKey &&
                            settings.signInMode.getValue() == SignInMode.SEMI_AUTOMATIC
                    },
                    onCompleted = {
                        policy.finish(attemptKey)
                        if (activeKey == attemptKey) activeKey = null
                    },
                )
            }
        }
    }
}
