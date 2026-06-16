package me.thenano.yamibo.yamibo_app.systembars

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun SystemBarsEffect(
    statusBarColor: Color,
    navigationBarColor: Color,
    priority: Int,
) {
    val activity = LocalContext.current.findActivity() ?: return
    val key = remember { Any() }
    SideEffect {
        SystemBarRequestRegistry.update(
            key = key,
            activity = activity,
            statusBarColor = statusBarColor,
            navigationBarColor = navigationBarColor,
            priority = priority,
        )
    }
    DisposableEffect(key) {
        onDispose {
            SystemBarRequestRegistry.remove(key, activity)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private object SystemBarRequestRegistry {
    private data class Request(
        val statusBarColor: Color,
        val navigationBarColor: Color,
        val priority: Int,
        val sequence: Long,
    )

    private val requests = linkedMapOf<Any, Request>()
    private var sequence = 0L

    fun update(
        key: Any,
        activity: Activity,
        statusBarColor: Color,
        navigationBarColor: Color,
        priority: Int,
    ) {
        requests[key] = Request(
            statusBarColor = statusBarColor,
            navigationBarColor = navigationBarColor,
            priority = priority,
            sequence = ++sequence,
        )
        apply(activity)
    }

    fun remove(key: Any, activity: Activity) {
        requests.remove(key)
        apply(activity)
    }

    private fun apply(activity: Activity) {
        val request = requests.values.maxWithOrNull(
            compareBy<Request> { it.priority }.thenBy { it.sequence }
        ) ?: return
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = request.statusBarColor.toArgb()
        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = request.navigationBarColor.toArgb()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = request.statusBarColor.luminance() > 0.5f
            isAppearanceLightNavigationBars = request.navigationBarColor.luminance() > 0.5f
        }
    }
}
