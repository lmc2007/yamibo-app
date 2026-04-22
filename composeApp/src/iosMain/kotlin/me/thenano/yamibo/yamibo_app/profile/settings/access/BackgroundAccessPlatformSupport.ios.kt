package me.thenano.yamibo.yamibo_app.profile.settings.access

import androidx.compose.runtime.Composable

@Composable
actual fun rememberBackgroundAccessNotificationPermissionRequester(
    onPermissionHandled: () -> Unit,
): (() -> Unit)? = null

@Composable
actual fun BackgroundAccessResumeRefreshEffect(
    onResume: () -> Unit,
) = Unit
