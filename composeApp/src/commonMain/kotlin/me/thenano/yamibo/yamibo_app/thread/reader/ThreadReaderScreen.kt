package me.thenano.yamibo.yamibo_app.thread.reader

import me.thenano.yamibo.yamibo_app.i18n.i18n

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.*
import me.thenano.yamibo.yamibo_app.favorite.*
import me.thenano.yamibo.yamibo_app.components.tracking.ReadingTimeTracker
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkContext
import me.thenano.yamibo.yamibo_app.repository.ContentCoverRepository
import me.thenano.yamibo.yamibo_app.repository.contentcover.ThreadCoverResolver
import me.thenano.yamibo.yamibo_app.repository.BookMarkRepository as BookMarkRepository
import me.thenano.yamibo.yamibo_app.repository.ChapterStateRepository as ChapterStateRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.repository.download.DownloadStatus
import me.thenano.yamibo.yamibo_app.repository.download.ThreadPageDownloadKey
import me.thenano.yamibo.yamibo_app.components.systembars.SystemBarsEffect
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.components.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadErrorContent
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadLoadingSkeleton
import me.thenano.yamibo.yamibo_app.profile.settings.backup.IBackupSettingsScreen
import me.thenano.yamibo.yamibo_app.repository.contentcover.toCoverTargetType
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageActionMessageListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageClickListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageDoubleClickListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageSetCatalogCoverLabel
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageSetCatalogCoverListener
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

private const val LONG_READER_HTML_THRESHOLD = 16_000
internal const val MAX_READER_TEXT_SEGMENT_CHARS = 4_000

internal fun splitLongReaderTextBlock(block: HtmlBlock.Text): List<HtmlBlock.Text> {
    val text = block.annotatedString.text
    if (text.length <= MAX_READER_TEXT_SEGMENT_CHARS) return listOf(block)

    return buildList {
        var start = 0
        var chunkIndex = 0
        while (start < text.length) {
            val hardEnd = (start + MAX_READER_TEXT_SEGMENT_CHARS).coerceAtMost(text.length)
            val preferredBreak = if (hardEnd < text.length) text.lastIndexOf('\n', hardEnd - 1) else -1
            val end = preferredBreak
                .takeIf { it > start + MAX_READER_TEXT_SEGMENT_CHARS / 2 }
                ?.plus(1)
                ?: hardEnd
            add(
                block.copy(
                    annotatedString = block.annotatedString.subSequence(start, end),
                    anchorId = "${block.anchorId}-$chunkIndex",
                )
            )
            start = end
            chunkIndex++
        }
    }
}

private fun HtmlBlock.readerTextLength(): Int = when (this) {
    is HtmlBlock.Text -> annotatedString.length
    is HtmlBlock.Code -> codeText.length
    is HtmlBlock.Quote -> contentBlocks.sumOf { it.readerTextLength() }
    is HtmlBlock.Collapse -> contentBlocks.sumOf { it.readerTextLength() }
    is HtmlBlock.Locked -> contentBlocks.sumOf { it.readerTextLength() }
    is HtmlBlock.Table -> rows.sumOf { row ->
        row.cells.sumOf { cell -> cell.blocks.sumOf { it.readerTextLength() } }
    }
    else -> 0
}

private fun groupReaderBlocks(blocks: List<HtmlBlock>): List<List<HtmlBlock>> {
    val groups = mutableListOf<List<HtmlBlock>>()
    val current = mutableListOf<HtmlBlock>()
    var currentLength = 0

    fun flush() {
        if (current.isEmpty()) return
        groups += current.toList()
        current.clear()
        currentLength = 0
    }

    blocks.forEach { block ->
        val blockLength = block.readerTextLength()
        if (current.isNotEmpty() && currentLength + blockLength > MAX_READER_TEXT_SEGMENT_CHARS) flush()
        current += block
        currentLength += blockLength
        if (currentLength >= MAX_READER_TEXT_SEGMENT_CHARS) flush()
    }
    flush()
    return groups
}

internal fun splitLongReaderBlock(block: HtmlBlock): List<HtmlBlock> = when (block) {
    is HtmlBlock.Text -> splitLongReaderTextBlock(block)
    is HtmlBlock.Code -> block.codeText.chunked(MAX_READER_TEXT_SEGMENT_CHARS).mapIndexed { index, text ->
        block.copy(codeText = text, anchorId = "${block.anchorId}-$index")
    }
    is HtmlBlock.Quote -> groupReaderBlocks(block.contentBlocks.flatMap(::splitLongReaderBlock)).mapIndexed { index, blocks ->
        block.copy(contentBlocks = blocks, anchorId = "${block.anchorId}-$index")
    }
    is HtmlBlock.Collapse -> groupReaderBlocks(block.contentBlocks.flatMap(::splitLongReaderBlock)).mapIndexed { index, blocks ->
        block.copy(contentBlocks = blocks, anchorId = "${block.anchorId}-$index")
    }
    is HtmlBlock.Locked -> groupReaderBlocks(block.contentBlocks.flatMap(::splitLongReaderBlock)).mapIndexed { index, blocks ->
        block.copy(contentBlocks = blocks, anchorId = "${block.anchorId}-$index")
    }
    else -> listOf(block)
}

private fun buildReaderBodySegments(post: Post, contentHtml: String): List<ReaderBodySegment>? {
    val shouldSegmentImages = post.images.size >= 6
    val shouldSegmentLongText = contentHtml.length >= LONG_READER_HTML_THRESHOLD
    if (!shouldSegmentImages && !shouldSegmentLongText) return null

    val blocks = normalizeHtmlBlocks(HtmlParser.parseHtml(contentHtml))
    if (blocks.isEmpty()) return null

    val segments = mutableListOf<ReaderBodySegment>()
    val currentBlocks = mutableListOf<HtmlBlock>()
    var currentTextLength = 0

    fun flushCurrentBlocks() {
        if (currentBlocks.isEmpty()) return
        val firstBlock = currentBlocks.first()
        segments += ReaderBodySegment(
            blocks = currentBlocks.toList(),
            anchorBlockId = firstBlock.anchorId.takeIf { it.isNotBlank() },
            anchorBlockType = if (currentBlocks.size == 1) firstBlock::class.simpleName else "Mixed",
        )
        currentBlocks.clear()
        currentTextLength = 0
    }

    val segmentableBlocks = blocks.flatMap { block ->
        if (shouldSegmentLongText) splitLongReaderBlock(block) else listOf(block)
    }

    segmentableBlocks.forEach { block ->
        if (block is HtmlBlock.Image && shouldSegmentImages) {
            flushCurrentBlocks()
            segments += ReaderBodySegment(
                blocks = listOf(block),
                anchorBlockId = block.anchorId.takeIf { it.isNotBlank() },
                anchorBlockType = "Image",
            )
        } else {
            val blockTextLength = block.readerTextLength()
            if (currentBlocks.isNotEmpty() && currentTextLength + blockTextLength > MAX_READER_TEXT_SEGMENT_CHARS) {
                flushCurrentBlocks()
            }
            currentBlocks += block
            currentTextLength += blockTextLength
            if (currentTextLength >= MAX_READER_TEXT_SEGMENT_CHARS) flushCurrentBlocks()
        }
    }
    flushCurrentBlocks()

    val imageSegmentCount = segments.count { it.anchorBlockType == "Image" }
    return when {
        shouldSegmentLongText && segments.size > 1 -> segments
        shouldSegmentImages && imageSegmentCount >= 6 && segments.size > 2 -> segments
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThreadReaderScreen(
    tid: ThreadId,
    title: String,
    threadType: ReadHistoryRepository.ThreadEntryType = ReadHistoryRepository.ThreadEntryType.Normal,
    authorId: UserId? = null,
    initialPage: Int = 1,
    targetPid: PostId? = null,
    catalogCoverTargetType: ContentCoverRepository.TargetType? = null,
    catalogCoverTargetId: Long? = null,
    catalogTagId: TagId? = null,
    catalogTagName: String? = null,
    catalogTagPage: Int? = null,
    catalogRssSubscriptionId: Long? = null,
    catalogRssTitle: String? = null,
    catalogRssQuery: String? = null,
    catalogRssPage: Int? = null,
) {
    DebugRecomposeProbe("ThreadReaderScreen", tid.value.toString())
    val colors = YamiboTheme.colors
    val appSettingsRepository = LocalAppSettingsRepository.current
    val novelSettingsRepository = LocalNovelReaderSettingsRepository.current
    val threadRepository = LocalThreadRepository.current
    val downloadRepository = LocalDownloadRepository.current
    val downloadQueue by downloadRepository.queue.collectAsState()
    val favoriteRepository = LocalFavoriteRepository.current
    val favoriteSyncRepository = LocalFavoriteSyncRepository.current
    val readHistoryRepo = LocalReadHistoryRepository.current
    val contentCoverRepository = LocalContentCoverRepository.current
    val bookMarkRepository = LocalBookMarkRepository.current
    val chapterStateRepository = LocalChapterStateRepository.current
    ReadingTimeTracker()
    val navigator = LocalNavigator.current
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val progressCoordinator = remember(tid, chapterStateRepository, scope) {
        ReaderProgressCoordinator(
            repository = chapterStateRepository,
            parentId = tid.value.toLong(),
            scope = scope,
        )
    }
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
    var showDownloadSheet by remember { mutableStateOf(false) }
    var downloadSheetPage by remember(tid, authorId) { mutableIntStateOf(initialPage) }
    var showRefreshDownloadedDialog by remember { mutableStateOf(false) }
    var showDownloadedLastPageWarning by remember { mutableStateOf(false) }
    var dismissedUpdateWarningPages by remember(tid, authorId) { mutableStateOf(emptySet<Int>()) }
    var scrollJumpButtonPointsDown by remember { mutableStateOf(false) }
    var showScrollJumpButtonAfterSlide by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val authRepo = LocalAuthRepository.current
    val snackbarHostState = remember { SnackbarHostState() }

    val readerUsesBrownSystemBar = showMenu || showSettingsPanel || drawerState.isOpen
    SystemBarsEffect(
        statusBarColor = if (readerUsesBrownSystemBar) colors.brownDeep else colors.creamBackground,
        navigationBarColor = colors.creamBackground,
        priority = 20,
        darkStatusBarIcons = !readerUsesBrownSystemBar,
        darkNavigationBarIcons = true,
    )

    var hasRestoredPosition by remember { mutableStateOf(false) }
    var pendingSavedPosition by remember(tid, targetPid) { mutableStateOf<ThreadReadingHistory?>(null) }
    var isRestoringSavedPosition by remember { mutableStateOf(false) }
    var canPersistReadingState by remember(tid, targetPid) { mutableStateOf(false) }
    var pendingTargetPid by remember(tid, targetPid) { mutableStateOf(targetPid?.value?.toLong()) }
    var lastReadingSnapshot by remember(tid) {
        mutableStateOf<Pair<ThreadReadingHistory?, List<ChapterStateRepository.ProgressUpdate>>?>(null)
    }
    val threadHistoryOrigin = when {
        catalogTagId != null -> ReadHistoryRepository.ThreadHistoryOrigin.TagCatalog
        catalogRssSubscriptionId != null -> ReadHistoryRepository.ThreadHistoryOrigin.RssCatalog
        else -> ReadHistoryRepository.ThreadHistoryOrigin.Direct
    }

    /** Extract image URL for thread avatar based on forum type */
    var coverUrl by remember { mutableStateOf<String?>(null) }
    var manualCoverUrlOverride by remember(tid) { mutableStateOf<String?>(null) }
    val coverKey = remember(tid, threadType) {
        ContentCoverRepository.Key(threadType.toCoverTargetType(), tid.value.toLong())
    }
    val catalogCoverKey = remember(catalogCoverTargetType, catalogCoverTargetId) {
        if (catalogCoverTargetType == ContentCoverRepository.TargetType.TagManga ||
            catalogCoverTargetType == ContentCoverRepository.TargetType.RssSearch
        ) {
            catalogCoverTargetId?.let { ContentCoverRepository.Key(catalogCoverTargetType, it) }
        } else {
            null
        }
    }
    val catalogCoverLabel = when (catalogCoverKey?.targetType) {
        ContentCoverRepository.TargetType.TagManga -> i18n("設為標籤封面")
        ContentCoverRepository.TargetType.RssSearch -> i18n("設為RSS封面")
        else -> null
    }
    val catalogCoverSavedMessage = when (catalogCoverKey?.targetType) {
        ContentCoverRepository.TargetType.TagManga -> i18n("已設為標籤封面")
        ContentCoverRepository.TargetType.RssSearch -> i18n("已設為RSS封面")
        else -> null
    }
    val canonicalCover by contentCoverRepository.observeCover(coverKey).collectAsState(null)
    val threadCoverResolver = remember(threadRepository) { ThreadCoverResolver(threadRepository) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteDialogCategories by remember {
        mutableStateOf<List<me.thenano.yamibo.yamibo_app.repository.FavoriteStoreRepository.FavoriteCategory>>(emptyList())
    }
    var favoriteDialogCategorySelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogOptions by remember {
        mutableStateOf<List<me.thenano.yamibo.yamibo_app.repository.FavoriteStoreRepository.FavoriteCollectionOption>>(emptyList())
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
    var catalogActionPost by remember { mutableStateOf<Post?>(null) }
    var observedDownloadedPages by remember { mutableStateOf<Set<Int>?>(null) }

    suspend fun reloadPostBookMarks() {
        postBookMarkEntries = bookMarkRepository
            .getEntriesByParent(BookMarkRepository.TargetType.ThreadPost, tid.value.toLong())
            .associateBy { it.targetId }
    }

    LaunchedEffect(tid) {
        reloadPostBookMarks()
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

    LaunchedEffect(canonicalCover?.resolvedUrl) {
        canonicalCover?.resolvedUrl?.let { coverUrl = it }
    }

    LaunchedEffect(tid, threadType, loadedPostsByPage[1]) {
        if (threadType != ReadHistoryRepository.ThreadEntryType.Normal || loadedPostsByPage[1] == null) {
            return@LaunchedEffect
        }
        threadCoverResolver.resolve(tid)?.let { resolved ->
            contentCoverRepository.setAutomaticCover(coverKey, resolved)
            catalogCoverKey?.let { contentCoverRepository.setAutomaticCover(it, resolved) }
        }
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
                    onSuccess = { scope.launch { snackbarHostState.showSnackbar(i18n("回復已發表，請刷新頁面查看")) } },
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
    val progressEntryRangeByPid = remember(readerEntries) {
        readerEntries
            .withIndex()
            .filter { (_, entry) ->
                when (entry.kind) {
                    ReaderEntryKind.WholePost,
                    ReaderEntryKind.SegmentedHeader,
                    ReaderEntryKind.SegmentedBody,
                    ReaderEntryKind.SegmentedFooter -> true
                    else -> false
                }
            }
            .groupBy { it.value.post.pid.value.toLong() }
            .mapValues { (_, entries) -> entries.first().index..entries.last().index }
    }
    val contentPostEndingAtIndex = remember(readerEntries, progressEntryRangeByPid) {
        buildMap {
            progressEntryRangeByPid.forEach { (postId, range) ->
                readerEntries.getOrNull(range.last)?.post?.let { post ->
                    put(range.last, postId to post)
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

        fun distanceFromViewportCenter(item: LazyListItemInfo): Int =
            when {
                viewportCenter < item.offset -> item.offset - viewportCenter
                viewportCenter > item.offset + item.size -> viewportCenter - (item.offset + item.size)
                else -> 0
            }

        val visibleEntries = visibleItems.mapNotNull { item ->
            readerEntries.getOrNull(item.index)?.let { entry -> item to entry }
        }
        val centerEntry = visibleEntries
            .filter { (_, entry) -> entry.isScrollAnchor }
            .minByOrNull { (item, _) -> distanceFromViewportCenter(item) }
            ?: visibleEntries.minByOrNull { (item, _) -> distanceFromViewportCenter(item) }
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
            historyOrigin = threadHistoryOrigin,
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

    fun canWriteReadingState(): Boolean {
        return canPersistReadingState ||
            (state == ReaderState.Success && readerEntries.isNotEmpty() && !isRestoringSavedPosition)
    }

    fun progressUpdateForVisiblePost(
        postId: Long,
        visibleItems: List<LazyListItemInfo>,
        visibleByIndex: Map<Int, LazyListItemInfo>,
        viewportTop: Int,
        viewportBottom: Int,
        allowPassedShortPost: Boolean,
    ): ChapterStateRepository.ProgressUpdate? {
        val range = progressEntryRangeByPid[postId] ?: return null
        val visibleItem = visibleItems.firstOrNull { it.index in range } ?: return null
        val knownHeights = range.mapNotNull { index ->
            val entry = readerEntries.getOrNull(index) ?: return@mapNotNull null
            progressCoordinator.itemHeight(entry.key) ?: visibleByIndex[index]?.size
        }
        val estimatedHeight = knownHeights.average().toInt().coerceAtLeast(1)
        var totalHeight = 0
        var heightBeforeVisibleItem = 0
        for (index in range) {
            val entry = readerEntries.getOrNull(index) ?: return null
            val height = progressCoordinator.itemHeight(entry.key)
                ?: visibleByIndex[index]?.size
                ?: estimatedHeight
            if (index < visibleItem.index) heightBeforeVisibleItem += height
            totalHeight += height
        }
        val post = readerEntries[range.first].post
        val geometry = ReaderProgressGeometry(
            postId = postId,
            title = post.title,
            top = visibleItem.offset - heightBeforeVisibleItem,
            bottom = visibleItem.offset - heightBeforeVisibleItem + totalHeight,
        )
        val result = calculateReaderProgress(
            geometry = geometry,
            viewportTop = viewportTop,
            viewportBottom = viewportBottom,
            allowPassedShortPost = allowPassedShortPost,
        ) ?: return null
        if (progressCoordinator.isRead(postId)) return null
        return progressCoordinator.update(
            postId = postId,
            title = post.title.ifBlank { i18n("（無標題）") },
            progressPercent = result.progressPercent,
            read = result.read,
        )
    }

    fun currentChapterUpdates(): List<ChapterStateRepository.ProgressUpdate> {
        if (posts.isEmpty() || readerEntries.isEmpty()) return emptyList()
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return emptyList()

        val viewportTop = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding
        val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
        val visibleByIndex = visibleItems.associateBy { it.index }
        val candidatePostIds = LinkedHashSet<Long>()
        visibleItems.forEach { item ->
            val entry = readerEntries.getOrNull(item.index) ?: return@forEach
            val postId = entry.post.pid.value.toLong()
            val range = progressEntryRangeByPid[postId] ?: return@forEach
            if (item.index in range) candidatePostIds += postId
        }

        return candidatePostIds.mapNotNull { postId ->
            progressUpdateForVisiblePost(
                postId = postId,
                visibleItems = visibleItems,
                visibleByIndex = visibleByIndex,
                viewportTop = viewportTop,
                viewportBottom = viewportBottom,
                allowPassedShortPost = false,
            )
        }
    }

    suspend fun persistReadingSnapshot(
        history: ThreadReadingHistory?,
        progressUpdates: List<ChapterStateRepository.ProgressUpdate>,
    ) {
        if (history != null) {
            try {
                readHistoryRepo.savePosition(history)
            } catch (error: Exception) {
                println("READER_PERSIST|history|${error::class.simpleName}|${error.message.orEmpty()}")
            }
            try {
                val historyCover = history.threadCover ?: coverUrl
                if (catalogTagId != null && catalogTagName != null && catalogTagPage != null) {
                    readHistoryRepo.saveTagCatalogThreadHistory(
                        ReadHistoryRepository.TagCatalogReadingHistory(
                            tagId = catalogTagId,
                            tagName = catalogTagName,
                            tagPage = catalogTagPage,
                            threadId = history.threadId,
                            threadTitle = history.threadName,
                            threadPage = history.page,
                            postId = history.postId,
                            postTitle = history.postTitle,
                            authorId = history.authorId,
                            anchorPostId = history.anchorPostId,
                            anchorPostRatio = history.anchorPostRatio,
                            anchorBlockId = history.anchorBlockId,
                            anchorBlockType = history.anchorBlockType,
                            anchorBlockRatio = history.anchorBlockRatio,
                            viewportHeight = history.viewportHeight,
                            firstVisibleItemIndex = history.firstVisibleItemIndex,
                            firstVisibleItemOffset = history.firstVisibleItemOffset,
                            lastVisitTime = history.lastVisitTime,
                            coverUrl = historyCover,
                        )
                    )
                }
                if (catalogRssSubscriptionId != null && catalogRssTitle != null && catalogRssPage != null) {
                    readHistoryRepo.saveRssCatalogThreadHistory(
                        ReadHistoryRepository.RssCatalogReadingHistory(
                            subscriptionId = catalogRssSubscriptionId,
                            subscriptionTitle = catalogRssTitle,
                            subscriptionQuery = catalogRssQuery ?: catalogRssTitle,
                            subscriptionPage = catalogRssPage,
                            threadId = history.threadId,
                            threadTitle = history.threadName,
                            threadPage = history.page,
                            postId = history.postId,
                            postTitle = history.postTitle,
                            authorId = history.authorId,
                            anchorPostId = history.anchorPostId,
                            anchorPostRatio = history.anchorPostRatio,
                            anchorBlockId = history.anchorBlockId,
                            anchorBlockType = history.anchorBlockType,
                            anchorBlockRatio = history.anchorBlockRatio,
                            viewportHeight = history.viewportHeight,
                            firstVisibleItemIndex = history.firstVisibleItemIndex,
                            firstVisibleItemOffset = history.firstVisibleItemOffset,
                            lastVisitTime = history.lastVisitTime,
                            coverUrl = historyCover,
                        )
                    )
                }
            } catch (error: Exception) {
                println("READER_PERSIST|catalog_history|${error::class.simpleName}|${error.message.orEmpty()}")
            }
        }
        try {
            progressCoordinator.applyProgress(progressUpdates)
        } catch (error: Exception) {
            println("READER_PERSIST|chapter|${error::class.simpleName}|${error.message.orEmpty()}")
        }
    }

    suspend fun persistCurrentReadingState() {
        if (!canWriteReadingState()) return
        val history = runCatching(::buildHistory).getOrNull()
        val progressUpdates = runCatching(::currentChapterUpdates).getOrDefault(emptyList())
        if (history != null || progressUpdates.isNotEmpty()) {
            lastReadingSnapshot = history to progressUpdates
        }
        persistReadingSnapshot(history, progressUpdates)
    }

    fun currentVisiblePageForAction(): Int {
        val currentIndex = visibleAnchorEntryIndex() ?: listState.firstVisibleItemIndex
        val currentEntry = readerEntries.getOrNull(currentIndex) ?: return initialPage
        return pageByPid[currentEntry.post.pid.value.toLong()] ?: initialPage
    }

    LaunchedEffect(loadedPages, totalPages, tid, authorId, readerEntries, downloadQueue, dismissedUpdateWarningPages) {
        snapshotFlow { currentVisiblePageForAction() }
            .distinctUntilChanged()
            .collect { currentPage ->
                showDownloadedLastPageWarning =
                    currentPage == totalPages &&
                        currentPage !in dismissedUpdateWarningPages &&
                        downloadQueue.any {
                            it.key == ThreadPageDownloadKey(tid.value, currentPage, authorId?.value) &&
                                it.status == DownloadStatus.UpdateAvailable
                        }
            }
    }

    LaunchedEffect(downloadQueue, tid, authorId) {
        val downloadedPages = downloadQueue.mapNotNull {
            val key = it.key as? ThreadPageDownloadKey ?: return@mapNotNull null
            if (key.tid == tid.value && key.authorId == authorId?.value && it.status == DownloadStatus.Downloaded) {
                key.page
            } else {
                null
            }
        }.toSet()
        val previous = observedDownloadedPages
        if (previous != null) {
            val newlyDownloaded = (downloadedPages - previous).minOrNull()
            if (newlyDownloaded != null) {
                snackbarHostState.showSnackbar(i18n("下載完成：第 {} 頁", newlyDownloaded))
            }
        }
        observedDownloadedPages = downloadedPages
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

    suspend fun scrollToChapterTarget(index: Int, pid: Long) {
        val progressPercent = progressCoordinator.chapterStates.value[pid]
            ?.progressPercent
            ?.takeIf { it in 1..99 }
            ?: 0
        if (progressPercent <= 0) {
            listState.scrollToItem(index)
            return
        }

        val range = progressEntryRangeByPid[pid]
        if (range == null) {
            listState.scrollToItem(index)
            return
        }
        val entryCount = (range.last - range.first + 1).coerceAtLeast(1)
        val targetProgress = (progressPercent / 100f) * entryCount
        val targetEntryOffset = targetProgress.toInt().coerceIn(0, entryCount - 1)
        val targetIndex = (range.first + targetEntryOffset).coerceIn(range.first, range.last)
        val intraEntryProgress = (targetProgress - targetEntryOffset).coerceIn(0f, 0.95f)

        listState.scrollToItem(targetIndex)
        withFrameNanos { }
        val itemHeight = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == targetIndex }
            ?.size
            ?: 0
        val offset = (itemHeight * intraEntryProgress).toInt().coerceAtLeast(0)
        listState.scrollToItem(targetIndex, offset)
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

    fun saveCurrentHistoryAndPop() {
        if (!canWriteReadingState()) {
            navigator.pop()
            return
        }
        scope.launch {
            persistCurrentReadingState()
            navigator.pop()
        }
    }

    fun applySelectedCover(imageUrl: String) {
        val resolvedCoverUrl = resolveValidCoverUrl(imageUrl) ?: return
        manualCoverUrlOverride = resolvedCoverUrl
        coverUrl = resolvedCoverUrl
        scope.launch {
            contentCoverRepository.setManualCover(coverKey, resolvedCoverUrl)
            if (canPersistReadingState) {
                buildHistory()?.copy(threadCover = resolvedCoverUrl)?.let { history ->
                    try {
                        readHistoryRepo.savePosition(history)
                        progressCoordinator.applyProgress(currentChapterUpdates())
                    } catch (_: Exception) {
                    }
                }
            }
            try {
                favoriteRepository.syncFavoriteMetadata(favoriteTarget(coverOverride = resolvedCoverUrl))
            } catch (_: Exception) {
            }
            snackbarHostState.showSnackbar(i18n("已設為封面"))
        }
    }

    fun applySelectedCatalogCover(imageUrl: String) {
        val key = catalogCoverKey ?: return
        val savedMessage = catalogCoverSavedMessage ?: return
        val resolvedCoverUrl = resolveValidCoverUrl(imageUrl) ?: return
        scope.launch {
            val saved = contentCoverRepository.setManualCover(key, resolvedCoverUrl)
            if (saved) {
                snackbarHostState.showSnackbar(savedMessage)
            }
        }
    }

    suspend fun loadPage(page: Int, forceRefresh: Boolean = false, autoTriggered: Boolean = false): Boolean {
        if (!forceRefresh && page in loadedPages) return true
        isLoadingNextPage = true
        var loadSucceeded = false

        fun markPageLoadSucceeded() {
            if (state is ReaderState.Loading || page == initialPage || page == 1) {
                state = ReaderState.Success
            }
        }

        suspend fun loadFromDownload(): Boolean {
            val downloadKey = ThreadPageDownloadKey(tid.value, page, authorId?.value)
            val downloaded = downloadRepository.getDownloadedPage(downloadKey)
                ?: return false
            loadedPostsByPage[page] = downloaded.posts
            rebuildPosts()
            threadInfo = downloaded.thread
            totalPages = downloaded.pageNav?.totalPages ?: 1
            loadedPages = loadedPages + page
            failedAutoLoadPages.remove(page)
            markPageLoadSucceeded()
            return true
        }

        fun loadFromCache(): Boolean {
            val cached = threadRepository.getCachedThread(tid, authorId, page)
            if (cached != null) {
                loadedPostsByPage[page] = cached.posts
                rebuildPosts()
                threadInfo = cached.thread
                totalPages = cached.pageNav?.totalPages ?: 1
                loadedPages = loadedPages + page
                failedAutoLoadPages.remove(page)
                markPageLoadSucceeded()
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
            markPageLoadSucceeded()
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
            if (loadFromDownload()) {
                isLoadingNextPage = false
                return true
            }

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

    fun ReadHistoryRepository.TagCatalogReadingHistory.toThreadReadingHistory(): ThreadReadingHistory {
        return ThreadReadingHistory(
            threadType = ReadHistoryRepository.ThreadEntryType.Normal,
            threadName = threadTitle,
            threadId = threadId,
            threadCover = coverUrl,
            lastUpdatedTime = null,
            forumName = null,
            forumId = null,
            authorId = authorId,
            page = threadPage,
            postId = postId,
            postTitle = postTitle,
            anchorPostId = anchorPostId,
            anchorPostRatio = anchorPostRatio,
            anchorBlockId = anchorBlockId,
            anchorBlockType = anchorBlockType,
            anchorBlockRatio = anchorBlockRatio,
            globalScrollY = null,
            viewportHeight = viewportHeight,
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemOffset = firstVisibleItemOffset,
            historyOrigin = ReadHistoryRepository.ThreadHistoryOrigin.TagCatalog,
            lastVisitTime = lastVisitTime,
        )
    }

    fun ReadHistoryRepository.RssCatalogReadingHistory.toThreadReadingHistory(): ThreadReadingHistory {
        return ThreadReadingHistory(
            threadType = ReadHistoryRepository.ThreadEntryType.Normal,
            threadName = threadTitle,
            threadId = threadId,
            threadCover = coverUrl,
            lastUpdatedTime = null,
            forumName = null,
            forumId = null,
            authorId = authorId,
            page = threadPage,
            postId = postId,
            postTitle = postTitle,
            anchorPostId = anchorPostId,
            anchorPostRatio = anchorPostRatio,
            anchorBlockId = anchorBlockId,
            anchorBlockType = anchorBlockType,
            anchorBlockRatio = anchorBlockRatio,
            globalScrollY = null,
            viewportHeight = viewportHeight,
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemOffset = firstVisibleItemOffset,
            historyOrigin = ReadHistoryRepository.ThreadHistoryOrigin.RssCatalog,
            lastVisitTime = lastVisitTime,
        )
    }

    // Initial load + position restore
    LaunchedEffect(tid, initialPage, targetPid) {
        loadPage(initialPage)

        /** Restore position from history if no explicit targetPid */
        if (!hasRestoredPosition && targetPid != null && threadHistoryOrigin != ReadHistoryRepository.ThreadHistoryOrigin.Direct) {
            try {
                val catalogPosition = when (threadHistoryOrigin) {
                    ReadHistoryRepository.ThreadHistoryOrigin.TagCatalog -> catalogTagId
                        ?.let { readHistoryRepo.getTagCatalogThreadHistoryPosition(it) }
                        ?.takeIf { it.threadId == tid }
                        ?.toThreadReadingHistory()
                    ReadHistoryRepository.ThreadHistoryOrigin.RssCatalog -> catalogRssSubscriptionId
                        ?.let { readHistoryRepo.getRssCatalogThreadHistoryPosition(it) }
                        ?.takeIf { it.threadId == tid }
                        ?.toThreadReadingHistory()
                    ReadHistoryRepository.ThreadHistoryOrigin.Direct -> null
                }
                if (catalogPosition != null) {
                    catalogPosition.threadCover?.let { savedCover ->
                        coverUrl = savedCover
                        contentCoverRepository.setAutomaticCover(coverKey, savedCover)
                        catalogCoverKey?.let { contentCoverRepository.setAutomaticCover(it, savedCover) }
                    }
                    if (catalogPosition.page != initialPage) {
                        loadPage(catalogPosition.page)
                    }
                    pendingSavedPosition = catalogPosition
                    pendingTargetPid = null
                }
            } catch (_: Exception) {
                // Fall through to targetPid restore.
            }
        }

        if (!hasRestoredPosition && targetPid == null) {
            try {
                val savedPosition = readHistoryRepo.getPosition(tid, threadType, authorId)
                if (savedPosition != null) {
                    savedPosition.threadCover?.let { savedCover ->
                        coverUrl = savedCover
                        contentCoverRepository.setAutomaticCover(coverKey, savedCover)
                        catalogCoverKey?.let { contentCoverRepository.setAutomaticCover(it, savedCover) }
                    }
                    // Ensure the saved page is loaded
                    if (savedPosition.page != initialPage) {
                        loadPage(savedPosition.page)
                    }
                    pendingSavedPosition = savedPosition
                } else {
                    hasRestoredPosition = true
                    canPersistReadingState = true
                }
            } catch (_: Exception) {
                hasRestoredPosition = true
                canPersistReadingState = true
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
            canPersistReadingState = true
            delay(300.milliseconds)
            isRestoringSavedPosition = false
        }
    }

    LaunchedEffect(state, canPersistReadingState, readerEntries) {
        if (state != ReaderState.Success || !canPersistReadingState || readerEntries.isEmpty()) {
            return@LaunchedEffect
        }
        delay(300.milliseconds)
        persistCurrentReadingState()
    }

    LaunchedEffect(state, posts, readerEntries, pendingTargetPid) {
        val targetPidLong = pendingTargetPid ?: return@LaunchedEffect
        if (state != ReaderState.Success || posts.isEmpty() || readerEntries.isEmpty()) return@LaunchedEffect

        val targetIndex = entryIndexByPid[targetPidLong] ?: -1
        if (targetIndex >= 0) {
            listState.scrollToItem(targetIndex)
            hasRestoredPosition = true
            canPersistReadingState = true
            pendingTargetPid = null
        } else {
            fallbackNearestPost(targetPidLong, initialPage)
            hasRestoredPosition = true
            canPersistReadingState = true
            pendingTargetPid = null
        }
    }

    val latestContentPostEndingAtIndex = rememberUpdatedState(contentPostEndingAtIndex)
    val latestPersistCurrentReadingState = rememberUpdatedState<suspend () -> Unit> {
        persistCurrentReadingState()
    }
    val latestCaptureReadingSnapshot = rememberUpdatedState(
        newValue = {
            if (!canWriteReadingState()) {
                null
            } else {
                val history = runCatching(::buildHistory)
                    .onFailure { error ->
                        println("READER_PERSIST|history_snapshot|${error::class.simpleName}|${error.message.orEmpty()}")
                    }
                    .getOrNull()
                val progressUpdates = runCatching(::currentChapterUpdates)
                        .onFailure { error ->
                            println("READER_PERSIST|chapter_snapshot|${error::class.simpleName}|${error.message.orEmpty()}")
                        }
                        .getOrDefault(emptyList())
                if (history != null || progressUpdates.isNotEmpty()) {
                    history to progressUpdates
                } else {
                    lastReadingSnapshot
                }
            }
        }
    )
    val latestUntitledLabel = rememberUpdatedState(i18n("（無標題）"))
    val readerScrollSession = remember(listState) { ReaderScrollSession() }
    fun recordCrossedIndices(indices: IntRange?) {
        indices ?: return
        for (index in indices) {
            latestContentPostEndingAtIndex.value[index]?.let { (postId, post) ->
                progressCoordinator.recordCrossedPost(
                    postId = postId,
                    title = post.title.ifBlank { latestUntitledLabel.value },
                )
            }
        }
    }
    val latestFinishScrollSession = rememberUpdatedState<suspend (Int) -> Unit> { generation ->
        val crossedIndices = readerScrollSession.finish(
            expectedGeneration = generation,
            currentIndex = listState.firstVisibleItemIndex,
        )
        if (crossedIndices != null) {
            recordCrossedIndices(crossedIndices)
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val firstVisiblePostId = visibleItems.firstOrNull()
                ?.let { item -> readerEntries.getOrNull(item.index) }
                ?.post
                ?.pid
                ?.value
                ?.toLong()
            if (firstVisiblePostId != null) {
                val passedShortPost = progressUpdateForVisiblePost(
                    postId = firstVisiblePostId,
                    visibleItems = visibleItems,
                    visibleByIndex = visibleItems.associateBy { it.index },
                    viewportTop = layoutInfo.viewportStartOffset + layoutInfo.beforeContentPadding,
                    viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding,
                    allowPassedShortPost = true,
                )
                if (passedShortPost?.read == true) {
                    progressCoordinator.recordCrossedPost(
                        postId = passedShortPost.targetId,
                        title = passedShortPost.title,
                    )
                }
            }
            latestPersistCurrentReadingState.value()
        }
    }
    LaunchedEffect(listState) {
        launch {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { currentIndex ->
                    if (readerScrollSession.activeGeneration() != null) {
                        recordCrossedIndices(readerScrollSession.observe(currentIndex))
                        progressCoordinator.noteScrollStarted()
                    }
                }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                val (history, progressUpdates) = latestCaptureReadingSnapshot.value()
                    ?: return@LifecycleEventObserver
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    persistReadingSnapshot(history, progressUpdates)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(drawerState.isOpen, state, readerEntries, canPersistReadingState) {
        if (!drawerState.isOpen || state != ReaderState.Success || !canPersistReadingState) return@LaunchedEffect
        try {
            persistCurrentReadingState()
        } catch (_: Exception) {
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

    /** Save on back before popping so the previous detail screen reads fresh progress. */
    DisposableEffect(tid, navigator) {
        val handler = {
            if (navigator.currentScreen is IThreadReaderScreen) {
                saveCurrentHistoryAndPop()
                true
            } else {
                false
            }
        }
        navigator.backHandlers.add(handler)
        onDispose {
            navigator.backHandlers.remove(handler)
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
                        chapterStates = progressCoordinator.chapterStates,
                        onPageOrPostClick = { page, post ->
                            scope.launch {
                                val activeGeneration = readerScrollSession.activeGeneration()
                                if (activeGeneration != null) {
                                    latestFinishScrollSession.value(activeGeneration)
                                } else {
                                    persistCurrentReadingState()
                                }
                                if (post != null) {
                                    drawerState.close()
                                    if (page !in loadedPages) {
                                        loadPage(page)
                                        delay(50.milliseconds) // Wait briefly for Compose to layout the new items
                                    }
                                    val targetIndex = entryIndexByPid[post.pid.value.toLong()] ?: -1
                                    if (targetIndex >= 0) scrollToChapterTarget(targetIndex, post.pid.value.toLong())
                                } else {
                                    // User just clicked the page header to expand catalog, load the page, don't close drawer
                                    if (page !in loadedPages) {
                                        loadPage(page)
                                    }
                                }
                            }
                        },
                        onDownload = {
                            scope.launch {
                                downloadSheetPage = currentVisiblePageForAction()
                                drawerState.close()
                                withFrameNanos { }
                                showDownloadSheet = true
                            }
                        },
                        downloadEntriesByPage = downloadQueue
                            .mapNotNull {
                                val key = it.key as? ThreadPageDownloadKey ?: return@mapNotNull null
                                if (key.tid == tid.value && key.authorId == authorId?.value) key.page to it else null
                            }
                            .toMap(),
                        onPostLongPress = { post -> catalogActionPost = post },
                        drawerOpen = drawerState.isOpen,
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
            LocalImageSetCatalogCoverListener provides if (catalogCoverKey != null) ::applySelectedCatalogCover else null,
            LocalImageSetCatalogCoverLabel provides catalogCoverLabel,
            LocalImageActionMessageListener provides { message ->
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(message)
                }
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.creamBackground)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val gestureStartPosition =
                                listState.firstVisibleItemIndex to
                                    listState.firstVisibleItemScrollOffset
                            val hadActiveSession = readerScrollSession.activeGeneration() != null
                            val gestureGeneration =
                                readerScrollSession.start(gestureStartPosition.first)
                            var exceededTouchSlop = false
                            var pointerEvent = awaitPointerEvent(PointerEventPass.Initial)
                            while (pointerEvent.changes.any { it.pressed }) {
                                val trackedChange = pointerEvent.changes.firstOrNull { it.id == down.id }
                                if (
                                    trackedChange != null &&
                                    (trackedChange.position - down.position).getDistance() > viewConfiguration.touchSlop
                                ) {
                                    exceededTouchSlop = true
                                }
                                pointerEvent = awaitPointerEvent(PointerEventPass.Initial)
                            }
                            val finalChange = pointerEvent.changes.firstOrNull { it.id == down.id }
                            if (
                                finalChange != null &&
                                (finalChange.position - down.position).getDistance() > viewConfiguration.touchSlop
                            ) {
                                exceededTouchSlop = true
                            }
                            readerScrollSession.scheduleIdle(scope) {
                                if (!exceededTouchSlop) {
                                    delay(100.milliseconds)
                                    val delayedPosition =
                                        listState.firstVisibleItemIndex to
                                            listState.firstVisibleItemScrollOffset
                                    if (delayedPosition == gestureStartPosition && !hadActiveSession) {
                                        readerScrollSession.cancel(gestureGeneration)
                                        return@scheduleIdle
                                    }
                                    if (delayedPosition != gestureStartPosition) {
                                        progressCoordinator.noteScrollStarted()
                                    }
                                } else {
                                    progressCoordinator.noteScrollStarted()
                                }

                                var stableSamples = 0
                                var sampleCount = 0
                                var lastPosition: Pair<Int, Int>? = null
                                while (
                                    readerScrollSession.isActive(gestureGeneration) &&
                                    stableSamples < 3 &&
                                    sampleCount < 40
                                ) {
                                    delay(50.milliseconds)
                                    sampleCount += 1
                                    val currentPosition =
                                        listState.firstVisibleItemIndex to
                                            listState.firstVisibleItemScrollOffset
                                    recordCrossedIndices(readerScrollSession.observe(currentPosition.first))
                                    stableSamples = if (currentPosition == lastPosition) {
                                        stableSamples + 1
                                    } else {
                                        0
                                    }
                                    lastPosition = currentPosition
                                }
                                latestFinishScrollSession.value(gestureGeneration)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { position ->
                            if (position.x in (size.width / 3f)..(size.width * 2f / 3f)) {
                                showMenu = !showMenu
                                debugPerfLog("toggle_overlay|showMenu=$showMenu")
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
                                            modifier = Modifier.onSizeChanged { size ->
                                                progressCoordinator.updateItemHeight(entry.key, size.height)
                                            },
                                            threadTitle = if (post.floor == 1) title else null,
                                            totalViews = threadInfo?.totalViews.takeIf { post.floor == 1 },
                                            totalReplies = threadInfo?.totalReplies.takeIf { post.floor == 1 },
                                            linkContext = htmlLinkContext,
                                            onVote = { optionIds -> handleVote(optionIds) },
                                            onLoadRateOptions = { threadRepository.fetchRatePopoutPage(tid, post.pid) },
                                            onLoadRateResults = { threadRepository.fetchRateResults(tid, post.pid) },
                                            onLoadVoters = { optionId, page -> threadRepository.fetchVoters(tid, optionId, page) },
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
                                            modifier = Modifier.onSizeChanged { size ->
                                                progressCoordinator.updateItemHeight(entry.key, size.height)
                                            },
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
                                            modifier = Modifier.onSizeChanged { size ->
                                                progressCoordinator.updateItemHeight(entry.key, size.height)
                                            },
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
                                            modifier = Modifier.onSizeChanged { size ->
                                                progressCoordinator.updateItemHeight(entry.key, size.height)
                                            },
                                            bodyBlocks = emptyList(),
                                            showHeader = false,
                                            showFooter = true,
                                            linkContext = htmlLinkContext,
                                            verticalPadding = 0.dp,
                                            onVote = { optionIds -> handleVote(optionIds) },
                                            onLoadRateOptions = { threadRepository.fetchRatePopoutPage(tid, post.pid) },
                                            onLoadRateResults = { threadRepository.fetchRateResults(tid, post.pid) },
                                            onLoadVoters = { optionId, page -> threadRepository.fetchVoters(tid, optionId, page) },
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

                            if (totalPages in loadedPages && posts.isNotEmpty()) {
                                item(key = "refresh_last_page_$totalPages") {
                                    CommentBanner(
                                        text = i18n("重新整理最後一頁"),
                                        icon = "↻",
                                        onClick = {
                                            scope.launch {
                                                val key = ThreadPageDownloadKey(tid.value, totalPages, authorId?.value)
                                                if (downloadRepository.getStatus(key) in setOf(
                                                        DownloadStatus.Downloaded,
                                                        DownloadStatus.UpdateAvailable,
                                                    )
                                                ) {
                                                    showRefreshDownloadedDialog = true
                                                } else {
                                                    state = ReaderState.Loading
                                                    loadPage(totalPages, forceRefresh = true)
                                                }
                                            }
                                        },
                                    )
                                }
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
                            .background(if (readerUsesBrownSystemBar) colors.brownDeep else colors.creamBackground)
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
                    onCatalog = {
                        scope.launch {
                            persistCurrentReadingState()
                            drawerState.open()
                        }
                    },
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
                                scope.launch { snackbarHostState.showSnackbar(i18n("回復已發表，請刷新頁面查看")) }
                                },
                            )
                        )
                    },
                    // Reload the current page
                    onRefresh = {
                        scope.launch {
                            val page = currentVisiblePageForAction()
                            val key = ThreadPageDownloadKey(tid.value, page, authorId?.value)
                            if (downloadRepository.getStatus(key) in setOf(
                                    DownloadStatus.Downloaded,
                                    DownloadStatus.UpdateAvailable,
                                )
                            ) {
                                showRefreshDownloadedDialog = true
                            } else {
                                state = ReaderState.Loading
                                loadPage(page, forceRefresh = true)
                            }
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

                val clipboardManager = LocalClipboardManager.current

                val copyChapter: () -> Unit = {
                    val currentIndex = visibleAnchorEntryIndex() ?: listState.firstVisibleItemIndex
                    val entry = readerEntries.getOrNull(currentIndex)
                    if (entry != null) {
                        val pid = entry.post.pid.value.toLong()
                        val rawHtml = convertedContentByPid[pid] ?: entry.post.contentHtml
                        val plainText = rawHtml
                            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
                            .replace(Regex("<br\\s*/?>\r?\n?", RegexOption.IGNORE_CASE), "\n")
                            .replace(Regex("<[^>]*>"), "")
                            .replace("&nbsp;", " ")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&quot;", "\"")
                            .replace(Regex("\\n\\s*\\n+"), "\n")
                            .trim()
                        clipboardManager.setText(AnnotatedString(plainText))
                        scope.launch {
                            snackbarHostState.showSnackbar(i18n("已複製本章內容"))
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(i18n("暫無內容可複製"))
                        }
                    }
                }

                NovelReaderSettingsPanel(
                    visible = showSettingsPanel,
                    appSettingsRepo = appSettingsRepo,
                    onCopyChapter = copyChapter,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        }
        if (drawerState.isOpen) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(colors.brownDeep)
                    .zIndex(2f)
            )
        }

        if (showDownloadSheet) {
            val threadDownloadEntries = downloadQueue.mapNotNull {
                val key = it.key as? ThreadPageDownloadKey ?: return@mapNotNull null
                if (key.tid == tid.value && key.authorId == authorId?.value) key to it else null
            }
            val pageStatus = threadDownloadEntries
                .firstOrNull { it.first.page == downloadSheetPage }
                ?.second
                ?.status
                ?: DownloadStatus.NotDownloaded
            val completedOrActiveStatuses = setOf(
                DownloadStatus.Queued,
                DownloadStatus.Downloading,
                DownloadStatus.Downloaded,
                DownloadStatus.Paused,
                DownloadStatus.UpdateAvailable,
            )
            val completedOrActivePages = threadDownloadEntries
                .filter { it.second.status in completedOrActiveStatuses }
                .mapTo(mutableSetOf()) { it.first.page }
            ReaderDownloadSheet(
                onDismiss = { showDownloadSheet = false },
                showDownloadPage = pageStatus == DownloadStatus.NotDownloaded ||
                    pageStatus == DownloadStatus.Failed,
                showDownloadThread = (1..totalPages).any { it !in completedOrActivePages },
                showDownloadThreadExceptLastPage = totalPages > 1 &&
                    (1 until totalPages).any { it !in completedOrActivePages },
                showClearPage = pageStatus != DownloadStatus.NotDownloaded,
                showClearThread = threadDownloadEntries.isNotEmpty(),
                onDownloadPage = {
                    val page = downloadSheetPage
                    showDownloadSheet = false
                    scope.launch {
                        downloadRepository.enqueuePage(tid, threadInfo?.title ?: title, authorId, page)
                            .onSuccess { snackbarHostState.showSnackbar(i18n("已加入下載佇列")) }
                            .onFailure {
                                snackbarHostState.showSnackbar(it.message ?: i18n("下載失敗"))
                                navigator.navigate(IBackupSettingsScreen())
                            }
                    }
                },
                onDownloadThread = {
                    showDownloadSheet = false
                    scope.launch {
                        downloadRepository.enqueueThread(tid, threadInfo?.title ?: title, authorId)
                            .onSuccess { snackbarHostState.showSnackbar(i18n("已加入完整 Thread 下載")) }
                            .onFailure {
                                snackbarHostState.showSnackbar(it.message ?: i18n("下載失敗"))
                                if (!downloadRepository.isStorageReady()) {
                                    navigator.navigate(IBackupSettingsScreen())
                                }
                            }
                    }
                },
                onDownloadThreadExceptLastPage = {
                    showDownloadSheet = false
                    scope.launch {
                        downloadRepository.enqueueThreadExceptLastPage(
                            tid,
                            threadInfo?.title ?: title,
                            authorId,
                        )
                            .onSuccess { snackbarHostState.showSnackbar(i18n("已加入除最後一頁外的下載")) }
                            .onFailure {
                                snackbarHostState.showSnackbar(it.message ?: i18n("下載失敗"))
                                if (!downloadRepository.isStorageReady()) {
                                    navigator.navigate(IBackupSettingsScreen())
                                }
                            }
                    }
                },
                onClearPage = {
                    val page = downloadSheetPage
                    showDownloadSheet = false
                    scope.launch {
                        downloadRepository.clearPage(ThreadPageDownloadKey(tid.value, page, authorId?.value))
                        snackbarHostState.showSnackbar(i18n("已清除目前頁下載"))
                    }
                },
                onClearThread = {
                    val page = currentVisiblePageForAction()
                    showDownloadSheet = false
                    scope.launch {
                        downloadRepository.clearThread(ThreadPageDownloadKey(tid.value, page, authorId?.value))
                        snackbarHostState.showSnackbar(i18n("已清除整個 Thread 下載"))
                    }
                },
            )
        }

        if (showDownloadedLastPageWarning) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 132.dp)
                    .fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
                elevation = CardDefaults.cardElevation(4.dp),
                onClick = { showRefreshDownloadedDialog = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(i18n("此頁可能有新回覆"), color = colors.textStrong, fontSize = 15.sp)
                        Text(i18n("建議從網站刷新此下載頁"), color = colors.textDark.copy(alpha = 0.68f), fontSize = 12.sp)
                    }
                    Text(i18n("刷新"), color = colors.brownPrimary, fontSize = 14.sp)
                    TextButton(
                        onClick = {
                            val page = currentVisiblePageForAction()
                            dismissedUpdateWarningPages = dismissedUpdateWarningPages + page
                            showDownloadedLastPageWarning = false
                        },
                    ) {
                        Text("×", color = colors.textDark, fontSize = 20.sp)
                    }
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

    if (showRefreshDownloadedDialog) {
        AlertDialog(
            onDismissRequest = { showRefreshDownloadedDialog = false },
            title = { Text(i18n("刷新已下載頁面"), color = colors.textStrong) },
            text = { Text(i18n("將從網站重新抓取目前頁，成功後覆蓋舊的下載內容。失敗時會保留原下載。"), color = colors.textDark) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val page = currentVisiblePageForAction()
                        showRefreshDownloadedDialog = false
                        scope.launch {
                            state = ReaderState.Loading
                            when (val result = downloadRepository.refreshPage(tid, threadInfo?.title ?: title, authorId, page)) {
                                is YamiboResult.Success -> {
                                    loadedPages = loadedPages - page
                                    loadPage(page)
                                    snackbarHostState.showSnackbar(i18n("已刷新下載內容"))
                                }
                                else -> {
                                    state = ReaderState.Success
                                    snackbarHostState.showSnackbar(i18n("刷新失敗: {}", i18n(result.message())))
                                }
                            }
                        }
                    }
                ) {
                    Text(i18n("刷新"), color = colors.brownPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefreshDownloadedDialog = false }) {
                    Text(i18n("取消"), color = colors.textDark)
                }
            },
            containerColor = colors.creamSurface,
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
        val chapterState = progressCoordinator.chapterStates.value[post.pid.value.toLong()]
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
                    progressCoordinator.setRead(
                        postId = post.pid.value.toLong(),
                        title = post.title.ifBlank { i18n("（無標題）") },
                        read = true,
                    )
                    snackbarHostState.showSnackbar(i18n("已標為已讀"), duration = SnackbarDuration.Short)
                }
            },
            onMarkUnread = {
                catalogActionPost = null
                scope.launch {
                    progressCoordinator.setRead(
                        postId = post.pid.value.toLong(),
                        title = post.title.ifBlank { i18n("（無標題）") },
                        read = false,
                    )
                    snackbarHostState.showSnackbar(i18n("已標為未讀"), duration = SnackbarDuration.Short)
                }
            },
            onClearHistory = {
                catalogActionPost = null
                scope.launch {
                    progressCoordinator.clearAll {
                        when (threadHistoryOrigin) {
                            ReadHistoryRepository.ThreadHistoryOrigin.TagCatalog -> {
                                catalogTagId?.let { readHistoryRepo.deleteTagCatalogThreadHistory(it) }
                            }
                            ReadHistoryRepository.ThreadHistoryOrigin.RssCatalog -> {
                                catalogRssSubscriptionId?.let { readHistoryRepo.deleteRssCatalogThreadHistory(it) }
                            }
                            ReadHistoryRepository.ThreadHistoryOrigin.Direct -> {
                                readHistoryRepo.deleteHistory(tid, threadType, authorId)
                            }
                        }
                    }
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
    chapterStates: kotlinx.coroutines.flow.StateFlow<Map<Long, ChapterStateRepository.Entry>>,
    onPageOrPostClick: (Int, Post?) -> Unit,
    onDownload: () -> Unit,
    downloadEntriesByPage: Map<Int, me.thenano.yamibo.yamibo_app.repository.download.DownloadQueueEntry>,
    onPostLongPress: (Post) -> Unit,
    drawerOpen: Boolean,
) {
    val currentChapterStates by chapterStates.collectAsState()
    val currentPosition by remember(listState, readerEntries, pageByPid, initialPage) {
        derivedStateOf {
            calculateReaderCatalogCurrentPosition(
                listState = listState,
                readerEntries = readerEntries,
                pageByPid = pageByPid,
                initialPage = initialPage,
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
        downloadEntriesByPage = downloadEntriesByPage,
        chapterStates = currentChapterStates,
        onPageOrPostClick = onPageOrPostClick,
        onDownload = onDownload,
        onPostLongPress = onPostLongPress,
        drawerOpen = drawerOpen,
    )
}

private fun calculateReaderCatalogCurrentPosition(
    listState: LazyListState,
    readerEntries: List<ReaderListEntry>,
    pageByPid: Map<Long, Int>,
    initialPage: Int,
): ReaderCatalogCurrentPosition {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val centeredEntry = visibleItems
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
    val entry = centeredEntry?.second
    return ReaderCatalogCurrentPosition(
        page = entry?.let { pageByPid[it.post.pid.value.toLong()] } ?: initialPage,
        pid = entry?.post?.pid,
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
@OptIn(ExperimentalMaterial3Api::class)
private fun ReaderDownloadSheet(
    onDismiss: () -> Unit,
    showDownloadPage: Boolean,
    showDownloadThread: Boolean,
    showDownloadThreadExceptLastPage: Boolean,
    showClearPage: Boolean,
    showClearThread: Boolean,
    onDownloadPage: () -> Unit,
    onDownloadThread: () -> Unit,
    onDownloadThreadExceptLastPage: () -> Unit,
    onClearPage: () -> Unit,
    onClearThread: () -> Unit,
) {
    val colors = YamiboTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.creamSurface,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(i18n("下載"), color = colors.textStrong, fontSize = 22.sp)
            Text(
                text = i18n("下載會保存整頁帖子與原始圖片。"),
                color = colors.textDark.copy(alpha = 0.68f),
                fontSize = 13.sp,
            )
            if (showDownloadPage) {
                DownloadSheetAction(i18n("下載目前頁"), i18n("保存此頁所有帖子與圖片"), false, onDownloadPage)
            }
            if (showDownloadThread) {
                DownloadSheetAction(i18n("下載完整 Thread"), i18n("將全部頁面加入背景佇列"), false, onDownloadThread)
            }
            if (showDownloadThreadExceptLastPage) {
                DownloadSheetAction(
                    i18n("下載除最後一頁的所有頁面"),
                    i18n("保留可能持續更新的最後一頁在線閱讀"),
                    false,
                    onDownloadThreadExceptLastPage,
                )
            }
            if (showClearPage) {
                DownloadSheetAction(i18n("清除目前頁下載"), i18n("只刪除此頁離線內容"), true, onClearPage)
            }
            if (showClearThread) {
                DownloadSheetAction(i18n("清除整個 Thread 下載"), i18n("取消佇列並刪除所有已下載頁"), true, onClearThread)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(i18n("關閉"), color = colors.brownPrimary)
            }
        }
    }
}

@Composable
private fun DownloadSheetAction(
    title: String,
    subtitle: String,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamBackground),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = YamiboIcons.Download,
                contentDescription = null,
                tint = if (destructive) colors.redAccent else colors.orangeAccent,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    color = if (destructive) colors.redAccent else colors.textStrong,
                    fontSize = 15.sp,
                )
                Text(
                    text = subtitle,
                    color = colors.textDark.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
        }
    }
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
                    CatalogActionRow(
                        if (read) i18n("標為未讀") else i18n("標為已讀"),
                        if (read) onMarkUnread else onMarkRead,
                    )
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
