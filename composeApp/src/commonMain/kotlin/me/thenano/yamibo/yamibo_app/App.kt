package me.thenano.yamibo.yamibo_app

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import me.thenano.yamibo.yamibo_app.home.HomePageScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.NavAction
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun HomeScreenContent() {
    HomePageScreen()
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.35)
                    .build()
            }
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }

    val navigator = LocalNavigator.current
    val holder = rememberSaveableStateHolder()
    navigator.stateHolder = holder

    val stack = navigator.stack
    val poppingIdx by navigator.poppingIndex
    val duration = 250

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = YamiboTheme.colors.creamBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            stack.forEachIndexed { index, navigatable ->
                val isPopping = index == poppingIdx
                val isTop = index == stack.lastIndex
                val isNewPush = navigator.lastAction == NavAction.Push && isTop && !isPopping

                key(navigatable.id) {
                    // New push screens start invisible (false→true), others start visible
                    val visibleState = remember {
                        MutableTransitionState(!isNewPush)
                    }

                    // Drive animation: pop = true→false, otherwise stay/become true
                    if (isPopping) {
                        visibleState.targetState = false
                    } else {
                        visibleState.targetState = true
                    }

                    holder.SaveableStateProvider(navigatable.id) {
                        AnimatedVisibility(
                            visibleState = visibleState,
                            enter = slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(duration)
                            ) + fadeIn(animationSpec = tween(duration)),
                            exit = slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(duration)
                            ) + fadeOut(animationSpec = tween(duration)),
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(index.toFloat())
                        ) {
                            navigatable.Content()
                        }
                    }

                    // When exit animation finished, actually remove from stack
                    if (isPopping && visibleState.isIdle && !visibleState.currentState) {
                        LaunchedEffect(Unit) {
                            navigator.completePop()
                        }
                    }
                }
            }
        }
    }
}
