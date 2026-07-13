package me.thenano.yamibo.yamibo_app.components.systembars

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun SystemBarsEffect(
    statusBarColor: Color,
    navigationBarColor: Color,
    priority: Int = 0,
    darkStatusBarIcons: Boolean? = null,
    darkNavigationBarIcons: Boolean? = null,
)
