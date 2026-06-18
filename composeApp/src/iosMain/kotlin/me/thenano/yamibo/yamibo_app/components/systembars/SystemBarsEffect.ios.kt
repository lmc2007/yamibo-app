package me.thenano.yamibo.yamibo_app.components.systembars

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SystemBarsEffect(
    statusBarColor: Color,
    navigationBarColor: Color,
    priority: Int,
    darkStatusBarIcons: Boolean?,
    darkNavigationBarIcons: Boolean?,
) = Unit

