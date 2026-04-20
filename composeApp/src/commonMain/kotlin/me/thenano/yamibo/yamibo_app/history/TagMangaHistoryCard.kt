package me.thenano.yamibo.yamibo_app.history

import YamiboIcons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import me.thenano.yamibo.yamibo_app.favorite.FavoriteActionButton
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.TagMangaReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.book

@Composable
fun TagMangaHistoryCard(
    history: TagMangaReadingHistory,
    timeLabel: String,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    isFavorited: Boolean = false,
    onClick: () -> Unit,
    onCoverClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onFavoriteLongPress: (() -> Unit)? = null,
) {
    val colors = YamiboTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val timingSummary = "最近閱讀 $timeLabel"
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .scale(scale)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = colors.brownDeep,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(92.dp)
                    .aspectRatio(0.72f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCoverClick
                    ),
                colors = CardDefaults.cardColors(containerColor = colors.brownLight.copy(alpha = 0.2f))
            ) {
                if (!history.coverUrl.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = rememberImageRequest(url = history.coverUrl!!),
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.book),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colors.brownPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = history.tagName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = history.threadTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textDark.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val progress = "第 ${history.threadImagePageIndex + 1} / ${history.threadImageTotalPages} 頁"
                Text(
                    text = progress,
                    fontSize = 12.sp,
                    color = colors.textDark.copy(alpha = 0.56f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = timingSummary,
                    fontSize = 12.sp,
                    color = colors.textDark.copy(alpha = 0.48f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.size(4.dp))

            if (!isSelectMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FavoriteActionButton(
                        onClick = onFavorite,
                        onLongClick = onFavoriteLongPress,
                        modifier = Modifier.size(32.dp),
                        tint = colors.brownPrimary.copy(alpha = 0.75f),
                        iconSize = 16,
                        filled = isFavorited
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = YamiboIcons.Trashcan,
                            contentDescription = "刪除",
                            modifier = Modifier.size(16.dp),
                            tint = colors.textDark.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
