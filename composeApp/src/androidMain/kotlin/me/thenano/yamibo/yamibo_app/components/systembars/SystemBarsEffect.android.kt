package me.thenano.yamibo.yamibo_app.components.systembars

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
    darkStatusBarIcons: Boolean?,
    darkNavigationBarIcons: Boolean?,
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
            darkStatusBarIcons = darkStatusBarIcons,
            darkNavigationBarIcons = darkNavigationBarIcons,
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
        val darkStatusBarIcons: Boolean?,
        val darkNavigationBarIcons: Boolean?,
        val sequence: Long,
    )

    private data class Defaults(
        val statusBarColor: Int,
        val navigationBarColor: Int,
        val lightStatusBars: Boolean,
        val lightNavigationBars: Boolean,
    )

    private val requests = linkedMapOf<Any, Request>()
    private var sequence = 0L
    private var defaults: Defaults? = null

    fun update(
        key: Any,
        activity: Activity,
        statusBarColor: Color,
        navigationBarColor: Color,
        priority: Int,
        darkStatusBarIcons: Boolean?,
        darkNavigationBarIcons: Boolean?,
    ) {
        captureDefaults(activity)
        requests[key] = Request(
            statusBarColor = statusBarColor,
            navigationBarColor = navigationBarColor,
            priority = priority,
            darkStatusBarIcons = darkStatusBarIcons,
            darkNavigationBarIcons = darkNavigationBarIcons,
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
        )
        if (request == null) {
            restoreDefaults(activity)
            return
        }
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = request.statusBarColor.toArgb()
        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = request.navigationBarColor.toArgb()
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = request.darkStatusBarIcons ?: (request.statusBarColor.luminance() > 0.5f)
            isAppearanceLightNavigationBars = request.darkNavigationBarIcons ?: (request.navigationBarColor.luminance() > 0.5f)
        }
    }

    private fun captureDefaults(activity: Activity) {
        if (defaults != null) return
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        @Suppress("DEPRECATION")
        defaults = Defaults(
            statusBarColor = activity.window.statusBarColor,
            navigationBarColor = activity.window.navigationBarColor,
            lightStatusBars = controller.isAppearanceLightStatusBars,
            lightNavigationBars = controller.isAppearanceLightNavigationBars,
        )
    }

    private fun restoreDefaults(activity: Activity) {
        val currentDefaults = defaults ?: return
        @Suppress("DEPRECATION")
        activity.window.statusBarColor = currentDefaults.statusBarColor
        @Suppress("DEPRECATION")
        activity.window.navigationBarColor = currentDefaults.navigationBarColor
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = currentDefaults.lightStatusBars
            isAppearanceLightNavigationBars = currentDefaults.lightNavigationBars
        }
    }
}

