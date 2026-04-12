package me.thenano.yamibo.yamibo_app.thread.detail.tag.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.TagPage
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation

/** Tag Detail Content (scrollable body) */
@Composable
fun TagDetailContent(
    tagPage: TagPage,
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
    onShare: () -> Unit,
    onPageChange: (Int) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header card
        item {
            TagDetailHeaderCard(
                tagName = tagName,
                coverUrl = coverUrl,
                isMangaMode = isMangaMode,
                onMangaModeChange = onMangaModeChange,
                hasReadingHistory = hasReadingHistory,
                readingProgressText = readingProgressText,
                onContinueRead = onContinueRead,
                isFavorited = isFavorited,
                onFavorite = onFavorite,
                onFavoriteLongPress = onFavoriteLongPress,
                onShare = onShare
            )
        }

        // top page nav
        if (tagPage.pageNav != null) {
            item {
                PageNavigation(
                    pageNav = tagPage.pageNav!!,
                    onPageChange = onPageChange
                )
            }
        }

        // Thread list
        items(tagPage.threadSummaries, key = { it.tid.value }) { thread ->
            TagThreadCard(
                thread = thread,
                onClick = { onThreadClick(thread) }
            )
        }

        // bottom page nav
        if (tagPage.pageNav != null) {
            item {
                PageNavigation(
                    pageNav = tagPage.pageNav!!,
                    onPageChange = onPageChange
                )
            }
        }
    }
}
