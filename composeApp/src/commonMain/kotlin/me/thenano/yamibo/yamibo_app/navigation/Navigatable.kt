package me.thenano.yamibo.yamibo_app.navigation

import androidx.compose.runtime.Composable

interface Navigatable {
    val id: Any
    @Composable fun Content()
}