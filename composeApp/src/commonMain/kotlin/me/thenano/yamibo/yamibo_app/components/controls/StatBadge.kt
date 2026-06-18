package me.thenano.yamibo.yamibo_app.components.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/**
 * Compact icon + value badge for counts.
 *
 * Use for view/reply/read counts in thread cards, blog readers, and other
 * metadata rows. This keeps count presentation icon-based instead of text-only.
 *
 * @param icon Yamibo icon for the metric.
 * @param value Preformatted metric value.
 */
@Composable
fun YamiboStatBadge(icon: ImageVector, value: String) {
    val colors = YamiboTheme.colors
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.brownPrimary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, tint = colors.brownPrimary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            Text(value, color = colors.brownPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}
