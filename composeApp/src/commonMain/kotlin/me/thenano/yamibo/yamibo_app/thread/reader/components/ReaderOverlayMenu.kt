package me.thenano.yamibo.yamibo_app.thread.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadTopBar

/** Overlay top bar + snackbar host for the reader screen */
@Composable
internal fun ReaderOverlayMenu(
    visible: Boolean,
    title: String,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors

    // Snackbar host
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 32.dp)
    )

    // Animated top bar
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = colors.brownDeep,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ThreadTopBar(
                title = title,
                onBack = onBack,
                windowInsets = WindowInsets.statusBars,
                actions = {
                    IconButton(
                        onClick = onCatalog,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "☰",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    }
                }
            )
        }
    }
}
