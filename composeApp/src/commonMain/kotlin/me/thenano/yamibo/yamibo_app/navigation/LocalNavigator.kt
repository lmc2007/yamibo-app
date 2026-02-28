package me.thenano.yamibo.yamibo_app.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import me.thenano.yamibo.yamibo_app.IMainScreen

annotation class ScreenKey(val name: String)
class ComposableNavigator(val start: Navigatable = IMainScreen()) {
    val stack = mutableStateListOf<Navigatable>().apply { add(start) }
    lateinit var stateHolder: SaveableStateHolder
    val currentScreen: Navigatable
        get() = stack.last()
    fun navigate(navigatable: Navigatable) {
        stack.add(navigatable)
    }

    fun replace(navigatable: Navigatable) {
        stack[stack.lastIndex] = navigatable
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popToRoot() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun canGoBack(): Boolean = stack.size > 1
}
val LocalNavigator = compositionLocalOf<ComposableNavigator> {
    error("LocalNavigator not provided")
}