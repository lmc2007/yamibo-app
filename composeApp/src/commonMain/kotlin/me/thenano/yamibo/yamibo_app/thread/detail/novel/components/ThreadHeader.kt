package me.thenano.yamibo.yamibo_app.thread.detail.novel.components

import YamiboIcons
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.dto.page.ThreadPage
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.book

/** Thread header — cover + info + actions */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ThreadHeader(
    threadPage: ThreadPage,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onContinueRead: () -> Unit = {},
    readingProgressText: String? = null,
    onCopy: (String) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val colors = YamiboTheme.colors
    val thread = threadPage.thread
    val firstPost = threadPage.posts.firstOrNull()
    
    val coverUrl = remember(firstPost) {
        val attachedImage = firstPost?.images?.firstOrNull()?.url ?: return@remember null

        if (attachedImage.contains("none.gif") || attachedImage.contains("smiley/") || attachedImage.contains("face")) return@remember null
        if (attachedImage.startsWith("http")) attachedImage else "${YamiboRoute.Domain.build()}$attachedImage"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                /** Cover image */
                Card(
                    modifier = Modifier.size(width = 100.dp, height = 130.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.brownPrimary.copy(alpha = 0.1f))
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

                /** Info column */
                Column(modifier = Modifier.weight(1f)) {
                    /** Title (long-press to copy) */
                    val titleInteractionSource = remember { MutableInteractionSource() }
                    val isTitlePressed by titleInteractionSource.collectIsPressedAsState()
                    val titleScale by animateFloatAsState(targetValue = if (isTitlePressed) 0.95f else 1f)

                    Text(
                        text = thread.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textDark,
                        modifier =
                            Modifier.graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                            }
                                .combinedClickable(
                                    interactionSource = titleInteractionSource,
                                    indication = null,
                                    onClick = {},
                                    onLongClick = {
                                        haptic.performHapticFeedback(
                                            HapticFeedbackType.LongPress
                                        )
                                        clipboardManager.setText(
                                            AnnotatedString(thread.title)
                                        )
                                        onCopy("已複製標題：${thread.title}")
                                    }
                                )
                    )
                    Spacer(Modifier.height(6.dp))

                    /** Author (long-press to copy) */
                    if (firstPost != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = YamiboIcons.PersonFill,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.brownPrimary.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(4.dp))
                            val authorInteractionSource = remember { MutableInteractionSource() }
                            val isAuthorPressed by authorInteractionSource.collectIsPressedAsState()
                            val authorScale by animateFloatAsState(targetValue = if (isAuthorPressed) 0.95f else 1f)

                            Text(
                                text = firstPost.author.name,
                                fontSize = 13.sp,
                                color = colors.brownPrimary.copy(alpha = 0.8f),
                                modifier =
                                    Modifier.graphicsLayer {
                                        scaleX = authorScale
                                        scaleY = authorScale
                                    }
                                        .combinedClickable(
                                            interactionSource = authorInteractionSource,
                                            indication = null,
                                            onClick = {},
                                            onLongClick = {
                                                haptic.performHapticFeedback(
                                                    HapticFeedbackType.LongPress
                                                )
                                                clipboardManager.setText(
                                                    AnnotatedString(
                                                        firstPost.author.name
                                                    )
                                                )
                                                onCopy("已複製作者：${firstPost.author.name}")
                                            }
                                        )
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                    }

                    /** Time */
                    if (firstPost != null) {
                        Text(
                            text = firstPost.timeText,
                            fontSize = 12.sp,
                            color = colors.brownPrimary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(3.dp))
                    }

                    /** View + reply counts */
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (thread.totalViews != null) {
                            Icon(
                                imageVector = YamiboIcons.Views,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.brownPrimary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${thread.totalViews}",
                                fontSize = 12.sp,
                                color = colors.brownPrimary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        if (thread.totalReplies != null) {
                            Icon(
                                imageVector = YamiboIcons.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.brownPrimary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${thread.totalReplies}",
                                fontSize = 12.sp,
                                color = colors.brownPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))

                    /** Forum name tag */
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.brownDeep.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "#${thread.forum.name}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = colors.brownDeep
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            /** Action row: [onFavorite] [onShare] [continue read + progress] */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                /** Favorite button */
                Surface(
                    onClick = onFavorite,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.1f)
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = YamiboIcons.StarOutline,
                            contentDescription = "收藏",
                            modifier = Modifier.size(22.dp),
                            tint = colors.brownDeep
                        )
                    }
                }

                /** Share button */
                Surface(
                    onClick = onShare,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.1f)
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = YamiboIcons.Share,
                            contentDescription = "分享",
                            modifier = Modifier.size(22.dp),
                            tint = colors.brownDeep
                        )
                    }
                }

                /** Continue / Start reading button */
                Surface(
                    onClick = onContinueRead,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownDeep
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (readingProgressText != null) "繼續閱讀" else "開始閱讀",
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }

                        /** Play icon */
                        Text(text = "▶", color = Color.White, fontSize = 16.sp)
                    }
                }
            }

            /** Tip bar */
            Text(
                text = "💡 長按標題或作者可以複製",
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 10.sp,
                color = colors.brownPrimary.copy(alpha = 0.4f)
            )
        }
    }
}
