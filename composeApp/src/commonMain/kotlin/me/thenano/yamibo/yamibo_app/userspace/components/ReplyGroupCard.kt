package me.thenano.yamibo.yamibo_app.userspace.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.ReplyItem
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
internal fun ReplyGroupCard(
    item: ReplyItem,
    onThreadClick: (ThreadSummary) -> Unit,
    onQuoteClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.creamSurface,
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.35f)),
        onClick = {
            onThreadClick(
                ThreadSummary(
                    tid = item.tId,
                    title = item.title,
                    hasPoll = false,
                    url = item.url,
                )
            )
        },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.title, color = colors.textStrong, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            item.posts.forEach { post ->
                Surface(
                    onClick = onQuoteClick,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = colors.creamBackground,
                ) {
                    Text(
                        text = post.quote,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = colors.textDark,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
