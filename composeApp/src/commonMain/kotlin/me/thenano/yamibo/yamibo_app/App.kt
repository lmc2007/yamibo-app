package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import me.thenano.yamibo.yamibo_app.home.HomePageScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreenContent() {
    HomePageScreen()
}

@Composable
@Preview
fun App() {
    val navigator = LocalNavigator.current
    val holder = rememberSaveableStateHolder()
    navigator.stateHolder = holder

    val navigatable = navigator.currentScreen

    holder.SaveableStateProvider(navigatable.id) { navigatable.Content() }
}
