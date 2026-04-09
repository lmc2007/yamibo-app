package me.thenano.yamibo.yamibo_app.history

import YamiboIcons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import me.thenano.yamibo.yamibo_app.Logger
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Single history entry card */
@Composable
fun ReadHistoryCard(
    history: ThreadReadingHistory,
    timeLabel: String,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onCoverClick: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
) {
    val colors = YamiboTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            /** Thread cover */
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(52.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCoverClick
                    ),
                colors = CardDefaults.cardColors(containerColor = colors.brownLight.copy(alpha = 0.2f))
            ) {
                Logger.i("threadCover", "${history.threadName} : ${history.threadCover.toString()}")
                if (!history.threadCover.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = rememberImageRequest(url = history.threadCover!!),
                        contentDescription = "thread cover",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = YamiboIcons.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = colors.brownPrimary.copy(alpha = 0.5f)
                                )
                            }
                        },
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = colors.brownPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 1.5.dp
                                )
                            }
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = YamiboIcons.History,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colors.brownPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            /** Title + details */
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = history.threadName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textDark,
                )
                Spacer(Modifier.height(4.dp))
                
                /** Reading Progress (post title) */
                val progress = if (history.postTitle.isNotEmpty()) {
                    "P.${history.page} ${history.postTitle}"
                } else {
                    "第 ${history.page} 頁"
                }
                Text(
                    text = progress,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textDark.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))

                /** Forum & Time */
                val metaInfo = buildString {
                    if (history.forumName != null) {
                        append("#${history.forumName}  ·  ")
                    }
                    append(timeLabel)
                }
                Text(
                    text = metaInfo,
                    fontSize = 12.sp,
                    color = colors.textDark.copy(alpha = 0.5f),
                )
            }

            Spacer(Modifier.width(4.dp))

            /** Action buttons — star + trashcan */
            if (!isSelectMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    /** Star (favorite) button */
                    IconButton(
                        onClick = onCoverClick,
                        modifier = Modifier.size(0.dp)
                    ) {}
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = YamiboIcons.StarOutline,
                            contentDescription = "收藏",
                            modifier = Modifier.size(16.dp),
                            tint = colors.brownPrimary.copy(alpha = 0.6f)
                        )
                    }

                    /** Trashcan (delete) button */
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

