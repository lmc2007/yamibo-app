package me.thenano.yamibo.yamibo_app.forum.components

import YamiboIcons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import io.github.littlesurvival.dto.model.ThreadSummary
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Thread Card */
@Composable
fun ThreadCard(thread: ThreadSummary, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by
    animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150)
    )

    Card(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 5.dp)
                .scale(scale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
        shape = RoundedCornerShape(16.dp),
        elevation =
            CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            /** author and time row */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                thread.author?.let { user ->
                    val avatarUrl = user.avatarUrl
                    if (!avatarUrl.isNullOrEmpty()) {
                        SubcomposeAsyncImage(
                            model = rememberImageRequest(url = avatarUrl),
                            contentDescription = "Avatar",
                            modifier = Modifier.size(28.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = {
                                Icon(imageVector = YamiboIcons.PersonFill, contentDescription = null, modifier = Modifier.size(28.dp), tint = colors.textDark.copy(alpha = 0.5f))
                            },
                            loading = {
                                CircularProgressIndicator(
                                    color = colors.brownPrimary,
                                    modifier = Modifier.padding(6.dp),
                                    strokeWidth = 1.5.dp
                                )
                            }
                        )
                    } else {
                        Icon(imageVector = YamiboIcons.PersonFill, contentDescription = null, modifier = Modifier.size(28.dp), tint = colors.textDark.copy(alpha = 0.5f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = user.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.brownPrimary
                    )
                }
                thread.lastUpdateText?.let { time ->
                    Spacer(Modifier.width(8.dp))
                    Text(text = time, fontSize = 11.sp, color = colors.brownLight)
                }
            }

            Spacer(Modifier.height(6.dp))

            /** title */
            Text(
                text = thread.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textDark,
                lineHeight = 22.sp
            )

            /** description preview */
            if (!thread.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = thread.description!!,
                    fontSize = 13.sp,
                    color = colors.brownPrimary.copy(alpha = 0.65f),
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            /** footer: stats and tag */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                thread.viewCount?.let { views ->
                    StatBadge(icon = YamiboIcons.Views, value = "$views")
                    Spacer(Modifier.width(12.dp))
                }
                thread.replyCount?.let { replies ->
                    StatBadge(icon = YamiboIcons.Comment, value = "$replies")
                }
                Spacer(Modifier.weight(1f))
                thread.tag?.let { tag ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.orangeAccent.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "#$tag",
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

/** Stat badge (views/replies) */
@Composable
fun StatBadge(icon: ImageVector, value: String) {
    val colors = YamiboTheme.colors
    Surface(shape = RoundedCornerShape(12.dp), color = colors.orangeAccent.copy(alpha = 0.15f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.brownDeep,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                color = colors.brownDeep,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
