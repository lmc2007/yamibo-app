package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.PostComment
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun CommentRenderer(comments: List<PostComment>, modifier: Modifier = Modifier) {
    val colors = YamiboTheme.colors
    if (comments.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💬 點評",
                    color = colors.brownPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            comments.forEachIndexed { index, comment ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    // Avatar
                    val avatarUrl = comment.user.avatarUrl ?: ""
                    if (avatarUrl.isNotEmpty()) {
                        KamelImage(
                            resource = asyncPainterResource(data = avatarUrl),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.brownPrimary.copy(alpha = 0.2f))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = comment.user.name,
                                color = colors.brownPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = comment.timeText,
                                color = colors.textDark.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = comment.message,
                            color = colors.textDark,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                if (index < comments.size - 1) {
                    Divider(color = colors.brownLight.copy(alpha = 0.3f), modifier = Modifier.padding(start = 48.dp))
                }
            }
        }
    }
}
