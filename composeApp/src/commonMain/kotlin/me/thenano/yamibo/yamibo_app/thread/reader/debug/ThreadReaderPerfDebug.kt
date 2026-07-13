package me.thenano.yamibo.yamibo_app.thread.reader.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember

internal const val THREAD_READER_PERF_DEBUG = false

@Composable
internal fun DebugRecomposeProbe(tag: String, key: String) {
    if (!THREAD_READER_PERF_DEBUG) return

    val count = remember(tag, key) { mutableIntStateOf(0) }
    DisposableEffect(tag, key) {
        println("TR_PROF|enter|$tag|$key")
        onDispose {
            println("TR_PROF|dispose|$tag|$key|count=${count.intValue}")
        }
    }
    SideEffect {
        count.intValue += 1
        val current = count.intValue
        if (current <= 3 || current % 10 == 0) {
            println("TR_PROF|recompose|$tag|$key|count=$current")
        }
    }
}

internal fun debugPerfLog(message: String) {
    if (THREAD_READER_PERF_DEBUG) {
        println("TR_PROF|event|$message")
    }
}
