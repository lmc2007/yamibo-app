package me.thenano.yamibo.yamibo_app.thread.detail.tag.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.book

/** Tag Detail Header Card — cover image + tag info + actions (similar to novel ThreadHeader) */
@Composable
fun TagDetailHeaderCard(
    tagName: String,
    coverUrl: String?,
    isMangaMode: Boolean,
    onMangaModeChange: (Boolean) -> Unit,
    hasReadingHistory: Boolean,
    readingProgressText: String?,
    onContinueRead: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    val colors = YamiboTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Cover image
                Card(
                    modifier = Modifier.size(width = 100.dp, height = 130.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.brownPrimary.copy(alpha = 0.1f)
                    )
                ) {
                    if (coverUrl != null) {
                        SubcomposeAsyncImage(
                            model = rememberImageRequest(url = coverUrl),
                            contentDescription = "cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.book),
                                        contentDescription = "loading",
                                        modifier = Modifier.size(32.dp),
                                        tint = colors.brownPrimary.copy(alpha = 0.2f)
                                    )
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.book),
                                        contentDescription = "error",
                                        modifier = Modifier.size(48.dp),
                                        tint = colors.brownPrimary.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.book),
                                contentDescription = "cover",
                                modifier = Modifier.size(48.dp),
                                tint = colors.brownPrimary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Info column
                Column(modifier = Modifier.weight(1f)) {
                    // Tag name
                    Text(
                        text = "🏷️ $tagName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )
                    Spacer(Modifier.height(8.dp))

                    // Tag label
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.brownDeep.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "#標籤",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = colors.brownDeep
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Manga mode toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "漫畫閱讀模式",
                            fontSize = 13.sp,
                            color = colors.brownPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isMangaMode,
                            onCheckedChange = onMangaModeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.brownDeep,
                                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Action row: [fav] [share] [continue / start reading]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite button
                Surface(
                    onClick = onFavorite,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.1f)
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = YamiboIcons.StarOutline,
                            contentDescription = "收藏",
                            modifier = Modifier.size(22.dp),
                            tint = colors.brownDeep
                        )
                    }
                }

                // Share button
                Surface(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.1f)
                ) {
                    Box(
                        modifier = Modifier.padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = YamiboIcons.Share,
                            contentDescription = "分享",
                            modifier = Modifier.size(22.dp),
                            tint = colors.brownDeep
                        )
                    }
                }

                // Continue / Start reading button
                Surface(
                    onClick = onContinueRead,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownDeep
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasReadingHistory) "繼續閱讀" else "開始閱讀",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (readingProgressText != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = readingProgressText,
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.StartEllipsis
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        // Play icon
                        Text(text = "▶", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}