package me.thenano.yamibo.yamibo_app.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import me.thenano.yamibo.yamibo_app.IMainScreen

annotation class ScreenKey(val name: String)

enum class NavAction {
    Push,
    Pop,
    Replace
}

class ComposableNavigator(start: Navigatable = IMainScreen()) {
    val stack = mutableStateListOf(start)
    var lastAction = NavAction.Push
    lateinit var stateHolder: SaveableStateHolder
    val backHandlers = mutableListOf<() -> Boolean>()

    val currentScreen: Navigatable
        get() = stack.last()

    fun navigate(navigatable: Navigatable) {
        lastAction = NavAction.Push
        stack.add(navigatable)
    }

    fun replace(navigatable: Navigatable) {
        lastAction = NavAction.Replace
        stack[stack.lastIndex] = navigatable
    }

    fun pop(): Boolean {
        for (handler in backHandlers.reversed()) {
            if (handler()) return true
        }
        if (stack.size <= 1) return false
        lastAction = NavAction.Pop
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popToRoot() {
        if (stack.size > 1) lastAction = NavAction.Pop
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun canGoBack(): Boolean = stack.size > 1
}

val LocalNavigator =
    compositionLocalOf<ComposableNavigator> { error("LocalNavigator not provided") }
