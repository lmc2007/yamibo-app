package me.thenano.yamibo.yamibo_app.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import me.thenano.yamibo.yamibo_app.IMainScreen

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

    /** Index of the screen currently animating out (-1 = none) */
    val poppingIndex = mutableIntStateOf(-1)

    val currentScreen: Navigatable
        get() = stack.last()

    fun navigate(navigatable: Navigatable) {
        // Prevent pushing the exact same screen twice consecutively (avoids common duplicate key crashes)
        if (stack.isNotEmpty() && stack.last().id == navigatable.id) return
        
        lastAction = NavAction.Push
        stack.add(navigatable)
    }

    fun replace(navigatable: Navigatable) {
        lastAction = NavAction.Replace
        stack[stack.lastIndex] = navigatable
    }

    fun dispatchBack(): Boolean {
        for (handler in backHandlers.reversed()) {
            if (handler()) return true
        }
        return pop()
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        if (poppingIndex.intValue >= 0) return false // already popping
        lastAction = NavAction.Pop
        poppingIndex.intValue = stack.lastIndex
        return true
    }

    /** Called when pop exit animation completes — actually remove the screen */
    fun completePop() {
        val idx = poppingIndex.intValue
        if (idx in stack.indices) {
            stack.removeAt(idx)
        }
        poppingIndex.intValue = -1
    }

    fun popToRoot() {
        if (stack.size > 1) lastAction = NavAction.Pop
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    @Deprecated("There's no longer a need for this.")
    fun canGoBack(): Boolean = stack.size > 1
}

val LocalNavigator =
    compositionLocalOf<ComposableNavigator> { error("LocalNavigator not provided") }
