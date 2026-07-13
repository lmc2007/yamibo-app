package me.thenano.yamibo.yamibo_app.components.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.core.cache.CacheStorageUsage
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.util.formatStorageSize

@Composable
fun YamiboStorageUsageOverview(
    title: String,
    usages: List<CacheStorageUsage>,
    modifier: Modifier = Modifier,
    rootPath: String? = null,
) {
    val colors = YamiboTheme.colors
    val usageColors = yamiboStorageUsageColors()
    val visibleUsages = usages.filter { it.bytes > 0L }
    val totalBytes = visibleUsages.sumOf { it.bytes }.coerceAtLeast(1L)

    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.brownDeep,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (!rootPath.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = rootPath,
                fontSize = 12.sp,
                color = colors.textDark.copy(alpha = 0.62f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.brownLight.copy(alpha = 0.32f)),
        ) {
            if (visibleUsages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(colors.brownLight.copy(alpha = 0.32f)),
                )
            } else {
                visibleUsages.forEach { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((item.bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.01f))
                            .background(usageColors[item.key] ?: colors.brownPrimary),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        visibleUsages.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(usageColors[item.key] ?: colors.brownPrimary),
                )
                Text(
                    text = item.label,
                    color = colors.textDark,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                Text(
                    text = formatStorageSize(item.bytes),
                    color = colors.textDark.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                )
            }
        }
        if (visibleUsages.isEmpty()) {
            Text(
                text = i18n("目前沒有可統計的儲存內容"),
                color = colors.textDark.copy(alpha = 0.62f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

private fun yamiboStorageUsageColors(): Map<String, Color> = mapOf(
    "images" to Color(0xFF5C8DD6),
    "pages" to Color(0xFFB66D32),
    "userspace" to Color(0xFF7D63B8),
    "backup" to Color(0xFF4A9A76),
    "downloads" to Color(0xFFB58A35),
    "thread_downloads" to Color(0xFFB66D32),
    "tag_manga_downloads" to Color(0xFF4A9A76),
    "other" to Color(0xFF8A7D70),
)
