package me.thenano.yamibo.yamibo_app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.thenano.yamibo.yamibo_app.repository.settings.core.SettingItem

/** Compose-friendly accessor: just call `setting.state()` to get the current reactive value. */
@Composable
fun <T> SettingItem<T>.state(): T {
    val current by stateFlow.collectAsState()
    return current
}
