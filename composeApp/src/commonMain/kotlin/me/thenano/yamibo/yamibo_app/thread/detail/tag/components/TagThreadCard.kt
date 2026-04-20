package me.thenano.yamibo.yamibo_app.thread.detail.tag.components

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.dto.model.AttachmentType
import io.github.littlesurvival.dto.model.ThreadSummary
import me.thenano.yamibo.yamibo_app.forum.components.StatBadge
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Thread card for the Tag Detail page */
@Composable
fun TagThreadCard(
    thread: ThreadSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLastRead: Boolean = false,
    readingProgressText: String? = null
) {
    val colors = YamiboTheme.colors

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.creamSurface,
        border = BorderStroke(0.5.dp, colors.brownPrimary.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .let {
                    if (isLastRead) {
                        it.drawBehind {
                            drawRect(
                                color = colors.orangeAccent,
                                size = size.copy(width = 4.dp.toPx())
                            )
                        }
                    } else it
                }
                .padding(14.dp)
        ) {
            // Author + Time (Top)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (thread.author != null) {
                    Text(
                        text = thread.author!!.name,
                        fontSize = 12.sp,
                        color = colors.brownPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                val lastUpdate = thread.lastUpdate
                if (lastUpdate != null) {
                    Text(
                        text = " · ${lastUpdate.text}",
                        fontSize = 12.sp,
                        color = colors.textDark.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Title + Attachment (Middle)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Attachment type icon
                val attachIcon = when (thread.attachmentType) {
                    AttachmentType.Image -> "🖼"
                    AttachmentType.Other -> "📎"
                    else -> null
                }
                if (attachIcon != null) {
                    Text(
                        text = attachIcon,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Poll icon
                if (thread.hasPoll) {
                    Text(
                        text = "📊",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                Text(
                    text = thread.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textDark,
                    modifier = Modifier.weight(1f),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer (Bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View count
                if (thread.viewCount != null) {
                    StatBadge(icon = YamiboIcons.Views, value = "${thread.viewCount}")
                    Spacer(Modifier.width(12.dp))
                }
                // Reply count
                if (thread.replyCount != null) {
                    StatBadge(icon = YamiboIcons.Comment, value = "${thread.replyCount}")
                }
                
                Spacer(Modifier.weight(1f))
                
                // Reading progress text in middle/right
                if (readingProgressText != null) {
                    Text(
                        text = readingProgressText,
                        fontSize = 11.sp,
                        color = colors.orangeAccent,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(12.dp))
                }
                
                // Forum Name (Right)
                val forumName = thread.fid?.let { YamiboForum.toForumName(it) }
                if (forumName != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.orangeAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "#$forumName",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.brownDeep
                        )
                    }
                }
            }
        }
    }
}
