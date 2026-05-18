package me.thenano.yamibo.yamibo_app.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

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

/**
 * Centered empty-list message for Yamibo list pages.
 *
 * Use this for data-loaded-but-empty states such as no messages, no notices,
 * no comments, no favorite items, or no user-space posts. Prefer a concrete
 * domain message (appString(Res.string.ui_no_comments_found)) instead of a generic placeholder.
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

/**
 * Standard page-level error card with a retry affordance.
 *
 * Use this when a full page cannot load and the user should be able to retry
 * the same fetch. For inline failures or per-card failures, prefer a smaller
 * feature-local component.
 *
 * @param message Error text to show under the title.
 * @param onRetry Called when the retry button is tapped.
 * @param modifier Root modifier, usually `Modifier.fillMaxSize()`.
 */
@Composable
fun YamiboErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(appString(Res.string.ui_loading_failed), color = colors.brownDeep, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                Text(message, color = colors.brownPrimary.copy(alpha = 0.75f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Surface(onClick = onRetry, shape = RoundedCornerShape(50), color = colors.brownDeep) {
                    Text(appString(Res.string.ui_try_again), modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp), color = Color.White)
                }
            }
        }
    }
}

