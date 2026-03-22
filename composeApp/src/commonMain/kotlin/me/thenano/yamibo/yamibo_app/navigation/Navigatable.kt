package me.thenano.yamibo.yamibo_app.navigation

import androidx.compose.runtime.Composable

interface Navigatable {
    val id: String
    fun buildId(vararg param: Any?) = "${this::class.simpleName}_${param.joinToString("_")}"

    @Composable
    fun Content()
}