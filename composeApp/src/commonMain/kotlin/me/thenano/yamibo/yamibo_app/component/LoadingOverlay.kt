package me.thenano.yamibo.yamibo_app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

@Composable
fun LoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    blockInput: Boolean = true,
    content: @Composable () -> Unit = {
        CircularProgressIndicator()
    }
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .let {
                if (blockInput) it.pointerInput(Unit) {} else it
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
