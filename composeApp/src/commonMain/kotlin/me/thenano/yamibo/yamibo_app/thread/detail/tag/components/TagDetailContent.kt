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
import me.thenano.yamibo.yamibo_app.repository.LocalChapterStateRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.thread.detail.components.DetailNoteCard

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
    noteContent: String,
    onNoteClick: () -> Unit,
    onPageChange: (Int) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit,
    bookmarkedThreadIds: Set<Long> = emptySet(),
    readThreadIds: Set<Long> = emptySet(),
    chapterStates: Map<Long, LocalChapterStateRepository.Entry> = emptyMap(),
    tagMangaHistory: ReadHistoryRepository.TagMangaReadingHistory? = null,
    onThreadLongPress: (ThreadSummary) -> Unit = {},
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
                onShare = onShare,
                noteContent = noteContent,
                onNoteClick = onNoteClick,
            )
        }

        if (noteContent.isNotBlank()) {
            item {
                DetailNoteCard(
                    content = noteContent,
                    onEdit = onNoteClick,
                )
            }
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
            val threadId = thread.tid.value.toLong()
            val chapterState = chapterStates[thread.tid.value.toLong()]
            val read = threadId in readThreadIds ||
                chapterState?.read == true ||
                tagMangaHistory?.isReadThread(threadId) == true
            val readingProgressText = if (isMangaMode) {
                when {
                    read -> null
                    chapterState?.hasProgress == true -> chapterState.progressLabel()
                    else -> tagMangaHistory?.progressLabelFor(threadId)
                }
            } else {
                null
            }
            TagThreadCard(
                thread = thread,
                onClick = { onThreadClick(thread) },
                bookmarked = threadId in bookmarkedThreadIds,
                read = read,
                readingProgressText = readingProgressText,
                onLongPress = { onThreadLongPress(thread) },
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

private fun LocalChapterStateRepository.Entry.progressLabel(): String? {
    if (read) return null
    val currentPage = lastPageIndex?.plus(1) ?: return null
    val totalPage = totalPages?.takeIf { it > 0 } ?: return null
    return "$currentPage/$totalPage"
}

private fun ReadHistoryRepository.TagMangaReadingHistory.isReadThread(threadId: Long): Boolean {
    return this.threadId.value.toLong() == threadId &&
        threadImageTotalPages > 0 &&
        threadImagePageIndex >= threadImageTotalPages - 1
}

private fun ReadHistoryRepository.TagMangaReadingHistory.progressLabelFor(threadId: Long): String? {
    if (this.threadId.value.toLong() != threadId) return null
    if (threadImageTotalPages <= 0) return null
    val progress = (((threadImagePageIndex.coerceIn(0, threadImageTotalPages - 1) + 1) * 100f) / threadImageTotalPages)
        .toInt()
        .coerceIn(1, 100)
    if (progress >= 100) return null
    return "${threadImagePageIndex.coerceIn(0, threadImageTotalPages - 1) + 1}/$threadImageTotalPages"
}
