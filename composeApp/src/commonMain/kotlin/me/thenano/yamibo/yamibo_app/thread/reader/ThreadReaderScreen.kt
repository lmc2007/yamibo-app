package me.thenano.yamibo.yamibo_app.thread.reader

import me.thenano.yamibo.yamibo_app.i18n.i18n


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.*
import me.thenano.yamibo.yamibo_app.favorite.*
import me.thenano.yamibo.yamibo_app.components.tracking.ReadingTimeTracker
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkContext
import me.thenano.yamibo.yamibo_app.repository.LocalBookMarkRepository as BookMarkRepository
import me.thenano.yamibo.yamibo_app.repository.LocalChapterStateRepository as ChapterStateRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.systembars.SystemBarsEffect
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadErrorContent
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadLoadingSkeleton
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageClickListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageDoubleClickListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageSetCoverListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalReaderOverlayVisible
import me.thenano.yamibo.yamibo_app.thread.reader.debug.DebugRecomposeProbe
import me.thenano.yamibo.yamibo_app.thread.reader.debug.debugPerfLog
import me.thenano.yamibo.yamibo_app.thread.reader.components.CommentBanner
import me.thenano.yamibo.yamibo_app.thread.reader.components.ReaderCatalogPanel
import me.thenano.yamibo.yamibo_app.thread.reader.components.ReaderOverlayMenu
import me.thenano.yamibo.yamibo_app.thread.reader.components.novel.NovelReaderSettingsPanel
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderPageProgress
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderPageProgressHint
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderPageProgressSlideBar
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderScrollJumpButton
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.PostRenderer
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlBlock
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlParser
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.normalizeHtmlBlocks
import me.thenano.yamibo.yamibo_app.thread.reader.components.tag.ITagListScreen
import me.thenano.yamibo.yamibo_app.util.buildImageRequest
import me.thenano.yamibo.yamibo_app.util.normalizeImageUrl
import me.thenano.yamibo.yamibo_app.util.shareText
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamibo_app.util.time.epochMillisOrNull
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView
import me.thenano.yamibo.yamibo_app.repository.settings.ReaderScrollButtonDisplayMode
import me.thenano.yamibo.yamibo_app.repository.settings.ReaderScrollButtonJumpTarget
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface ReaderState {
    data object Loading : ReaderState
    data object Success : ReaderState
    data class Error(val message: String) : ReaderState
}

private enum class ReaderEntryKind {
    WholePost,
    SegmentedHeader,
    SegmentedBody,
    SegmentedFooter,
    RegularTagBanner,
    NovelTagBanner,
    NovelCommentBanner,
    Separator,
}

private data class ReaderBodySegment(
    val blocks: List<HtmlBlock>,
    val anchorBlockId: String?,
    val anchorBlockType: String?,
)

private data class ReaderListEntry(
    val key: String,
    val contentType: String,
    val kind: ReaderEntryKind,
    val post: Post,
    val postIndex: Int,
    val bodyBlocks: List<HtmlBlock> = emptyList(),
    val anchorBlockId: String? = null,
    val anchorBlockType: String? = null,
) {
    val isScrollAnchor: Boolean
        get() = kind == ReaderEntryKind.WholePost || kind == ReaderEntryKind.SegmentedBody
}

private data class VisiblePostRange(
    val firstIndex: Int?,
    val lastIndex: Int?,
)

private data class ReaderCatalogCurrentPosition(
    val page: Int,
    val pid: PostId?,
)

private data class VisibleChapterProgress(
    val post: Post,
    val progressPercent: Int,
    val read: Boolean,
)

private fun buildReaderBodySegments(post: Post, contentHtml: String): List<ReaderBodySegment>? {
    if (post.images.size < 6) return null

    val blocks = normalizeHtmlBlocks(HtmlParser.parseHtml(contentHtml))
    if (blocks.none { it is HtmlBlock.Image }) return null

    val segments = mutableListOf<ReaderBodySegment>()
    val currentBlocks = mutableListOf<HtmlBlock>()

    fun flushCurrentBlocks() {
        if (currentBlocks.isEmpty()) return
        val firstBlock = currentBlocks.first()
        segments += ReaderBodySegment(
            blocks = currentBlocks.toList(),
            anchorBlockId = firstBlock.anchorId.takeIf { it.isNotBlank() },
            anchorBlockType = if (currentBlocks.size == 1) firstBlock::class.simpleName else "Mixed",
        )
        currentBlocks.clear()
    }

    blocks.forEach { block ->
        if (block is HtmlBlock.Image) {
            flushCurrentBlocks()
            segments += ReaderBodySegment(
                blocks = listOf(block),
                anchorBlockId = block.anchorId.takeIf { it.isNotBlank() },
                anchorBlockType = "Image",
            )
        } else {
            currentBlocks += block
        }
    }
    flushCurrentBlocks()

    val imageSegmentCount = segments.count { it.anchorBlockType == "Image" }
    return if (imageSegmentCount >= 6 && segments.size > 2) segments else null
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
internal fun ThreadReaderScreen(
    tid: ThreadId,
    title: String,
    threadType: ReadHistoryRepository.ThreadEntryType = ReadHistoryRepository.ThreadEntryType.Normal,
    authorId: UserId? = null,
    initialPage: Int = 1,
    targetPid: PostId? = null
) {
    DebugRecomposeProbe("ThreadReaderScreen", tid.value.toString())
    val colors = YamiboTheme.colors
    SystemBarsEffect(
        statusBarColor = colors.creamBackground,
        navigationBarColor = colors.creamBackground,
        priority = 100,
    )
    val appSettingsRepository = LocalAppSettingsRepository.current
    val novelSettingsRepository = LocalNovelReaderSettingsRepository.current
    val threadRepository = LocalThreadRepository.current
    val favoriteRepository = LocalFavoriteRepository.current
    val favoriteSyncRepository = LocalFavoriteSyncRepository.current
    val readHistoryRepo = LocalReadHistoryRepository.current
    val bookMarkRepository = LocalBookMarkRepository.current
    val chapterStateRepository = LocalChapterStateRepository.current
    ReadingTimeTracker()
    val navigator = LocalNavigator.current
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isNovelThread = threadType == ReadHistoryRepository.ThreadEntryType.Novel
    val keepSystemBarsBackground = novelSettingsRepository.keepSystemBarsBackground.state()
    val scrollButtonDisplayMode = novelSettingsRepository.scrollButtonDisplayMode.state()
    val scrollButtonDirectionThreshold = novelSettingsRepository.scrollButtonDirectionThreshold.state()
    val scrollButtonJumpTarget = novelSettingsRepository.scrollButtonJumpTarget.state()
    val showPageProgressHint = novelSettingsRepository.showPageProgressHint.state()

    var state by remember { mutableStateOf<ReaderState>(ReaderState.Loading) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var postIndexByPid by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var pageByPid by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var pageIndexBounds by remember { mutableStateOf<Map<Int, IntRange>>(emptyMap()) }
    var threadInfo by remember { mutableStateOf<ThreadInfo?>(null) }
    var loadedPages by remember { mutableStateOf(setOf<Int>()) }
    var currentPageFetching by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var isLoadingNextPage by remember { mutableStateOf(false) }

    val loadedPostsByPage = remember { mutableStateMapOf<Int, List<Post>>() }
    val postHeightCache = remember(tid) { mutableStateMapOf<Long, Int>() }
    val imageHeightCache = remember(tid) { mutableStateMapOf<String, Int>() }
    val imageAspectRatioCache = remember(tid) { mutableStateMapOf<String, Float>() }
    val pendingPostHeights = remember(tid) { mutableStateMapOf<Long, Int>() }
    val loadedImageUrlsByPost = remember(tid) { mutableStateMapOf<Long, Set<String>>() }
    val prefetchedImageUrls = remember(tid) { hashSetOf<String>() }
    val failedAutoLoadPages = remember(tid) { mutableStateMapOf<Int, String>() }
    val failedImageMessages = remember(tid) { mutableStateMapOf<String, String>() }
    val imageRetryKeys = remember(tid) { mutableStateMapOf<String, Int>() }

    var showMenu by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var scrollJumpButtonPointsDown by remember { mutableStateOf(false) }
    var showScrollJumpButtonAfterSlide by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val authRepo = LocalAuthRepository.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasRestoredPosition by remember { mutableStateOf(false) }
    var pendingSavedPosition by remember(tid, targetPid) { mutableStateOf<ThreadReadingHistory?>(null) }
    var isRestoringSavedPosition by remember { mutableStateOf(false) }
    var pendingTargetPid by remember(tid, targetPid) { mutableStateOf(targetPid?.value?.toLong()) }
    var activeChapterProgressPid by remember(tid, targetPid) { mutableStateOf(targetPid?.value?.toLong()) }

    /** Extract image URL for thread avatar based on forum type */
    var coverUrl by remember { mutableStateOf<String?>(null) }
    var manualCoverUrlOverride by remember(tid) { mutableStateOf<String?>(null) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteDialogCategories by remember {
        mutableStateOf<List<me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCategory>>(emptyList())
    }
    var favoriteDialogCategorySelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogOptions by remember {
        mutableStateOf<List<me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCollectionOption>>(emptyList())
    }
    var isFavorited by remember { mutableStateOf(false) }
    var favoriteRefreshToken by remember { mutableIntStateOf(0) }
    var pendingFavoriteRemovalSelection by remember { mutableStateOf<FavoriteLocationSelection?>(null) }
    var pendingFavoriteRemovalSuccessMessage by remember { mutableStateOf(i18n("已取消收藏")) }
    var showFavoriteRemovalConfirm by remember { mutableStateOf(false) }
    var showFavoriteMultiPathDialog by remember { mutableStateOf(false) }
    var showFavoriteAddSyncConfirm by remember { mutableStateOf(false) }
    var showFavoriteRemoveSyncConfirm by remember { mutableStateOf(false) }
    var postBookMarkEntries by remember { mutableStateOf<Map<Long, BookMarkRepository.Entry>>(emptyMap()) }
    var postChapterStates by remember { mutableStateOf<Map<Long, ChapterStateRepository.Entry>>(emptyMap()) }
    var catalogActionPost by remember { mutableStateOf<Post?>(null) }

    suspend fun reloadPostBookMarks() {
        postBookMarkEntries = bookMarkRepository
            .getEntriesByParent(BookMarkRepository.TargetType.ThreadPost, tid.value.toLong())
            .associateBy { it.targetId }
    }

    suspend fun reloadPostChapterStates() {
        postChapterStates = chapterStateRepository
            .getEntriesByParent(ChapterStateRepository.TargetType.ThreadPost, tid.value.toLong())
            .associateBy { it.targetId }
    }

    suspend fun syncAutoReadMarks(progresses: List<VisibleChapterProgress>) {
        var changed = false
        progresses.forEach { progress ->
            val postId = progress.post.pid.value.toLong()
            val currentState = postChapterStates[postId]
            if (currentState?.read == true) return@forEach

            val shouldSaveProgress = activeChapterProgressPid == postId && progress.progressPercent in 1..87
            val shouldMarkRead = progress.read
            if (!shouldSaveProgress && !shouldMarkRead) return@forEach
            if (!shouldMarkRead && progress.progressPercent <= (currentState?.progressPercent ?: 0)) return@forEach

            chapterStateRepository.upsertProgress(
                targetType = ChapterStateRepository.TargetType.ThreadPost,
                parentId = tid.value.toLong(),
                targetId = postId,
                title = progress.post.title.ifBlank { i18n("（無標題）") },
                progressPercent = if (shouldMarkRead) 100 else progress.progressPercent,
                read = shouldMarkRead,
            )
            changed = true
        }
        if (changed) {
            reloadPostChapterStates()
        }
    }

    LaunchedEffect(tid) {
        reloadPostBookMarks()
        reloadPostChapterStates()
    }
    
    fun resolveValidCoverUrl(rawUrl: String?): String? {
        if (
            rawUrl == null ||
            rawUrl.contains("none.gif") ||
            rawUrl.contains("smiley/") ||
            rawUrl.contains("face")
        ) {
            return null
        }
        return if (rawUrl.startsWith("http")) rawUrl else "${YamiboRoute.Domain.build()}$rawUrl"
    }

    LaunchedEffect(loadedPostsByPage[1], manualCoverUrlOverride) {
        if (manualCoverUrlOverride != null) {
            coverUrl = manualCoverUrlOverride
            return@LaunchedEffect
        }

        val firstPagePosts = loadedPostsByPage[1]
        if (firstPagePosts == null) {
            coverUrl = null
            return@LaunchedEffect
        }

        coverUrl = resolveValidCoverUrl(firstPagePosts.firstOrNull()?.images?.firstOrNull()?.url)
    }

    fun favoriteTarget(coverOverride: String? = coverUrl): FavoriteTargetPayload.Thread {
        val currentTitle = threadInfo?.title ?: title
        val firstPost = loadedPostsByPage[1]?.firstOrNull { it.floor == 1 } ?: posts.firstOrNull { it.floor == 1 }
        return FavoriteTargetPayload.Thread(
            tid = tid,
            title = currentTitle,
            threadType = threadType,
            authorId = authorId,
            coverUrl = coverOverride,
            lastUpdatedTime = firstPost?.lastEditedTime?.epochMillisOrNull() ?: firstPost?.timeCreate?.epochMillisOrNull(),
            forumId = threadInfo?.forum?.fid,
            forumName = threadInfo?.forum?.name,
        )
    }

    suspend fun refreshFavoriteState() {
        val target = favoriteTarget()
        favoriteRepository.syncFavoriteMetadata(target)
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        isFavorited = selection.item != null
    }

    suspend fun completeFavoriteAdd(syncToRemote: Boolean) {
        completeFavoriteAddWithFeedback(
            favoriteRepository = favoriteRepository,
            favoriteSyncRepository = favoriteSyncRepository,
            target = favoriteTarget(),
            syncToRemote = syncToRemote,
            snackbarHostState = snackbarHostState,
            onRefreshRequested = { favoriteRefreshToken += 1 },
        )
    }

    suspend fun completeSavedFavoriteSync(syncToRemote: Boolean) {
        completeSavedFavoriteSyncWithFeedback(
            favoriteRepository = favoriteRepository,
            favoriteSyncRepository = favoriteSyncRepository,
            target = favoriteTarget(),
            syncToRemote = syncToRemote,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onRefreshRequested = { favoriteRefreshToken += 1 },
        )
    }

    suspend fun completeFavoriteRemoval(removeRemote: Boolean) {
        completeFavoriteRemovalWithFeedback(
            favoriteRepository = favoriteRepository,
            favoriteSyncRepository = favoriteSyncRepository,
            target = favoriteTarget(),
            removeRemote = removeRemote,
            scope = scope,
            snackbarHostState = snackbarHostState,
            successMessage = pendingFavoriteRemovalSuccessMessage,
            failureMessage = i18n("取消收藏失敗"),
            onRefreshRequested = { favoriteRefreshToken += 1 },
        )
        pendingFavoriteRemovalSelection = null
    }

    suspend fun maybePromptRemoteRemoval() {
        val target = favoriteTarget()
        val shouldPromptRemote = target.supportsRemoteWebsiteSync() &&
            hasRemoteFavoriteForTarget(favoriteRepository, favoriteSyncRepository, target)
        when {
            shouldPromptRemote && appSettingsRepository.favoriteRemoveSyncPromptEnabled.getValue() -> {
                showFavoriteRemoveSyncConfirm = true
            }
            else -> {
                completeFavoriteRemoval(
                    removeRemote = shouldPromptRemote && appSettingsRepository.favoriteRemoveSyncDefault.getValue(),
                )
            }
        }
    }

    suspend fun toggleFavoriteQuickWithFeedback() {
        val target = favoriteTarget()
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        if (selection.item != null) {
            pendingFavoriteRemovalSelection = selection
            pendingFavoriteRemovalSuccessMessage = i18n("已取消收藏")
            if (appSettingsRepository.skipFavoriteRemovalConfirm.getValue()) {
                if (selection.paths.size > 1) {
                    showFavoriteMultiPathDialog = true
                } else {
                    maybePromptRemoteRemoval()
                }
            } else {
                showFavoriteRemovalConfirm = true
            }
            return
        }

        if (target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncPromptEnabled.getValue()) {
            favoriteRepository.saveFavorite(target)
            favoriteRefreshToken += 1
            showFavoriteAddSyncConfirm = true
        } else {
            completeFavoriteAdd(
                syncToRemote = target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncDefault.getValue(),
            )
        }
    }

    LaunchedEffect(tid, threadType, authorId, coverUrl, threadInfo?.forum?.fid, threadInfo?.forum?.name, title, favoriteRefreshToken) {
        refreshFavoriteState()
    }

    fun getFormHash(): FormHash? {
        return authRepo.currentUser()?.formHash
    }

    var refreshThreadAfterVote: (suspend () -> Unit)? = null

    val handleVote: suspend (List<PollOptionId>) -> Boolean = { optionIds ->
        val formHash = getFormHash()
        val fId = threadInfo?.forum?.fid
        if (formHash == null || fId == null) {
            snackbarHostState.showSnackbar(i18n("獲取登入資訊失敗，請重新登入"))
            false
        } else {
            when (val res = threadRepository.votePoll(fId, tid, optionIds, formHash)) {
                is YamiboResult.Success -> {
                    snackbarHostState.showSnackbar(i18n("投票成功，正在刷新頁面..."))
                    refreshThreadAfterVote?.invoke()
                    true
                }
                else -> {
                    snackbarHostState.showSnackbar(i18n("投票失敗: {}", i18n(res.message())))
                    false
                }
            }
        }
    }

    val handleRate: (PostId, Int, String, Boolean) -> Unit = { pid, score, reason, noticeAuthor ->
        val formHash = getFormHash()
        if (formHash == null) {
            scope.launch { snackbarHostState.showSnackbar(i18n("獲取登入資訊失敗，請重新登入")) }
        } else {
            scope.launch {
                when (val res = threadRepository.ratePost(tid, pid, score, reason, formHash, noticeAuthor)) {
                    is YamiboResult.Success -> snackbarHostState.showSnackbar(i18n("評分成功，刷新後更新評分/點評狀態"))
                    else -> snackbarHostState.showSnackbar(i18n("評分失敗: {}", i18n(res.message())))
                }
            }
        }
    }

    val handleComment: (PostId, String) -> Unit = { pid, message ->
        val formHash = getFormHash()
        if (formHash == null) {
            scope.launch { snackbarHostState.showSnackbar(i18n("獲取登入資訊失敗，請重新登入")) }
        } else {
            scope.launch {
                when (val res = threadRepository.commentPost(tid, pid, message, formHash)) {
                    is YamiboResult.Success -> snackbarHostState.showSnackbar(i18n("點評成功，刷新後更新評分/點評狀態"))
                    else -> snackbarHostState.showSnackbar(i18n("點評失敗: {}", i18n(res.message())))
                }
            }
        }
    }

    val handleReply: (PostId) -> Unit = { pid ->
        val replyPageUrl = YamiboRoute.PostReply(tid, pid).build()
        navigator.navigate(
            IActionWebView(
                title = i18n("發表回復"),
                initialUrl = replyPageUrl,
                successCondition = { url -> url.contains("mod=viewthread") && url.contains("tid=") },
                onSuccess = { scope.launch { snackbarHostState.showSnackbar(i18n("回復成功")) } },
            )
        )
    }

    fun rebuildPosts() {
        val mergedPosts = mutableListOf<Post>()
        val pageByPidMutable = mutableMapOf<Long, Int>()
        val pageIndexBoundsMutable = mutableMapOf<Int, IntRange>()

        loadedPostsByPage.keys.sorted().forEach { page ->
            val startIndex = mergedPosts.size
            loadedPostsByPage[page].orEmpty().forEach { post ->
                val pid = post.pid.value.toLong()
                if (pageByPidMutable.containsKey(pid)) return@forEach
                pageByPidMutable[pid] = page
                mergedPosts += post
            }

            val endIndex = mergedPosts.lastIndex
            if (endIndex >= startIndex) {
                pageIndexBoundsMutable[page] = startIndex..endIndex
            }
        }

        posts = mergedPosts
        postIndexByPid = mergedPosts.mapIndexed { index, post -> post.pid.value.toLong() to index }.toMap()
        pageByPid = pageByPidMutable
        pageIndexBounds = pageIndexBoundsMutable
    }

    val expectedImageUrlsByPost = remember(posts) {
        posts.associate { post ->
            post.pid.value.toLong() to post.images
                .asSequence()
                .map { normalizeImageUrl(it.url) }
                .toSet()
        }
    }
    val forumId = threadInfo?.forum?.fid
    val htmlLinkContext = remember(tid, title, forumId, authorId, threadType) {
        InAppLinkContext(
            currentTid = tid,
            currentTitle = title,
            currentFid = forumId,
            currentAuthorId = authorId,
            currentThreadType = threadType,
        )
    }
    val chineseConversionRepository = LocalChineseConversionRepository.current
    val chineseConversionMode by chineseConversionRepository.currentMode.collectAsState()
    val convertedContentByPid = remember { mutableStateMapOf<Long, String>() }
    var convertedContentVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(posts, chineseConversionMode) {
        convertedContentByPid.clear()
        convertedContentVersion++
        if (chineseConversionMode != null) {
            posts.forEach { post ->
                convertedContentByPid[post.pid.value.toLong()] = chineseConversionRepository.convert(post.contentHtml)
            }
            convertedContentVersion++
        }
    }

    val isMangaForum = forumId?.let { YamiboForum.isMangaForum(it) } == true
    val isNovelForum = forumId?.let { YamiboForum.isNovelForum(it) } == true
    val showRegularFirstPostTagBanner = isMangaForum || (!isNovelForum && !isNovelThread)
    val showNovelFirstPostTagBanner = isNovelThread && isNovelForum
    val segmentedBodyByPostId = remember(posts, convertedContentVersion) {
        posts.associate { post ->
            val pid = post.pid.value.toLong()
            pid to buildReaderBodySegments(post, convertedContentByPid[pid] ?: post.contentHtml)
        }
    }
    val readerEntries = remember(
        posts,
        segmentedBodyByPostId,
        pageByPid,
        isNovelThread,
        showRegularFirstPostTagBanner,
        showNovelFirstPostTagBanner,
    ) {
        buildList {
            posts.forEachIndexed { index, post ->
                val postId = post.pid.value.toLong()
                val postPage = pageByPid[postId] ?: 1
                val segmentedBody = segmentedBodyByPostId[postId]

                if (segmentedBody.isNullOrEmpty()) {
                    add(
                        ReaderListEntry(
                            key = "post-$postId",
                            contentType = "thread_post",
                            kind = ReaderEntryKind.WholePost,
                            post = post,
                            postIndex = index,
                        )
                    )
                } else {
                    add(
                        ReaderListEntry(
                            key = "post-$postId-header",
                            contentType = "thread_post_header",
                            kind = ReaderEntryKind.SegmentedHeader,
                            post = post,
                            postIndex = index,
                        )
                    )
                    segmentedBody.forEachIndexed { segmentIndex, segment ->
                        add(
                            ReaderListEntry(
                                key = "post-$postId-segment-$segmentIndex",
                                contentType = if (segment.anchorBlockType == "Image") "thread_post_image_segment" else "thread_post_text_segment",
                                kind = ReaderEntryKind.SegmentedBody,
                                post = post,
                                postIndex = index,
                                bodyBlocks = segment.blocks,
                                anchorBlockId = segment.anchorBlockId,
                                anchorBlockType = segment.anchorBlockType,
                            )
                        )
                    }
                    add(
                        ReaderListEntry(
                            key = "post-$postId-footer",
                            contentType = "thread_post_footer",
                            kind = ReaderEntryKind.SegmentedFooter,
                            post = post,
                            postIndex = index,
                        )
                    )
                }

                if (post.floor == 1 && postPage == 1 && showRegularFirstPostTagBanner) {
                    add(
                        ReaderListEntry(
                            key = "post-$postId-regular-tag-banner",
                            contentType = "thread_banner",
                            kind = ReaderEntryKind.RegularTagBanner,
                            post = post,
                            postIndex = index,
                        )
                    )
                }

                if (isNovelThread) {
                    if (post.floor == 1 && postPage == 1 && showNovelFirstPostTagBanner) {
                        add(
                            ReaderListEntry(
                                key = "post-$postId-novel-tag-banner",
                                contentType = "thread_banner",
                                kind = ReaderEntryKind.NovelTagBanner,
                                post = post,
                                postIndex = index,
                            )
                        )
                    }
                    add(
                        ReaderListEntry(
                            key = "post-$postId-novel-comment-banner",
                            contentType = "thread_banner",
                            kind = ReaderEntryKind.NovelCommentBanner,
                            post = post,
                            postIndex = index,
                        )
                    )
                }

                if (index < posts.lastIndex) {
                    add(
                        ReaderListEntry(
                            key = "post-$postId-separator",
                            contentType = "thread_separator",
                            kind = ReaderEntryKind.Separator,
                            post = post,
                            postIndex = index,
                        )
                    )
                }
            }
        }
    }

    fun visibleChapterProgresses(): List<VisibleChapterProgress> {
        if (posts.isEmpty() || readerEntries.isEmpty()) return emptyList()
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return emptyList()

        val viewportBottom = layoutInfo.viewportEndOffset
        val progressEntries = readerEntries.withIndex()
            .filter { (_, entry) ->
                when (entry.kind) {
                    ReaderEntryKind.WholePost,
                    ReaderEntryKind.SegmentedHeader,
                    ReaderEntryKind.SegmentedBody,
                    ReaderEntryKind.SegmentedFooter -> true
                    else -> false
                }
            }
        val progressEntryRangeByPid = progressEntries
            .groupBy { it.value.post.pid.value.toLong() }
            .mapValues { (_, entries) -> entries.first().index..entries.last().index }

        return visibleItems
            .mapNotNull { item ->
                val entry = readerEntries.getOrNull(item.index) ?: return@mapNotNull null
                val range = progressEntryRangeByPid[entry.post.pid.value.toLong()] ?: return@mapNotNull null
                val entryCount = (range.last - range.first + 1).coerceAtLeast(1)
                val entryOffset = (item.index - range.first).coerceIn(0, entryCount - 1)
                val entryRatio = ((viewportBottom - item.offset).toFloat() / item.size.coerceAtLeast(1).toFloat())
                    .coerceIn(0f, 1f)
                val progressPercent = (((entryOffset + entryRatio) / entryCount.toFloat()) * 100f)
                    .toInt()
                    .coerceIn(0, 100)
                VisibleChapterProgress(
                    post = entry.post,
                    progressPercent = progressPercent,
                    read = progressPercent >= 88,
                )
            }
            .groupBy { it.post.pid.value.toLong() }
            .mapNotNull { (_, progresses) -> progresses.maxByOrNull { it.progressPercent } }
    }

    val entryIndexByPid = remember(readerEntries) {
        buildMap {
            readerEntries.forEachIndexed { index, entry ->
                val postId = entry.post.pid.value.toLong()
                if (!containsKey(postId)) {
                    put(postId, index)
                }
            }
        }
    }
    val entryIndexByAnchorBlockId = remember(readerEntries) {
        buildMap {
            readerEntries.forEachIndexed { index, entry ->
                entry.anchorBlockId?.let { anchorBlockId ->
                    if (!containsKey(anchorBlockId)) {
                        put(anchorBlockId, index)
                    }
                }
            }
        }
    }

    fun isPostHeightStable(postId: Long): Boolean {
        val expectedImageUrls = expectedImageUrlsByPost[postId].orEmpty()
        if (expectedImageUrls.isEmpty()) return true
        return loadedImageUrlsByPost[postId].orEmpty().containsAll(expectedImageUrls)
    }

    fun commitPostHeightIfStable(postId: Long) {
        val measuredHeight = pendingPostHeights[postId] ?: return
        if (isPostHeightStable(postId)) {
            postHeightCache[postId] = measuredHeight
        }
    }

    fun handlePostHeightChanged(post: Post, heightPx: Int) {
        val postId = post.pid.value.toLong()
        pendingPostHeights[postId] = heightPx
        commitPostHeightIfStable(postId)
    }

    fun handlePostImageSuccess(post: Post, imageUrl: String) {
        val postId = post.pid.value.toLong()
        val normalizedUrl = normalizeImageUrl(imageUrl)
        failedImageMessages.remove(normalizedUrl)
        val loadedImageUrls = loadedImageUrlsByPost[postId].orEmpty()
        if (normalizedUrl !in loadedImageUrls) {
            loadedImageUrlsByPost[postId] = loadedImageUrls + normalizedUrl
        }
        commitPostHeightIfStable(postId)
    }

    fun handlePostImageError(imageUrl: String, message: String) {
        val normalizedUrl = normalizeImageUrl(imageUrl)
        failedImageMessages[normalizedUrl] = message
    }

    fun handlePostImageReload(imageUrl: String) {
        val normalizedUrl = normalizeImageUrl(imageUrl)
        failedImageMessages.remove(normalizedUrl)
        imageRetryKeys[normalizedUrl] = (imageRetryKeys[normalizedUrl] ?: 0) + 1
    }

    fun handleImageHeightChanged(imageUrl: String, heightPx: Int) {
        if (heightPx <= 0) return
        val normalizedUrl = normalizeImageUrl(imageUrl)
        if (imageHeightCache[normalizedUrl] != heightPx) {
            imageHeightCache[normalizedUrl] = heightPx
        }
    }

    fun handleImageAspectRatioChanged(imageUrl: String, aspectRatio: Float) {
        if (aspectRatio <= 0f || !aspectRatio.isFinite()) return
        val normalizedUrl = normalizeImageUrl(imageUrl)
        if (imageAspectRatioCache[normalizedUrl] != aspectRatio) {
            imageAspectRatioCache[normalizedUrl] = aspectRatio
        }
    }

    fun imagePlaceholderAspectRatioFor(post: Post, imageUrl: String): Float {
        val normalizedUrl = normalizeImageUrl(imageUrl)
        imageAspectRatioCache[normalizedUrl]?.let { return it }

        val postRatios = expectedImageUrlsByPost[post.pid.value.toLong()]
            .orEmpty()
            .mapNotNull(imageAspectRatioCache::get)
        if (postRatios.isNotEmpty()) {
            return postRatios.average().toFloat().coerceIn(0.6f, 3.2f)
        }

        val threadRatios = imageAspectRatioCache.values.toList()
        if (threadRatios.isNotEmpty()) {
            return threadRatios.average().toFloat().coerceIn(0.6f, 3.2f)
        }

        return 1.35f
    }

    /** Build a reading history snapshot from current scroll state (does NOT save) */
    fun buildHistory(): ThreadReadingHistory? {
        if (posts.isEmpty() || readerEntries.isEmpty()) return null
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return null

        /** Find visible post at viewport center */
        val viewportTop = layoutInfo.viewportStartOffset
        val viewportBottom = layoutInfo.viewportEndOffset
        val viewportCenter = (viewportTop + viewportBottom) / 2

        val centerEntry = visibleItems
            .mapNotNull { item ->
                readerEntries.getOrNull(item.index)?.takeIf { it.isScrollAnchor }?.let { entry -> item to entry }
            }
            .minByOrNull { (item, _) ->
                when {
                    viewportCenter < item.offset -> item.offset - viewportCenter
                    viewportCenter > item.offset + item.size -> viewportCenter - (item.offset + item.size)
                    else -> 0
                }
            }
            ?: return null

        val centerItemInfo = centerEntry.first
        val centerReaderEntry = centerEntry.second
        val centerPost = centerReaderEntry.post

        val itemTop = centerItemInfo.offset
        val itemSize = centerItemInfo.size.coerceAtLeast(1)
        val entryRatio = ((viewportCenter - itemTop).toFloat() / itemSize.toFloat()).coerceIn(0f, 1f)

        /** Find which page this post is on */
        val postPage = pageByPid[centerPost.pid.value.toLong()] ?: initialPage

        val forumInfo = threadInfo?.forum
        val firstVisible = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
        Logger.i("ThreadReaderScreen", "$title : $coverUrl")
        return ThreadReadingHistory(
            threadType = threadType,
            threadName = threadInfo?.title ?: title,
            threadId = tid,
            threadCover = coverUrl,
            lastUpdatedTime = loadedPostsByPage[1]
                ?.firstOrNull { it.floor == 1 }
                ?.let { it.lastEditedTime?.epochMillisOrNull() ?: it.timeCreate.epochMillisOrNull() },
            forumName = forumInfo?.name,
            forumId = forumInfo?.fid,
            authorId = authorId,
            page = postPage,
            postId = centerPost.pid,
            postTitle = centerPost.title,
            anchorPostId = centerPost.pid.value.toLong(),
            anchorPostRatio = if (centerReaderEntry.kind == ReaderEntryKind.WholePost) entryRatio else null,
            anchorBlockId = centerReaderEntry.anchorBlockId,
            anchorBlockType = centerReaderEntry.anchorBlockType,
            anchorBlockRatio = if (centerReaderEntry.kind == ReaderEntryKind.SegmentedBody) entryRatio else null,
            globalScrollY = null,
            viewportHeight = (viewportBottom - viewportTop),
            firstVisibleItemIndex = firstVisible,
            firstVisibleItemOffset = firstVisibleOffset,
            lastVisitTime = currentTimeMillis()
        )
    }

    fun visibleAnchorEntryIndex(): Int? {
        if (readerEntries.isEmpty()) return null
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return null

        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val anchorIndex = visibleItems
            .mapNotNull { item ->
                readerEntries.getOrNull(item.index)
                    ?.takeIf { it.isScrollAnchor }
                    ?.let { item.index to item }
            }
            .minByOrNull { (_, item) ->
                when {
                    viewportCenter < item.offset -> item.offset - viewportCenter
                    viewportCenter > item.offset + item.size -> viewportCenter - (item.offset + item.size)
                    else -> 0
                }
            }
            ?.first
        if (anchorIndex != null) return anchorIndex

        return visibleItems
            .mapNotNull { item ->
                readerEntries.getOrNull(item.index)?.let { item.index to item }
            }
            .minByOrNull { (_, item) ->
                when {
                    viewportCenter < item.offset -> item.offset - viewportCenter
                    viewportCenter > item.offset + item.size -> viewportCenter - (item.offset + item.size)
                    else -> 0
                }
            }
            ?.first
    }

    fun currentReaderPageProgress(): ReaderPageProgress? {
        val currentIndex = visibleAnchorEntryIndex() ?: return null
        val currentEntry = readerEntries.getOrNull(currentIndex) ?: return null
        val postPage = pageByPid[currentEntry.post.pid.value.toLong()] ?: initialPage
        val postBounds = pageIndexBounds[postPage] ?: return ReaderPageProgress(
            page = postPage,
            totalPages = totalPages,
            fraction = 0f,
        )
        val pagePostCount = (postBounds.last - postBounds.first + 1).coerceAtLeast(1)
        val relativePostIndex = (currentEntry.postIndex - postBounds.first)
            .coerceIn(0, pagePostCount - 1)

        val layoutInfo = listState.layoutInfo
        val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
        val itemRatio = visibleItem?.let { item ->
            ((viewportCenter - item.offset).toFloat() / item.size.coerceAtLeast(1).toFloat())
                .coerceIn(0f, 1f)
        } ?: 0f

        return ReaderPageProgress(
            page = postPage,
            totalPages = totalPages,
            fraction = ((relativePostIndex + itemRatio) / pagePostCount.toFloat()).coerceIn(0f, 1f),
        )
    }

    fun currentVisiblePageForAction(): Int {
        val currentIndex = visibleAnchorEntryIndex() ?: listState.firstVisibleItemIndex
        val currentEntry = readerEntries.getOrNull(currentIndex) ?: return initialPage
        return pageByPid[currentEntry.post.pid.value.toLong()] ?: initialPage
    }

    fun anchorIndexForPost(postId: Long, last: Boolean): Int? {
        var foundIndex: Int? = null
        readerEntries.forEachIndexed { index, entry ->
            if (entry.isScrollAnchor && entry.post.pid.value.toLong() == postId) {
                if (!last) return index
                foundIndex = index
            }
        }
        return foundIndex
    }

    fun pageEdgeAnchorIndex(currentEntry: ReaderListEntry, pointsDown: Boolean): Int? {
        val currentPage = pageByPid[currentEntry.post.pid.value.toLong()] ?: return null
        val postBounds = pageIndexBounds[currentPage] ?: return null
        val targetPost = posts.getOrNull(if (pointsDown) postBounds.last else postBounds.first) ?: return null
        return anchorIndexForPost(targetPost.pid.value.toLong(), last = pointsDown)
    }

    fun postEdgeAnchorIndex(currentIndex: Int, currentEntry: ReaderListEntry, pointsDown: Boolean): Int? {
        val currentPostId = currentEntry.post.pid.value.toLong()
        val currentFirst = anchorIndexForPost(currentPostId, last = false) ?: currentIndex
        val currentLast = anchorIndexForPost(currentPostId, last = true) ?: currentIndex

        return if (pointsDown) {
            if (currentIndex < currentLast) {
                currentLast
            } else {
                posts.getOrNull(currentEntry.postIndex + 1)
                    ?.let { anchorIndexForPost(it.pid.value.toLong(), last = false) }
                    ?: currentLast
            }
        } else {
            val currentOffset = listState.firstVisibleItemScrollOffset
            if (currentIndex > currentFirst || currentOffset > 24) {
                currentFirst
            } else {
                posts.getOrNull(currentEntry.postIndex - 1)
                    ?.let { anchorIndexForPost(it.pid.value.toLong(), last = false) }
                    ?: currentFirst
            }
        }
    }

    fun scrollJumpTargetIndex(): Int? {
        val currentIndex = visibleAnchorEntryIndex() ?: return null
        val currentEntry = readerEntries.getOrNull(currentIndex) ?: return null
        return when (scrollButtonJumpTarget) {
            ReaderScrollButtonJumpTarget.PAGE_EDGE -> pageEdgeAnchorIndex(currentEntry, scrollJumpButtonPointsDown)
            ReaderScrollButtonJumpTarget.POST_EDGE -> postEdgeAnchorIndex(currentIndex, currentEntry, scrollJumpButtonPointsDown)
        }
    }

    fun saveCurrentHistoryBlocking() {
        val history = buildHistory() ?: return
        runBlocking {
            try {
                readHistoryRepo.savePosition(history)
                syncAutoReadMarks(visibleChapterProgresses())
            } catch (_: Exception) {
            }
        }
    }

    fun saveCurrentHistoryAndPop() {
        val history = buildHistory()
        if (history == null) {
            navigator.pop()
            return
        }
        scope.launch {
            try {
                readHistoryRepo.savePosition(history)
                syncAutoReadMarks(visibleChapterProgresses())
            } catch (_: Exception) {
            }
            navigator.pop()
        }
    }

    fun applySelectedCover(imageUrl: String) {
        val resolvedCoverUrl = resolveValidCoverUrl(imageUrl) ?: return
        manualCoverUrlOverride = resolvedCoverUrl
        coverUrl = resolvedCoverUrl
        scope.launch {
            buildHistory()?.copy(threadCover = resolvedCoverUrl)?.let { history ->
                try {
                    readHistoryRepo.savePosition(history)
                    syncAutoReadMarks(visibleChapterProgresses())
                } catch (_: Exception) {
                }
            }
            try {
                favoriteRepository.syncFavoriteMetadata(favoriteTarget(coverOverride = resolvedCoverUrl))
            } catch (_: Exception) {
            }
            snackbarHostState.showSnackbar(i18n("已設為封面"))
        }
    }

    suspend fun loadPage(page: Int, forceRefresh: Boolean = false, autoTriggered: Boolean = false): Boolean {
        if (!forceRefresh && page in loadedPages) return true
        isLoadingNextPage = true
        var loadSucceeded = false

        fun loadFromCache(): Boolean {
            val cached = threadRepository.getCachedThread(tid, authorId, page)
            if (cached != null) {
                loadedPostsByPage[page] = cached.posts
                rebuildPosts()
                threadInfo = cached.thread
                totalPages = cached.pageNav?.totalPages ?: 1
                loadedPages = loadedPages + page
                failedAutoLoadPages.remove(page)
                if (page == initialPage || page == 1) state = ReaderState.Success
                return true
            }
            return false
        }

        fun updatePage(result: YamiboResult.Success<ThreadPage>) {
            loadedPostsByPage[page] = result.value.posts
            rebuildPosts()
            totalPages = result.value.pageNav?.totalPages ?: 1
            loadedPages = loadedPages + page
            failedAutoLoadPages.remove(page)
            threadInfo = result.value.thread
            if (page == initialPage || page == 1) state = ReaderState.Success
        }

        if (forceRefresh) {
            when (val result = threadRepository.fetchThread(tid, authorId, page)) {
                is YamiboResult.Success -> {
                    updatePage(result)
                    loadSucceeded = true
                }
                else -> {
                    snackbarHostState.showSnackbar(i18n("刷新失敗: {}，嘗試讀取緩存", i18n(result.message())))
                    if (loadFromCache()) {
                        loadSucceeded = true
                    } else if (page == initialPage || page == 1) {
                        state = ReaderState.Error(i18n(result.message()))
                    }
                }
            }
        } else {
            if (loadFromCache()) {
                isLoadingNextPage = false
                return true
            }

            when (val result = threadRepository.fetchThread(tid, authorId, page)) {
                is YamiboResult.Success -> {
                    updatePage(result)
                    loadSucceeded = true
                }
                else -> {
                    if (autoTriggered && page != initialPage && page != 1) {
                        failedAutoLoadPages[page] = i18n(result.message())
                    }
                    if (page == initialPage || page == 1) state = ReaderState.Error(i18n(result.message()))
                    else snackbarHostState.showSnackbar(i18n("載入失敗: {}", i18n(result.message())))
                }
            }
        }
        isLoadingNextPage = false
        return loadSucceeded
    }

    refreshThreadAfterVote = {
        val pagesToRefresh = loadedPages.ifEmpty { setOf(currentPageFetching) }.toList()
        pagesToRefresh.forEach { page ->
            loadPage(page, forceRefresh = true)
        }
    }

    suspend fun fallbackNearestPost(targetPidLong: Long, fallbackPage: Int) {
        if (posts.isEmpty() || state is ReaderState.Error) return
        var targetPage = fallbackPage
        val maxPid = posts.maxOfOrNull { it.pid.value.toLong() } ?: return
        val minPid = posts.minOfOrNull { it.pid.value.toLong() } ?: return

        if (targetPidLong > maxPid && targetPage < totalPages) {
            targetPage++
            if (targetPage !in loadedPages) loadPage(targetPage)
        } else if (targetPidLong < minPid && targetPage > 1) {
            targetPage--
            if (targetPage !in loadedPages) loadPage(targetPage)
        }

        val nearestIndex =
            posts.indices.minByOrNull { abs(posts[it].pid.value.toLong() - targetPidLong) } ?: -1
        if (nearestIndex >= 0) {
            val nearestPost = posts[nearestIndex]
            val entryIndex = entryIndexByPid[nearestPost.pid.value.toLong()] ?: nearestIndex
            listState.scrollToItem(entryIndex)
            hasRestoredPosition = true
            if (nearestPost.pid.value.toLong() != targetPidLong) {
                snackbarHostState.showSnackbar(i18n("找不到指定的樓層，已跳轉至最接近的樓層"))
            }
        }
    }

    suspend fun restoreSavedListOffset(index: Int, offset: Int) {
        listState.scrollToItem(index, offset)
        withFrameNanos { }
        delay(120.milliseconds)
        listState.scrollToItem(index, offset)
        delay(300.milliseconds)
        listState.scrollToItem(index, offset)
    }

    suspend fun scrollToChapterTarget(index: Int, pid: Long) {
        val progressPercent = postChapterStates[pid]
            ?.progressPercent
            ?.takeIf { it in 1..87 }
            ?: 0
        if (progressPercent <= 0) {
            listState.scrollToItem(index)
            return
        }

        listState.scrollToItem(index)
        withFrameNanos { }
        val itemHeight = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?.size
            ?: 0
        val offset = (itemHeight * (progressPercent / 100f)).toInt().coerceAtLeast(0)
        listState.scrollToItem(index, offset)
    }

    suspend fun restoreSavedPosition(savedPosition: ThreadReadingHistory) {
        val savedIndex = savedPosition.firstVisibleItemIndex
        val savedOffset = savedPosition.firstVisibleItemOffset
        if (savedIndex != null && savedIndex >= 0 && savedIndex < readerEntries.size) {
            val entryAtSavedIndex = readerEntries[savedIndex]
            if (
                entryAtSavedIndex.post.pid.value.toLong() == savedPosition.anchorPostId &&
                (savedPosition.anchorBlockId == null || entryAtSavedIndex.anchorBlockId == savedPosition.anchorBlockId)
            ) {
                restoreSavedListOffset(savedIndex, savedOffset ?: 0)
                return
            }
        }

        if (!savedPosition.anchorBlockId.isNullOrEmpty()) {
            val blockIndex = entryIndexByAnchorBlockId[savedPosition.anchorBlockId]
            if (blockIndex != null) {
                listState.scrollToItem(blockIndex)
                return
            }
        }

        if (savedPosition.anchorPostId > 0) {
            val postIndex = entryIndexByPid[savedPosition.anchorPostId] ?: -1
            if (postIndex >= 0) {
                listState.scrollToItem(postIndex)
            } else {
                fallbackNearestPost(savedPosition.anchorPostId, savedPosition.page)
            }
        }
    }

    // Initial load + position restore
    LaunchedEffect(tid, initialPage, targetPid) {
        loadPage(initialPage)

        /** Restore position from history if no explicit targetPid */
        if (!hasRestoredPosition && targetPid == null) {
            try {
                val savedPosition = readHistoryRepo.getPosition(tid, threadType, authorId)
                if (savedPosition != null) {
                    savedPosition.threadCover?.let { savedCover ->
                        manualCoverUrlOverride = savedCover
                        coverUrl = savedCover
                    }
                    // Ensure the saved page is loaded
                    if (savedPosition.page != initialPage) {
                        loadPage(savedPosition.page)
                    }
                    pendingSavedPosition = savedPosition
                } else {
                    hasRestoredPosition = true
                }
            } catch (_: Exception) {
                hasRestoredPosition = true
            }
        }
    }

    LaunchedEffect(state, readerEntries, pendingSavedPosition) {
        val savedPosition = pendingSavedPosition ?: return@LaunchedEffect
        if (state != ReaderState.Success || readerEntries.isEmpty()) return@LaunchedEffect

        isRestoringSavedPosition = true
        try {
            restoreSavedPosition(savedPosition)
        } finally {
            pendingSavedPosition = null
            hasRestoredPosition = true
            delay(300.milliseconds)
            isRestoringSavedPosition = false
        }
    }

    LaunchedEffect(state, posts, readerEntries, pendingTargetPid) {
        val targetPidLong = pendingTargetPid ?: return@LaunchedEffect
        if (state != ReaderState.Success || posts.isEmpty() || readerEntries.isEmpty()) return@LaunchedEffect

        val targetIndex = entryIndexByPid[targetPidLong] ?: -1
        if (targetIndex >= 0) {
            scrollToChapterTarget(targetIndex, targetPidLong)
            hasRestoredPosition = true
            pendingTargetPid = null
        } else {
            fallbackNearestPost(targetPidLong, initialPage)
            hasRestoredPosition = true
            pendingTargetPid = null
        }
    }

    /** Scroll detection → debounced position save */
    LaunchedEffect(listState, state) {
        if (state != ReaderState.Success) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(2000.milliseconds)
            .collect {
                if (!hasRestoredPosition || pendingSavedPosition != null || isRestoringSavedPosition) return@collect
                val history = buildHistory() ?: return@collect
                try {
                    readHistoryRepo.savePosition(history)
                    syncAutoReadMarks(visibleChapterProgresses())
                } catch (_: Exception) {
                }
            }
    }

    LaunchedEffect(listState, state, readerEntries, postChapterStates) {
        if (state != ReaderState.Success) return@LaunchedEffect
        snapshotFlow { visibleChapterProgresses() }
            .sample(500.milliseconds)
            .collect { progresses ->
                if (!hasRestoredPosition || pendingSavedPosition != null || isRestoringSavedPosition) return@collect
                try {
                    syncAutoReadMarks(progresses)
                } catch (_: Exception) {
                }
            }
    }

    LaunchedEffect(listState, state, scrollButtonDisplayMode, scrollButtonDirectionThreshold) {
        if (state != ReaderState.Success || scrollButtonDisplayMode == ReaderScrollButtonDisplayMode.NEVER) {
            showScrollJumpButtonAfterSlide = false
            return@LaunchedEffect
        }

        var anchorY: Long? = null
        var visibilityToken = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val currentY = index.toLong() * 1000L + offset.toLong()
                val lastY = anchorY
                if (lastY == null) {
                    anchorY = currentY
                    return@collect
                }

                val threshold = scrollButtonDirectionThreshold.coerceAtLeast(1).toLong()
                val delta = currentY - lastY
                if (delta > threshold) {
                    scrollJumpButtonPointsDown = true
                    anchorY = lastY + ((delta - threshold) / 2L) + 1L
                } else if (delta < -threshold || delta <= 0L) {
                    scrollJumpButtonPointsDown = false
                    anchorY = currentY
                }

                if (scrollButtonDisplayMode == ReaderScrollButtonDisplayMode.WHEN_USER_SLIDE) {
                    if (!showScrollJumpButtonAfterSlide) {
                        showScrollJumpButtonAfterSlide = true
                    }
                    visibilityToken += 1
                    val token = visibilityToken
                    launch {
                        delay(1800.milliseconds)
                        if (visibilityToken == token) {
                            showScrollJumpButtonAfterSlide = false
                        }
                    }
                }
            }
    }

    LaunchedEffect(listState, state, posts, readerEntries, tid, isLoadingNextPage, loadedPages, totalPages) {
        if (state != ReaderState.Success || posts.isEmpty() || readerEntries.isEmpty()) return@LaunchedEffect

        val imageLoader = SingletonImageLoader.get(platformContext)
        val cookie = authRepo.cookieStore.load().orEmpty()
        val preloadBehindCount = 2
        val preloadAheadCount = 2
        val pagePreloadThreshold = 5

        snapshotFlow {
            val visiblePostIndices = listState.layoutInfo.visibleItemsInfo
                .mapNotNull { item -> readerEntries.getOrNull(item.index)?.postIndex }
                .distinct()
            VisiblePostRange(
                firstIndex = visiblePostIndices.minOrNull(),
                lastIndex = visiblePostIndices.maxOrNull(),
            )
        }
            .distinctUntilChanged()
            .collect { range ->
                val firstVisibleItemIndex = range.firstIndex ?: return@collect
                val lastVisibleItemIndex = range.lastIndex ?: return@collect
                val startIndex = (firstVisibleItemIndex - preloadBehindCount).coerceAtLeast(0)
                val endIndex = (lastVisibleItemIndex + preloadAheadCount).coerceAtMost(posts.lastIndex)

                if (startIndex <= endIndex) {
                    for (index in startIndex..endIndex) {
                        val post = posts[index]
                        expectedImageUrlsByPost[post.pid.value.toLong()].orEmpty().forEach { imageUrl ->
                            if (imageUrl in failedImageMessages) return@forEach
                            if (!prefetchedImageUrls.add(imageUrl)) return@forEach
                            imageLoader.enqueue(
                                buildImageRequest(
                                    context = platformContext,
                                    url = imageUrl,
                                    cookie = cookie,
                                    enableCrossfade = false,
                                )
                            )
                        }
                    }
                }

                if (isLoadingNextPage) return@collect

                // Detect if we are close to the beginning of the CURRENT loaded page block
                val firstPost = posts.getOrNull(firstVisibleItemIndex)
                if (firstPost != null) {
                    val page = pageByPid[firstPost.pid.value.toLong()] ?: 1
                    val bounds = pageIndexBounds[page]
                    if (bounds != null && firstVisibleItemIndex - bounds.first <= pagePreloadThreshold) {
                        val prevPage = page - 1
                        if (prevPage >= 1 && prevPage !in loadedPages && prevPage !in failedAutoLoadPages) {
                            currentPageFetching = prevPage
                            debugPerfLog("auto_preload_prev|page=$prevPage|first=$firstVisibleItemIndex|last=$lastVisibleItemIndex")
                            scope.launch { loadPage(prevPage, autoTriggered = true) }
                            return@collect
                        }
                    }
                }

                // Detect if we are close to the end of the CURRENT loaded page block
                val lastPost = posts.getOrNull(lastVisibleItemIndex)
                if (lastPost != null) {
                    val page = pageByPid[lastPost.pid.value.toLong()] ?: 1
                    val bounds = pageIndexBounds[page]
                    if (bounds != null && bounds.last - lastVisibleItemIndex <= pagePreloadThreshold) {
                        val nextPage = page + 1
                        if (nextPage <= totalPages && nextPage !in loadedPages && nextPage !in failedAutoLoadPages) {
                            currentPageFetching = nextPage
                            debugPerfLog("auto_preload_next|page=$nextPage|first=$firstVisibleItemIndex|last=$lastVisibleItemIndex")
                            scope.launch { loadPage(nextPage, autoTriggered = true) }
                            return@collect
                        }
                    }
                }
            }
    }

    /** Save on leaving screen — use runBlocking since scope is already canceled */
    DisposableEffect(tid, navigator) {
        val handler = {
            if (navigator.currentScreen is IThreadReaderScreen) {
                saveCurrentHistoryBlocking()
            }
            false
        }
        navigator.backHandlers.add(handler)
        onDispose {
            navigator.backHandlers.remove(handler)
            saveCurrentHistoryBlocking()
        }
    }

    val nextFailedAutoLoadPage = remember(loadedPages, failedAutoLoadPages, totalPages) {
        val candidate = loadedPages.maxOrNull()?.plus(1) ?: return@remember null
        if (candidate <= totalPages && candidate in failedAutoLoadPages) candidate else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = colors.creamBackground,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    ReaderCatalogPanelWithPosition(
                        listState = listState,
                        readerEntries = readerEntries,
                        pageByPid = pageByPid,
                        initialPage = initialPage,
                        totalPages = totalPages,
                        loadedPostsByPage = loadedPostsByPage,
                        bookmarkedPostIds = postBookMarkEntries.values
                            .filter { it.bookmarked }
                            .mapTo(mutableSetOf()) { it.targetId },
                        readPostIds = emptySet(),
                        chapterStates = postChapterStates,
                        onPageOrPostClick = { page, post ->
                            scope.launch {
                                if (post != null) {
                                    activeChapterProgressPid = post.pid.value.toLong()
                                    drawerState.close()
                                    if (page !in loadedPages) {
                                        loadPage(page)
                                        delay(50.milliseconds) // Wait briefly for Compose to layout the new items
                                    }
                                    val targetIndex = entryIndexByPid[post.pid.value.toLong()] ?: -1
                                    if (targetIndex >= 0) {
                                        scrollToChapterTarget(targetIndex, post.pid.value.toLong())
                                    }
                                } else {
                                    // User just clicked the page header to expand catalog, load the page, don't close drawer
                                    if (page !in loadedPages) {
                                        loadPage(page)
                                    }
                                }
                            }
                        },
                        onPostLongPress = { post -> catalogActionPost = post },
                    )
                }
            }
        ) {
        val handleImageDoubleTap: (String) -> Unit = { url ->
            val post = posts.firstOrNull { p -> p.images.any { it.url.endsWith(url) || url.endsWith(it.url) } }
            if (post != null) {
                val imageList = post.images.map { img ->
                    if (img.url.startsWith("http")) img.url else "${YamiboRoute.Domain.build()}${img.url}"
                }
                val cleanUrl = if (url.startsWith("http")) url else "${YamiboRoute.Domain.build()}$url"
                val initialIndex = imageList.indexOfFirst { it == cleanUrl }.coerceAtLeast(0)

                navigator.navigate(
                    IImageReaderScreen(
                        tid = tid,
                        postId = post.pid,
                        fid = threadInfo?.forum?.fid,
                        authorId = authorId,
                        threadTitle = title,
                        imageList = imageList,
                        initialPage = initialIndex + 1
                    )
                )
            }
        }

        CompositionLocalProvider(
            LocalReaderOverlayVisible provides showMenu,
            LocalImageClickListener provides { showMenu = !showMenu },
            LocalImageDoubleClickListener provides handleImageDoubleTap,
            LocalImageSetCoverListener provides ::applySelectedCover,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.creamBackground)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = waitForUpOrCancellation()
                            if (up != null && !up.isConsumed) {
                                val x = up.position.x
                                val width = size.width
                                if (x in (width / 3f)..(width * 2f / 3f)) {
                                    showMenu = !showMenu
                                    debugPerfLog("toggle_overlay|showMenu=$showMenu")
                                }
                            }
                        }
                    }
            ) {
                when (val currentState = state) {
                    is ReaderState.Loading -> Box(
                        modifier = Modifier.systemBarsPadding().fillMaxSize()
                    ) { ThreadLoadingSkeleton() }

                    is ReaderState.Error -> Box(modifier = Modifier.systemBarsPadding().fillMaxSize()) {
                        ThreadErrorContent(
                            message = currentState.message,
                            onRetry = {
                                state = ReaderState.Loading
                                scope.launch { loadPage(1) }
                            }
                        )
                    }

                    is ReaderState.Success -> {
                        val topContentPadding = if (keepSystemBarsBackground) {
                            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                        } else {
                            0.dp
                        }
                        val bottomContentPadding = if (keepSystemBarsBackground) {
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 40.dp
                        } else {
                            40.dp
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = topContentPadding,
                                bottom = bottomContentPadding,
                            )
                        ) {
                            itemsIndexed(
                                items = readerEntries,
                                key = { _, entry -> entry.key },
                                contentType = { _, entry -> entry.contentType }
                            ) { _, entry ->
                                val post = entry.post
                                val postId = post.pid.value.toLong()
                                val hasTrackedImages = expectedImageUrlsByPost[postId].orEmpty().isNotEmpty()

                                when (entry.kind) {
                                    ReaderEntryKind.WholePost -> {
                                        PostRenderer(
                                            post = post,
                                            threadTitle = if (post.floor == 1) title else null,
                                            totalViews = threadInfo?.totalViews.takeIf { post.floor == 1 },
                                            totalReplies = threadInfo?.totalReplies.takeIf { post.floor == 1 },
                                            linkContext = htmlLinkContext,
                                            onVote = { optionIds -> handleVote(optionIds) },
                                            onLoadRateOptions = { threadRepository.fetchRatePopoutPage(tid, post.pid) },
                                            onRate = { score, reason, noticeAuthor -> handleRate(post.pid, score, reason, noticeAuthor) },
                                            onComment = { message -> handleComment(post.pid, message) },
                                            onReply = { handleReply(post.pid) },
                                            cachedHeightPx = if (hasTrackedImages) postHeightCache[postId] else null,
                                            onHeightChanged = if (hasTrackedImages) {
                                                { heightPx -> handlePostHeightChanged(post, heightPx) }
                                            } else {
                                                null
                                            },
                                            onImageSuccess = { imageUrl -> handlePostImageSuccess(post, imageUrl) },
                                            onImageError = { imageUrl, message -> handlePostImageError(imageUrl, message) },
                                            onImageReload = { imageUrl -> handlePostImageReload(imageUrl) },
                                            imageErrorMessageFor = { imageUrl ->
                                                failedImageMessages[normalizeImageUrl(imageUrl)]
                                            },
                                            imageRetryKeyFor = { imageUrl ->
                                                imageRetryKeys[normalizeImageUrl(imageUrl)] ?: 0
                                            },
                                            imageCachedHeightFor = { imageUrl ->
                                                imageHeightCache[normalizeImageUrl(imageUrl)]
                                            },
                                            imagePlaceholderAspectRatioFor = { imageUrl ->
                                                imagePlaceholderAspectRatioFor(post, imageUrl)
                                            },
                                            onImageHeightChanged = { imageUrl, heightPx ->
                                                handleImageHeightChanged(imageUrl, heightPx)
                                            },
                                            onImageAspectRatioChanged = { imageUrl, aspectRatio ->
                                                handleImageAspectRatioChanged(imageUrl, aspectRatio)
                                            },
                                        )
                                    }

                                    ReaderEntryKind.SegmentedHeader -> {
                                        PostRenderer(
                                            post = post,
                                            threadTitle = if (post.floor == 1) title else null,
                                            totalViews = threadInfo?.totalViews.takeIf { post.floor == 1 },
                                            totalReplies = threadInfo?.totalReplies.takeIf { post.floor == 1 },
                                            linkContext = htmlLinkContext,
                                            bodyBlocks = emptyList(),
                                            showFooter = false,
                                            onImageSuccess = { imageUrl -> handlePostImageSuccess(post, imageUrl) },
                                            onImageError = { imageUrl, message -> handlePostImageError(imageUrl, message) },
                                            onImageReload = { imageUrl -> handlePostImageReload(imageUrl) },
                                            imageErrorMessageFor = { imageUrl ->
                                                failedImageMessages[normalizeImageUrl(imageUrl)]
                                            },
                                            imageRetryKeyFor = { imageUrl ->
                                                imageRetryKeys[normalizeImageUrl(imageUrl)] ?: 0
                                            },
                                            imageCachedHeightFor = { imageUrl ->
                                                imageHeightCache[normalizeImageUrl(imageUrl)]
                                            },
                                            imagePlaceholderAspectRatioFor = { imageUrl ->
                                                imagePlaceholderAspectRatioFor(post, imageUrl)
                                            },
                                            onImageHeightChanged = { imageUrl, heightPx ->
                                                handleImageHeightChanged(imageUrl, heightPx)
                                            },
                                            onImageAspectRatioChanged = { imageUrl, aspectRatio ->
                                                handleImageAspectRatioChanged(imageUrl, aspectRatio)
                                            },
                                        )
                                    }

                                    ReaderEntryKind.SegmentedBody -> {
                                        PostRenderer(
                                            post = post,
                                            bodyBlocks = entry.bodyBlocks,
                                            linkContext = htmlLinkContext,
                                            showHeader = false,
                                            showFooter = false,
                                            verticalPadding = 0.dp,
                                            onImageSuccess = { imageUrl -> handlePostImageSuccess(post, imageUrl) },
                                            onImageError = { imageUrl, message -> handlePostImageError(imageUrl, message) },
                                            onImageReload = { imageUrl -> handlePostImageReload(imageUrl) },
                                            imageErrorMessageFor = { imageUrl ->
                                                failedImageMessages[normalizeImageUrl(imageUrl)]
                                            },
                                            imageRetryKeyFor = { imageUrl ->
                                                imageRetryKeys[normalizeImageUrl(imageUrl)] ?: 0
                                            },
                                            imageCachedHeightFor = { imageUrl ->
                                                imageHeightCache[normalizeImageUrl(imageUrl)]
                                            },
                                            imagePlaceholderAspectRatioFor = { imageUrl ->
                                                imagePlaceholderAspectRatioFor(post, imageUrl)
                                            },
                                            onImageHeightChanged = { imageUrl, heightPx ->
                                                handleImageHeightChanged(imageUrl, heightPx)
                                            },
                                            onImageAspectRatioChanged = { imageUrl, aspectRatio ->
                                                handleImageAspectRatioChanged(imageUrl, aspectRatio)
                                            },
                                        )
                                    }

                                    ReaderEntryKind.SegmentedFooter -> {
                                        PostRenderer(
                                            post = post,
                                            bodyBlocks = emptyList(),
                                            showHeader = false,
                                            showFooter = true,
                                            linkContext = htmlLinkContext,
                                            verticalPadding = 0.dp,
                                            onVote = { optionIds -> handleVote(optionIds) },
                                            onLoadRateOptions = { threadRepository.fetchRatePopoutPage(tid, post.pid) },
                                            onRate = { score, reason, noticeAuthor -> handleRate(post.pid, score, reason, noticeAuthor) },
                                            onComment = { message -> handleComment(post.pid, message) },
                                            onReply = { handleReply(post.pid) },
                                        )
                                    }

                                    ReaderEntryKind.RegularTagBanner,
                                    ReaderEntryKind.NovelTagBanner -> {
                                        CommentBanner(
                                            text = i18n("查看標籤列表"),
                                            icon = "🏷️",
                                            onClick = {
                                                navigator.navigate(
                                                    ITagListScreen(
                                                        tid = tid,
                                                        initialTags = post.tags.value
                                                    )
                                                )
                                            }
                                        )
                                    }

                                    ReaderEntryKind.NovelCommentBanner -> {
                                        CommentBanner(
                                            text = i18n("點擊跳轉到評論區"),
                                            onClick = {
                                                navigator.navigate(
                                                    ICommentReaderScreen(
                                                        tid = tid,
                                                        postTitle = post.title.ifEmpty { i18n("第{}樓", post.floor) },
                                                        oPostId = post.pid,
                                                        authorId = authorId ?: post.author.uid
                                                    )
                                                )
                                            }
                                        )
                                    }

                                    ReaderEntryKind.Separator -> {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                            color = colors.brownPrimary.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }

                            if (isLoadingNextPage) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = colors.brownPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            if (nextFailedAutoLoadPage != null) {
                                item(key = "retry_page_$nextFailedAutoLoadPage") {
                                    CommentBanner(
                                        text = i18n("第 {} 頁載入失敗，點擊重試", nextFailedAutoLoadPage),
                                        icon = "↻",
                                        onClick = {
                                            scope.launch {
                                                failedAutoLoadPages.remove(nextFailedAutoLoadPage)
                                                loadPage(nextFailedAutoLoadPage)
                                            }
                                        }
                                    )
                                }
                            }

                            if (loadedPages.size == totalPages && posts.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = i18n("- 沒有更多內容了 -"),
                                            color = colors.textDark.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (keepSystemBarsBackground) {
                    Spacer(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(colors.creamBackground)
                    )
                    Spacer(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(colors.creamBackground)
                    )
                }

                val progressOverlayTopPadding = if (keepSystemBarsBackground) {
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                } else {
                    0.dp
                }
                val progressOverlayBottomPadding = if (keepSystemBarsBackground) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 48.dp
                } else {
                    48.dp
                }
                val progressHintBottomPadding = if (keepSystemBarsBackground) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                } else {
                    0.dp
                }
                ThreadReaderProgressOverlay(
                    visible = !showSettingsPanel && state == ReaderState.Success,
                    showHint = showPageProgressHint,
                    listState = listState,
                    readerEntries = readerEntries,
                    pageByPid = pageByPid,
                    pageIndexBounds = pageIndexBounds,
                    totalPages = totalPages,
                    initialPage = initialPage,
                    slideBarModifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(
                            top = progressOverlayTopPadding,
                            bottom = progressOverlayBottomPadding,
                            end = 0.dp,
                        ),
                    hintModifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 0.dp, bottom = progressHintBottomPadding),
                )

                val showScrollJumpButton =
                    state == ReaderState.Success &&
                        readerEntries.isNotEmpty() &&
                        scrollButtonDisplayMode != ReaderScrollButtonDisplayMode.NEVER &&
                        (
                            scrollButtonDisplayMode == ReaderScrollButtonDisplayMode.ALWAYS ||
                                showScrollJumpButtonAfterSlide
                            )
                val scrollJumpBottomPadding = if (keepSystemBarsBackground) {
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 96.dp
                } else {
                    96.dp
                }
                if (!showSettingsPanel) {
                    ReaderScrollJumpButton(
                        visible = showScrollJumpButton,
                        pointsDown = scrollJumpButtonPointsDown,
                        onClick = {
                            scope.launch {
                                val targetIndex = scrollJumpTargetIndex()
                                if (targetIndex != null) {
                                    listState.animateScrollToItem(targetIndex)
                                } else {
                                    snackbarHostState.showSnackbar(i18n("目前無法定位跳轉位置"))
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = scrollJumpBottomPadding),
                    )
                }

                // Overlay menu
                ReaderOverlayMenu(
                    visible = showMenu,
                    title = threadInfo?.title ?: title,
                    isFavorited = isFavorited,
                    onBack = { saveCurrentHistoryAndPop() },
                    onCatalog = { scope.launch { drawerState.open() } },
                    onFavorite = { scope.launch { toggleFavoriteQuickWithFeedback() } },
                    onFavoriteLongPress = {
                        scope.launch {
                            val target = favoriteTarget()
                            favoriteDialogCategories = favoriteRepository.getCategories()
                            favoriteDialogOptions = favoriteRepository.getCollectionOptions()
                            val selection = favoriteRepository.getFavoriteLocationSelection(target)
                            favoriteDialogCategorySelection = selection.categoryIds
                            favoriteDialogSelection = selection.collectionIds
                            showFavoriteDialog = true
                        }
                    },
                    onShare = {
                        val url = YamiboRoute.Thread(tid).build()
                        shareText(platformContext, url, title)
                    },
                    onReply = {
                        val replyUrl = YamiboRoute.ThreadReply(tid, loadedPages.maxOrNull() ?: 1).build()
                        navigator.navigate(
                            IActionWebView(
                                title = i18n("發表回復"),
                                initialUrl = replyUrl,
                                successCondition = { url -> url.contains("mod=viewthread") && url.contains("tid=") },
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar(i18n("回復成功")) }
                                },
                            )
                        )
                    },
                    // Reload the current page
                    onRefresh = {
                        scope.launch {
                            state = ReaderState.Loading
                            loadPage(currentVisiblePageForAction(), forceRefresh = true)
                        }
                    },
                    onSettings = {
                        showSettingsPanel = true
                        showMenu = false
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Navigation Bar blocking scrim if settings are open
                if (showSettingsPanel) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showSettingsPanel = false }
                    )
                }

                val appSettingsRepo = LocalAppSettingsRepository.current

                NovelReaderSettingsPanel(
                    visible = showSettingsPanel,
                    appSettingsRepo = appSettingsRepo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        }
        YamiboSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
        )
    }

    if (showFavoriteDialog) {
        val target = favoriteTarget()
        FavoriteCollectionPickerDialog(
            categories = favoriteDialogCategories,
            options = favoriteDialogOptions,
            initialCategorySelection = favoriteDialogCategorySelection,
            initialCollectionSelection = favoriteDialogSelection,
            onDismiss = { showFavoriteDialog = false },
            onEdit = {
                showFavoriteDialog = false
                navigator.navigate(IFavoriteCategoryManageScreen())
            },
            onConfirm = { selectedCategories, selectedCollections ->
                scope.launch {
                    val existing = favoriteRepository.findFavoriteItem(target)
                    if (existing == null) {
                        favoriteRepository.saveFavorite(
                            target,
                            categoryIds = selectedCategories.toList(),
                            collectionIds = selectedCollections.toList()
                        )
                        showFavoriteDialog = false
                        favoriteRefreshToken += 1
                        if (target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncPromptEnabled.getValue()) {
                            showFavoriteAddSyncConfirm = true
                        } else {
                            completeSavedFavoriteSync(
                                syncToRemote = target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncDefault.getValue(),
                            )
                        }
                    } else if (selectedCategories.isEmpty() && selectedCollections.isEmpty()) {
                        showFavoriteDialog = false
                        pendingFavoriteRemovalSelection = favoriteRepository.getFavoriteLocationSelection(target)
                        pendingFavoriteRemovalSuccessMessage = i18n("已取消所有收藏")
                        if (appSettingsRepository.skipFavoriteRemovalConfirm.getValue()) {
                            if ((pendingFavoriteRemovalSelection?.paths?.size ?: 0) > 1) {
                                showFavoriteMultiPathDialog = true
                            } else {
                                maybePromptRemoteRemoval()
                            }
                        } else {
                            showFavoriteRemovalConfirm = true
                        }
                    } else {
                        favoriteRepository.setItemLocations(existing.id, selectedCategories, selectedCollections)
                        showFavoriteDialog = false
                        favoriteRefreshToken += 1
                        snackbarHostState.showSnackbar(i18n("已更新收藏路徑"))
                    }
                }
            }
        )
    }

    if (showFavoriteRemovalConfirm) {
        FavoriteRemovalConfirmDialog(
            onDismiss = {
                showFavoriteRemovalConfirm = false
                pendingFavoriteRemovalSelection = null
            },
            onConfirm = { skipNextTime ->
                appSettingsRepository.skipFavoriteRemovalConfirm.setValue(skipNextTime)
                showFavoriteRemovalConfirm = false
                scope.launch {
                    val selection = pendingFavoriteRemovalSelection
                    if ((selection?.paths?.size ?: 0) > 1) {
                        showFavoriteMultiPathDialog = true
                    } else {
                        pendingFavoriteRemovalSuccessMessage = i18n("已取消收藏")
                        maybePromptRemoteRemoval()
                    }
                }
            },
        )
    }

    if (showFavoriteAddSyncConfirm) {
        FavoriteAddSyncConfirmDialog(
            onDismiss = {
                showFavoriteAddSyncConfirm = false
                scope.launch { completeSavedFavoriteSync(syncToRemote = false) }
            },
            onConfirm = { rememberChoice, syncRemote ->
                showFavoriteAddSyncConfirm = false
                if (rememberChoice) {
                    appSettingsRepository.favoriteAddSyncPromptEnabled.setValue(false)
                    appSettingsRepository.favoriteAddSyncDefault.setValue(syncRemote)
                }
                scope.launch { completeSavedFavoriteSync(syncRemote) }
            },
        )
    }

    if (showFavoriteRemoveSyncConfirm) {
        FavoriteRemoveSyncConfirmDialog(
            onDismiss = {
                showFavoriteRemoveSyncConfirm = false
                pendingFavoriteRemovalSelection = null
            },
            onConfirm = { rememberChoice, syncRemote ->
                showFavoriteRemoveSyncConfirm = false
                if (rememberChoice) {
                    appSettingsRepository.favoriteRemoveSyncPromptEnabled.setValue(false)
                    appSettingsRepository.favoriteRemoveSyncDefault.setValue(syncRemote)
                }
                scope.launch { completeFavoriteRemoval(syncRemote) }
            },
        )
    }

    if (showFavoriteMultiPathDialog) {
        FavoriteMultiPathRemoveDialog(
            paths = pendingFavoriteRemovalSelection?.paths.orEmpty(),
            tip = i18n("tip：長按可詳細編輯收藏路徑"),
            onDismiss = {
                showFavoriteMultiPathDialog = false
                pendingFavoriteRemovalSelection = null
            },
            onRemoveAll = {
                showFavoriteMultiPathDialog = false
                pendingFavoriteRemovalSuccessMessage = i18n("已取消所有收藏")
                scope.launch {
                    maybePromptRemoteRemoval()
                }
            },
        )
    }

    catalogActionPost?.let { post ->
        val bookmarkEntry = postBookMarkEntries[post.pid.value.toLong()]
        val chapterState = postChapterStates[post.pid.value.toLong()]
        CatalogBookMarkActionDialog(
            bookmarked = bookmarkEntry?.bookmarked == true,
            read = chapterState?.read == true,
            hasProgress = chapterState?.hasProgress == true,
            onDismiss = { catalogActionPost = null },
            onToggleBookMark = {
                catalogActionPost = null
                scope.launch {
                    val next = bookmarkEntry?.bookmarked != true
                    bookMarkRepository.setBookmarked(
                        targetType = BookMarkRepository.TargetType.ThreadPost,
                        parentId = tid.value.toLong(),
                        targetId = post.pid.value.toLong(),
                        title = post.title.ifBlank { i18n("（無標題）") },
                        bookmarked = next,
                    )
                    reloadPostBookMarks()
                    snackbarHostState.showSnackbar(
                        if (next) i18n("已新增書籤") else i18n("已移除書籤"),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            onMarkRead = {
                catalogActionPost = null
                scope.launch {
                    chapterStateRepository.setRead(
                        targetType = ChapterStateRepository.TargetType.ThreadPost,
                        parentId = tid.value.toLong(),
                        targetId = post.pid.value.toLong(),
                        title = post.title.ifBlank { i18n("（無標題）") },
                        read = true,
                    )
                    reloadPostChapterStates()
                    snackbarHostState.showSnackbar(i18n("已標為已讀"), duration = SnackbarDuration.Short)
                }
            },
            onMarkUnread = {
                catalogActionPost = null
                scope.launch {
                    chapterStateRepository.setRead(
                        targetType = ChapterStateRepository.TargetType.ThreadPost,
                        parentId = tid.value.toLong(),
                        targetId = post.pid.value.toLong(),
                        title = post.title.ifBlank { i18n("（無標題）") },
                        read = false,
                    )
                    reloadPostChapterStates()
                    snackbarHostState.showSnackbar(i18n("已標為未讀"), duration = SnackbarDuration.Short)
                }
            },
            onClearHistory = {
                catalogActionPost = null
                scope.launch {
                    chapterStateRepository.clearParent(
                        targetType = ChapterStateRepository.TargetType.ThreadPost,
                        parentId = tid.value.toLong(),
                    )
                    readHistoryRepo.deleteHistory(tid, threadType, authorId)
                    reloadPostChapterStates()
                    snackbarHostState.showSnackbar(i18n("已清除全部閱讀紀錄"), duration = SnackbarDuration.Short)
                }
            },
        )
    }
}

@Composable
private fun ReaderCatalogPanelWithPosition(
    listState: LazyListState,
    readerEntries: List<ReaderListEntry>,
    pageByPid: Map<Long, Int>,
    initialPage: Int,
    totalPages: Int,
    loadedPostsByPage: Map<Int, List<Post>>,
    bookmarkedPostIds: Set<Long>,
    readPostIds: Set<Long>,
    chapterStates: Map<Long, ChapterStateRepository.Entry>,
    onPageOrPostClick: (Int, Post?) -> Unit,
    onPostLongPress: (Post) -> Unit,
) {
    val currentPosition by remember(listState, readerEntries, pageByPid, initialPage) {
        derivedStateOf {
            val currentEntry = readerEntries.getOrNull(listState.firstVisibleItemIndex)
            ReaderCatalogCurrentPosition(
                page = currentEntry?.let { pageByPid[it.post.pid.value.toLong()] } ?: initialPage,
                pid = currentEntry?.post?.pid,
            )
        }
    }

    ReaderCatalogPanel(
        totalPages = totalPages,
        loadedPostsByPage = loadedPostsByPage,
        currentPage = currentPosition.page,
        currentPid = currentPosition.pid,
        bookmarkedPostIds = bookmarkedPostIds,
        readPostIds = readPostIds,
        chapterStates = chapterStates,
        onPageOrPostClick = onPageOrPostClick,
        onPostLongPress = onPostLongPress,
    )
}

@Composable
private fun ThreadReaderProgressOverlay(
    visible: Boolean,
    showHint: Boolean,
    listState: LazyListState,
    readerEntries: List<ReaderListEntry>,
    pageByPid: Map<Long, Int>,
    pageIndexBounds: Map<Int, IntRange>,
    totalPages: Int,
    initialPage: Int,
    slideBarModifier: Modifier,
    hintModifier: Modifier,
) {
    val progress by remember(listState, readerEntries, pageByPid, pageIndexBounds, totalPages, initialPage) {
        derivedStateOf {
            calculateReaderPageProgress(
                listState = listState,
                readerEntries = readerEntries,
                pageByPid = pageByPid,
                pageIndexBounds = pageIndexBounds,
                totalPages = totalPages,
                initialPage = initialPage,
            )
        }
    }
    val currentProgress = progress
    if (!visible || currentProgress == null) return

    ReaderPageProgressSlideBar(
        progress = currentProgress,
        modifier = slideBarModifier,
    )
    ReaderPageProgressHint(
        progress = currentProgress,
        visible = showHint,
        modifier = hintModifier,
    )
}

private fun calculateReaderPageProgress(
    listState: LazyListState,
    readerEntries: List<ReaderListEntry>,
    pageByPid: Map<Long, Int>,
    pageIndexBounds: Map<Int, IntRange>,
    totalPages: Int,
    initialPage: Int,
): ReaderPageProgress? {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (readerEntries.isEmpty() || visibleItems.isEmpty()) return null

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredItem = visibleItems
        .mapNotNull { item ->
            readerEntries.getOrNull(item.index)
                ?.takeIf { it.isScrollAnchor }
                ?.let { item to it }
        }
        .minByOrNull { (item, _) ->
            when {
                viewportCenter < item.offset -> item.offset - viewportCenter
                viewportCenter > item.offset + item.size -> viewportCenter - (item.offset + item.size)
                else -> 0
            }
        }
        ?: visibleItems
            .mapNotNull { item -> readerEntries.getOrNull(item.index)?.let { item to it } }
            .minByOrNull { (item, _) ->
                when {
                    viewportCenter < item.offset -> item.offset - viewportCenter
                    viewportCenter > item.offset + item.size -> viewportCenter - (item.offset + item.size)
                    else -> 0
                }
            }
        ?: return null

    val item = centeredItem.first
    val entry = centeredItem.second
    val postPage = pageByPid[entry.post.pid.value.toLong()] ?: initialPage
    val postBounds = pageIndexBounds[postPage] ?: return ReaderPageProgress(
        page = postPage,
        totalPages = totalPages,
        fraction = 0f,
    )
    val pagePostCount = (postBounds.last - postBounds.first + 1).coerceAtLeast(1)
    val relativePostIndex = (entry.postIndex - postBounds.first).coerceIn(0, pagePostCount - 1)
    val itemRatio = ((viewportCenter - item.offset).toFloat() / item.size.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)

    return ReaderPageProgress(
        page = postPage,
        totalPages = totalPages,
        fraction = ((relativePostIndex + itemRatio) / pagePostCount.toFloat()).coerceIn(0f, 1f),
    )
}

@Composable
private fun CatalogBookMarkActionDialog(
    bookmarked: Boolean,
    read: Boolean,
    hasProgress: Boolean,
    onDismiss: () -> Unit,
    onToggleBookMark: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkUnread: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(i18n("閱讀標記"), color = colors.textStrong) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogActionRow(if (bookmarked) i18n("移除書籤") else i18n("新增書籤"), onToggleBookMark)
                if (hasProgress) {
                    CatalogActionRow(i18n("標為已讀"), onMarkRead)
                    CatalogActionRow(i18n("標為未讀"), onMarkUnread)
                } else {
                    CatalogActionRow(if (read) i18n("標為未讀") else i18n("標為已讀"), if (read) onMarkUnread else onMarkRead)
                }
                CatalogActionRow(i18n("清除全部閱讀紀錄"), onClearHistory)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(i18n("取消"), color = colors.textStrong) }
        },
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun CatalogActionRow(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = colors.creamBackground,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            color = colors.textDark,
        )
    }
}
