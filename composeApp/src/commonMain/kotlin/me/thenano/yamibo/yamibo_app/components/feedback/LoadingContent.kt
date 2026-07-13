package me.thenano.yamibo.yamibo_app.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/**
 * Centered loading state for Yamibo screens.
 *
 * Use this in page-level state switches when data is being fetched, including
 * ForumPage, UserSpace, BlogReader, PrivateMessage, search sheets, and history.
 * It intentionally only renders a spinner so feature pages can decide their own
 * skeletons when they need richer placeholders.
 */
@Composable
fun YamiboLoadingContent(modifier: Modifier = Modifier) {
    val colors = YamiboTheme.colors
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.brownDeep)
    }
}
