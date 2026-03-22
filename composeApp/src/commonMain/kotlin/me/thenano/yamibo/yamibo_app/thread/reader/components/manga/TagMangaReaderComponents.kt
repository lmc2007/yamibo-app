package me.thenano.yamibo.yamibo_app.thread.reader.components.manga

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Item types in the reader list */
internal sealed class ReaderItem {
    /** Interstitial page between chapters */
    data class InterstitialItem(
        val prevTitle: String?, 
        val nextTitle: String?, 
        val isEnd: Boolean = false,
        val isPrev: Boolean = false,
        val readingMode: ReadingMode
    ) : ReaderItem()
}

/** Interstitial page between chapters */
@Composable
internal fun InterstitialCard(item: ReaderItem.InterstitialItem) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(32.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (item.isPrev) {
                if (item.isEnd) {
                    Text("已經是第一話", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (item.nextTitle != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "目前章節：${item.nextTitle}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text("上一話", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        item.prevTitle ?: "",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val arrow = when (item.readingMode) {
                        ReadingMode.SINGLE_LTR -> "◀"
                        ReadingMode.SINGLE_RTL -> "▶"
                        else -> "▲"
                    }
                    Text(arrow, color = Color.White.copy(alpha = 0.4f), fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "「${item.nextTitle ?: ""}」開始",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                if (item.isEnd) {
                    Text("已讀完所有章節", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (item.prevTitle != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "最後章節：${item.prevTitle}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        "「${item.prevTitle ?: ""}」結束",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val arrow = when (item.readingMode) {
                        ReadingMode.SINGLE_LTR -> "▶"
                        ReadingMode.SINGLE_RTL -> "◀"
                        else -> "▼"
                    }
                    Text(arrow, color = Color.White.copy(alpha = 0.4f), fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "下一話", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        item.nextTitle ?: "",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}



/** Chapter navigation panel for Tag */
@Composable
internal fun TagCatalogPanel(
    totalPages: Int,
    loadedThreadsByPage: Map<Int, List<ThreadSummary>>,
    currentTagPage: Int,
    currentThreadId: ThreadId?,
    onPageOrThreadClick: (Int, ThreadSummary?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = YamiboTheme.colors
    var expandedPages by remember { mutableStateOf(setOf(currentTagPage)) }

    // Auto expand current page when it changes
    LaunchedEffect(currentTagPage) {
        if (!expandedPages.contains(currentTagPage)) {
            expandedPages = expandedPages + currentTagPage
        }
    }

    // Scrim
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onDismiss() })

    // Panel
    Box(
        modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight()
            .background(colors.creamBackground, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "章節列表", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textDark
                )
                IconButton(onClick = onDismiss) {
                    Text(
                        text = "✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textDark
                    )
                }
            }

            HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.15f))

            // Thread list
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(totalPages) { index ->
                    val page = index + 1
                    val isExpanded = expandedPages.contains(page)
                    val isLoaded = loadedThreadsByPage.containsKey(page)

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
                                    onPageOrThreadClick(page, null)
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
                                    text = "第 $page 頁",
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
                                val pageThreads = loadedThreadsByPage[page] ?: emptyList()
                                Column(modifier = Modifier.fillMaxWidth().background(colors.creamSurface)) {
                                    pageThreads.forEach { thread ->
                                        val isCurrentThread = thread.tid == currentThreadId
                                        Surface(
                                            color = if (isCurrentThread) colors.brownLight.copy(alpha = 0.15f) else colors.creamSurface,
                                            onClick = { onPageOrThreadClick(page, thread) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .let {
                                                        if (isCurrentThread) {
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
                                                        vertical = if (isCurrentThread) 14.dp else 12.dp
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = thread.title,
                                                    color = if (isCurrentThread) colors.brownDeep else colors.textDark,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrentThread) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        if (thread != pageThreads.lastOrNull()) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 24.dp),
                                                color = colors.brownPrimary.copy(alpha = 0.08f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}
