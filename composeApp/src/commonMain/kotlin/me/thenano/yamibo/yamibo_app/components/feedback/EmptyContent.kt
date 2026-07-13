package me.thenano.yamibo.yamibo_app.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/**
 * Centered empty-list message for Yamibo list pages.
 *
 * Use this for data-loaded-but-empty states such as no messages, no notices,
 * no comments, no favorite items, or no user-space posts. Prefer a concrete
 * domain message instead of a generic placeholder.
 *
 * @param message User-facing empty state text.
 * @param modifier Parent modifier, normally `Modifier.fillMaxWidth()`.
 */
@Composable
fun YamiboEmptyContent(message: String, modifier: Modifier = Modifier) {
    val colors = YamiboTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = colors.brownPrimary.copy(alpha = 0.65f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}
