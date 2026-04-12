package me.thenano.yamibo.yamibo_app.thread.detail.tag.components

import YamiboIcons
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
import me.thenano.yamibo.yamibo_app.favorite.FavoriteActionButton
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.book

@Composable
fun TagDetailHeaderCard(
    tagName: String,
    coverUrl: String?,
    isMangaMode: Boolean,
    onMangaModeChange: (Boolean) -> Unit,
    hasReadingHistory: Boolean,
    readingProgressText: String?,
    onContinueRead: () -> Unit,
    isFavorited: Boolean,
    onFavorite: () -> Unit,
    onFavoriteLongPress: (() -> Unit)? = null,
    onShare: () -> Unit
) {
    val colors = YamiboTheme.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
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
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#$tagName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.brownDeep.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "#標籤漫畫",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = colors.brownDeep
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "漫畫模式",
                            fontSize = 13.sp,
                            color = colors.brownPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isMangaMode,
                            onCheckedChange = onMangaModeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.brownDeep,
                                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.1f)
                ) {
                    FavoriteActionButton(
                        onClick = onFavorite,
                        onLongClick = onFavoriteLongPress,
                        tint = colors.brownDeep,
                        iconSize = 22,
                        modifier = Modifier.size(42.dp),
                        filled = isFavorited
                    )
                }

                Surface(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.1f)
                ) {
                    Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = YamiboIcons.Share,
                            contentDescription = "分享",
                            modifier = Modifier.size(22.dp),
                            tint = colors.brownDeep
                        )
                    }
                }

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
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        Text(text = "▶", color = Color.White, fontSize = 16.sp)
                    }
                }
            }

            Text(
                text = "星星按鈕可直接收藏，長按可指定小集合",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 10.sp,
                color = colors.brownPrimary.copy(alpha = 0.45f)
            )
        }
    }
}
