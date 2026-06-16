package me.thenano.yamibo.yamibo_app.systembars

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Applies platform system bar colors and automatically chooses dark or light
 * status/navigation bar icons from the given background color luminance.
 */
@Composable
expect fun SystemBarsEffect(
    statusBarColor: Color,
    navigationBarColor: Color,
    priority: Int = 0,
)
