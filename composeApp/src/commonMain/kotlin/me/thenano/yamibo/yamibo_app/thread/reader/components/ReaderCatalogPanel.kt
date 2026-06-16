package me.thenano.yamibo.yamibo_app.thread.reader.components

import me.thenano.yamibo.yamibo_app.i18n.i18n

import YamiboIcons
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.value.PostId
import me.thenano.yamibo.yamibo_app.components.text.rememberConvertedText
import me.thenano.yamibo.yamibo_app.repository.LocalChapterStateRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Catalog drawer panel showing pages and post-entries */
@Composable
internal fun ReaderCatalogPanel(
    totalPages: Int,
    loadedPostsByPage: Map<Int, List<Post>>,
    currentPage: Int,
    currentPid: PostId?,
    bookmarkedPostIds: Set<Long> = emptySet(),
    readPostIds: Set<Long> = emptySet(),
    chapterStates: Map<Long, LocalChapterStateRepository.Entry> = emptyMap(),
    onPageOrPostClick: (Int, Post?) -> Unit,
    onPostLongPress: (Post) -> Unit = {},
) {
    val colors = YamiboTheme.colors
    var expandedPages by remember { mutableStateOf(setOf(currentPage)) }

    // Auto expand current page when it changes
    LaunchedEffect(currentPage) {
        if (!expandedPages.contains(currentPage)) {
            expandedPages = expandedPages + currentPage
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.creamBackground).systemBarsPadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.brownDeep)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = i18n("目錄"),
                color = colors.creamBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(totalPages) { index ->
                val page = index + 1
                val isExpanded = expandedPages.contains(page)
                val isLoaded = loadedPostsByPage.containsKey(page)

                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "page_chevron"
                )

                Column {
                    // Page Header
                    Surface(
                        color = if (isExpanded) colors.brownLight.copy(alpha = 0.1f) else colors.creamBackground,
                        onClick = {
                            expandedPages = if (isExpanded) expandedPages - page else expandedPages + page
                            if (!isLoaded) {
                                onPageOrPostClick(page, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = i18n("第 {} 頁", page),
                                color = colors.brownPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "▲",
                                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                                color = colors.brownPrimary.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Content List (Expanded)
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (!isLoaded) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = colors.brownDeep,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            val pagePosts = loadedPostsByPage[page] ?: emptyList()
                            Column(modifier = Modifier.fillMaxWidth().background(colors.creamSurface)) {
                                pagePosts.forEach { post ->
                                    val isCurrentPost = post.pid == currentPid
                                    val isBookmarked = post.pid.value.toLong() in bookmarkedPostIds
                                    val chapterState = chapterStates[post.pid.value.toLong()]
                                    val isRead = post.pid.value.toLong() in readPostIds || chapterState?.read == true
                                    val progressText = chapterState?.progressLabel()
                                    val displayTitle = rememberConvertedText(post.title.ifEmpty { "..." })
                                    Surface(
                                        color = if (isCurrentPost) colors.brownLight.copy(alpha = 0.15f) else colors.creamSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (isRead) 0.6f else 1f)
                                            .pointerInput(page, post.pid) {
                                                detectTapGestures(
                                                    onTap = { onPageOrPostClick(page, post) },
                                                    onLongPress = { onPostLongPress(post) },
                                                )
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .let {
                                                    if (isCurrentPost) {
                                                        it.drawBehind {
                                                            drawRect(
                                                                color = colors.brownPrimary,
                                                                size = size.copy(width = 4.dp.toPx())
                                                            )
                                                        }
                                                    } else it
                                                }
                                                .padding(
                                                    horizontal = 24.dp,
                                                    vertical = if (isCurrentPost) 14.dp else 12.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${post.floor}#",
                                                color = if (isCurrentPost) colors.brownDeep else colors.brownPrimary,
                                                fontWeight = if (isCurrentPost) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontSize = if (isCurrentPost) 16.sp else 14.sp,
                                                modifier = Modifier.width(if (isCurrentPost) 48.dp else 40.dp)
                                            )
                                            if (isBookmarked) {
                                                Icon(
                                                    imageVector = YamiboIcons.Bookmark,
                                                    contentDescription = null,
                                                    tint = colors.orangeAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = displayTitle,
                                                modifier = Modifier.weight(1f),
                                                color = if (isCurrentPost) colors.brownDeep else colors.textDark,
                                                fontWeight = if (isCurrentPost) FontWeight.ExtraBold else FontWeight.Normal,
                                                fontSize = if (isCurrentPost) 16.sp else 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!isRead && progressText != null) {
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = progressText,
                                                    color = colors.orangeAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(
                                        color = colors.brownLight.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.2f))
                }
            }
        }
    }
}

private fun LocalChapterStateRepository.Entry.progressLabel(): String? {
    if (read) return null
    if (progressPercent <= 0) return null
    return i18n("已讀 {}%", progressPercent)
}
