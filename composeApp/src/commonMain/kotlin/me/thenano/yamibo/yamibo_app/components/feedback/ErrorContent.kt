package me.thenano.yamibo.yamibo_app.components.feedback

import me.thenano.yamibo.yamibo_app.i18n.i18n

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

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
                Text(i18n("載入失敗"), color = colors.textStrong, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                Text(message, color = colors.brownPrimary.copy(alpha = 0.75f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Surface(onClick = onRetry, shape = RoundedCornerShape(50), color = colors.brownDeep) {
                    Text(i18n("重試"), modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp), color = colors.textOnDeepHigh)
                }
            }
        }
    }
}
