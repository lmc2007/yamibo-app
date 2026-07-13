package me.thenano.yamibo.yamibo_app.navigation

import androidx.compose.runtime.Composable
import me.thenano.yamibo.yamibo_app.IMainScreen
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposableNavigatorTest {
    @Test
    fun navigateMovesExistingScreenIdToTopWithoutDuplicatingStackEntry() {
        val navigator = ComposableNavigator(start = IMainScreen())

        navigator.navigate(TestScreen("reader"))
        navigator.navigate(TestScreen("detail"))
        navigator.navigate(TestScreen("reader"))

        assertEquals(listOf(IMainScreen().id, "detail", "reader"), navigator.stack.map { it.id })
    }

    @Test
    fun replaceRemovesExistingScreenIdBeforeReplacingCurrentScreen() {
        val navigator = ComposableNavigator(start = IMainScreen())

        navigator.navigate(TestScreen("reader"))
        navigator.navigate(TestScreen("in_app_link_resolver"))
        navigator.replace(TestScreen("reader"))

        assertEquals(listOf(IMainScreen().id, "reader"), navigator.stack.map { it.id })
    }
}

private class TestScreen(
    override val id: String,
) : Navigatable {
    @Composable
    override fun Content() = Unit
}
