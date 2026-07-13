package me.thenano.yamibo.yamibo_app.components.theme

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun YamiboSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                containerColor = colors.brownDeep,
                contentColor = Color.White,
                actionColor = colors.orangeAccent,
                dismissActionContentColor = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                snackbarData = data,
            )
        }
    )
}
