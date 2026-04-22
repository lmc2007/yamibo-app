package me.thenano.yamibo.yamibo_app.profile.settings.access

import androidx.compose.runtime.Composable

@Composable
expect fun rememberBackgroundAccessNotificationPermissionRequester(
    onPermissionHandled: () -> Unit,
): (() -> Unit)?

@Composable
expect fun BackgroundAccessResumeRefreshEffect(
    onResume: () -> Unit,
)
