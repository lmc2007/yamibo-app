package me.thenano.yamibo.yamibo_app.thread.reader

import YamiboIcons
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.value.*
import io.github.littlesurvival.dto.page.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.LocalMangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalTagRepository
import me.thenano.yamibo.yamibo_app.LocalThreadRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.image.ImageContextMenu
import me.thenano.yamibo.yamibo_app.thread.image.ImageViewer
import me.thenano.yamibo.yamibo_app.thread.reader.components.manga.*
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.util.shareText
import me.thenano.yamibo.yamibo_app.util.state
import coil3.compose.LocalPlatformContext
import me.thenano.yamibo.yamibo_app.repository.settings.ReadingMode
import me.thenano.yamibo.yamibo_app.repository.settings.TouchZoneLayout
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Suppress("DuplicatedCode")
@Composable
fun ImagesReaderScreen(
    tid: ThreadId,
    postId: PostId?,
    fid: ForumId?,
    threadTitle: String,
    imageList: List<String>,
    initialPage: Int = 1,
    loadHistory: Boolean = false,

    // Tag Manga Mode Fields:
    tagId: TagId? = null,
    tagName: String? = null,
    tagPage: Int? = null,
    tagTotalPages: Int? = null,
    authorId: UserId?,
    tagThreads: List<ThreadSummary>?,
) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val historyRepo = LocalReadHistoryRepository.current
    val threadRepository = LocalThreadRepository.current
    val platformContext = LocalPlatformContext.current

    // Tag specific state
    var currentTagPage by remember { mutableStateOf(tagPage ?: 1) }
    var currentTagTotalPages by remember { mutableStateOf(tagTotalPages) }
    var currentThreads by remember { mutableStateOf(tagThreads ?: emptyList()) }

    var activeTid by remember(tid) { mutableStateOf(tid) }

    // Computed properties for the currently active thread

    val currentThreadIndex: () -> Int = { currentThreads.indexOfFirst { it.tid == activeTid } }
    val activeThread = if (currentThreadIndex() in currentThreads.indices) currentThreads[currentThreadIndex()] else null
    val activeTitle = activeThread?.title ?: threadTitle
    val activeAuthorId = activeThread?.author?.uid ?: authorId
    val activeFid = activeThread?.fid ?: fid

    val isMangaForum = remember(activeFid, tagId) { tagId != null || (activeFid?.let { YamiboForum.isMangaForum(it) } == true) }

    val mangaSettingsRepo = LocalMangaReaderSettingsRepository.current
    val readingMode = mangaSettingsRepo.readingMode.state()
    val touchZoneLayout = mangaSettingsRepo.touchZone.state()
    
    val isRtl = readingMode == ReadingMode.SINGLE_RTL
    val isVerticalMode = readingMode == ReadingMode.SINGLE_TTB
    val isScrollMode = readingMode == ReadingMode.SCROLL_CONTINUOUS || readingMode == ReadingMode.SCROLL_GAP

    var actualImageList by remember(activeTid) { mutableStateOf(if (activeTid == tid) imageList else emptyList()) }
    var isLoadingImages by remember { mutableStateOf(actualImageList.isEmpty()) }
    var actualPostId by remember(activeTid) { mutableStateOf(if (activeTid == tid) postId else null) }
    var startFromLastPage by remember { mutableStateOf(false) }

    /** State */
    var showOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTouchZonePreview by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuImageUrl by remember { mutableStateOf("") }
    // Tag Catalog State
    var showCatalog by remember { mutableStateOf(false) }
    val tagRepository = LocalTagRepository.current
    val loadedThreadsByPage = remember { mutableStateMapOf<Int, List<ThreadSummary>>() }

    LaunchedEffect(currentTagPage, currentThreads) {
        if (currentThreads.isNotEmpty()) {
            loadedThreadsByPage[currentTagPage] = currentThreads
        }
    }

    LaunchedEffect(tagId, currentTagPage) {
        if (tagId != null && currentThreads.isEmpty()) {
            val result = tagRepository.fetchTagPage(tagId, currentTagPage)
            if (result is YamiboResult.Success) {
                currentThreads = result.value.threadSummaries
                val idx = currentThreads.indexOfFirst { it.tid == activeTid }
                if (idx == -1 && currentThreads.isNotEmpty() && activeTid.value == 0) { // Only fallback if dummy tid
                    activeTid = currentThreads.first().tid
                }
            } else {
                snackbarHostState.showSnackbar("無法載入閱讀目錄")
            }
        }
    }
    
    var prevThreadTitle by remember { mutableStateOf<String?>(null) }
    var nextThreadTitle by remember { mutableStateOf<String?>(null) }
    var hasPrevChapter by remember { mutableStateOf(false) }
    var hasNextChapter by remember { mutableStateOf(false) }

    LaunchedEffect(currentThreadIndex(), currentThreads, currentTagPage, isLoadingImages) {
        if (tagId != null) {
            if (currentThreadIndex() > 0) {
                hasPrevChapter = true
                prevThreadTitle = currentThreads.getOrNull(currentThreadIndex() - 1)?.title
            } else if (currentTagPage > 1) {
                hasPrevChapter = true
                prevThreadTitle = if (isLoadingImages) "正在載入上一個分頁..." else "上一個分頁"
            } else {
                hasPrevChapter = false
                prevThreadTitle = null
            }

            if (currentThreadIndex() >= 0 && currentThreadIndex() < currentThreads.lastIndex) {
                hasNextChapter = true
                nextThreadTitle = currentThreads.getOrNull(currentThreadIndex() + 1)?.title
            } else if (currentTagPage < (tagTotalPages ?: 1)) {
                hasNextChapter = true
                nextThreadTitle = if (isLoadingImages) "正在載入下一個分頁..." else "下一個分頁"
            } else {
                hasNextChapter = false
                nextThreadTitle = null
            }
        }
    }

    val scrollListState = rememberLazyListState()
    val totalContentPages: () -> Int = { actualImageList.size.coerceAtLeast(1) }
    val minPage: () -> Int = { if (tagId != null) -1 else 0 }
    val maxPage: () -> Int = { if (tagId != null) totalContentPages() else totalContentPages() - 1 }

    var currentPage by remember(activeTid) { mutableIntStateOf(initialPage - 1) }

    var retryTrigger by remember { mutableIntStateOf(0) }

    var snapNextTransition by remember { mutableStateOf(false) }

    LaunchedEffect(tagId, currentTagPage) {
        if (tagId != null && currentThreads.isEmpty()) {
            val result = tagRepository.fetchTagPage(tagId, currentTagPage)
            if (result is YamiboResult.Success) {
                currentThreads = result.value.threadSummaries
                currentTagTotalPages = result.value.pageNav?.totalPages ?: 1
                loadedThreadsByPage[currentTagPage] = currentThreads
            }
        }
    }

    LaunchedEffect(activeTid, retryTrigger) {
        if (actualImageList.isEmpty()) {
            isLoadingImages = true
            
            suspend fun processThreadResult(result: YamiboResult<ThreadPage>? = null, cached: ThreadPage? = null) {
                val threadPage = cached ?: (result as? YamiboResult.Success)?.value
                if (threadPage != null) {
                    val p = threadPage.posts.firstOrNull()
                    if (p != null) {
                        actualPostId = p.pid
                        val targetAuthorId = activeAuthorId ?: p.author.uid
                        val authorPosts = threadPage.posts.filter { it.author.uid == targetAuthorId }.take(2)
                        actualImageList = authorPosts.flatMap { it.images }.map { it.url }
                        
                        // Coerce the initial page against the actual loaded bounds now
                        currentPage = currentPage.coerceIn(minPage(), maxPage())

                        if (startFromLastPage && actualImageList.isNotEmpty()) {
                            snapNextTransition = true
                            currentPage = actualImageList.size - 1
                        } else if (loadHistory) {
                            var didRestore = false

                            fun onRestoreScrollListState(history: ReadHistoryRepository.AnyReadingHistory) {
                                when(history) {
                                    is ReadHistoryRepository.TagMangaReadingHistory,is ReadHistoryRepository.TagMangaReadingHistory -> {
                                        snapNextTransition = true
                                        val finalPageToRestore = if (history.threadImagePageIndex >= actualImageList.size) 0 else history.threadImagePageIndex.coerceIn(minPage(), totalContentPages())
                                        currentPage = finalPageToRestore
                                        if (history.firstVisibleItemIndex != null && finalPageToRestore > 0 && isScrollMode) {
                                            scope.launch {
                                                while (scrollListState.layoutInfo.totalItemsCount == 0) delay(20.milliseconds)
                                                scrollListState.scrollToItem(history.firstVisibleItemIndex!!, history.firstVisibleItemOffset ?: 0)
                                            }
                                        }
                                        didRestore = true
                                    }
                                    else -> return
                                }
                            }

                            if (tagId != null) {
                                val history = historyRepo.getTagMangaReaderModeHistoryPosition(tagId)
                                if (history != null && history.threadId == activeTid) {
                                    onRestoreScrollListState(history)
                                }
                            }
                            if (!didRestore) {
                                val history = historyRepo.getImagePosition(p.pid)
                                if (history != null) {
                                    onRestoreScrollListState(history)
                                }
                            }
                            
                            if (!didRestore && isScrollMode) {
                                scope.launch {
                                    while (scrollListState.layoutInfo.totalItemsCount == 0) delay(20.milliseconds)
                                    scrollListState.scrollToItem(if (tagId != null) 1 else 0)
                                }
                            }
                        } else {
                            if (isScrollMode) {
                                scope.launch {
                                    while (scrollListState.layoutInfo.totalItemsCount == 0) delay(20.milliseconds)
                                    scrollListState.scrollToItem(if (tagId != null) 1 else 0)
                                }
                            }
                        }
                    }
                    startFromLastPage = false
                    isLoadingImages = false
                } else {
                    isLoadingImages = false
                }
            }

            val cachedPage = threadRepository.getCachedThread(activeTid, null, 1)
            if (cachedPage != null) {
                processThreadResult(cached = cachedPage)
            } else {
                processThreadResult(result = threadRepository.fetchThread(activeTid, null, 1))
            }
        }
    }

    val currentHistorySaver by rememberUpdatedState {
        if (isMangaForum || tagId != null) {
            val isScroll = readingMode == ReadingMode.SCROLL_CONTINUOUS || readingMode == ReadingMode.SCROLL_GAP
            val finalIdx = if (isScroll) scrollListState.firstVisibleItemIndex else null
            val finalOffset = if (isScroll) scrollListState.firstVisibleItemScrollOffset else null
            val snapshotPage = currentPage
            val total = actualImageList.size
            val activeTidSnap = activeTid
            val postIdSnap = actualPostId
            val tagPageSnap = currentTagPage
            val currentCoverSnap = actualImageList.getOrNull(1) ?: actualImageList.getOrNull(0)
            
            @OptIn(DelicateCoroutinesApi::class)
            GlobalScope.launch {
                if (tagId != null && tagName != null) {
                    val existing = historyRepo.getTagMangaReaderModeHistoryPosition(tagId)
                    val finalCover = currentCoverSnap ?: existing?.coverUrl

                    historyRepo.saveTagMangaReaderModeHistory(
                        ReadHistoryRepository.TagMangaReadingHistory(
                            tagId = tagId,
                            tagName = tagName,
                            tagPage = tagPageSnap,
                            threadId = activeTidSnap,
                            threadTitle = activeTitle,
                            threadImagePageIndex = snapshotPage.coerceIn(minPage(), maxPage()),
                            threadImageTotalPages = total.coerceAtLeast(1),
                            firstVisibleItemIndex = finalIdx,
                            firstVisibleItemOffset = finalOffset,
                            lastVisitTime = currentTimeMillis(),
                            coverUrl = finalCover
                        )
                    )
                } else if (isMangaForum && postIdSnap != null) {
                    historyRepo.saveImagePosition(
                        ReadHistoryRepository.ImageReadingHistory(
                            postId = postIdSnap,
                            threadId = activeTidSnap,
                            pageIndex = snapshotPage,
                            totalPages = total,
                            firstVisibleItemIndex = finalIdx,
                            firstVisibleItemOffset = finalOffset,
                            lastVisitTime = currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { currentHistorySaver() }
    }

    LaunchedEffect(activeTid, currentPage, currentTagPage) {
        delay(2000.milliseconds)
        currentHistorySaver()
    }

    /** Animated zoom state */
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(readingMode) {
        if (isScrollMode) {
            val targetIndex = if (tagId != null) currentPage.coerceAtLeast(0) + 1 else currentPage.coerceAtLeast(0)
            if (targetIndex >= 0) {
                while (scrollListState.layoutInfo.totalItemsCount == 0 && actualImageList.isNotEmpty()) {
                    delay(20.milliseconds)
                }
                scrollListState.scrollToItem(targetIndex)
            }
        }
    }

    /** Reset zoom with animation */
    fun resetZoom(animated: Boolean = false) {
        scope.launch {
            if (animated) {
                launch { scaleAnim.animateTo(1f, tween(250)) }
                launch { offsetXAnim.animateTo(0f, tween(250)) }
                launch { offsetYAnim.animateTo(0f, tween(250)) }
            } else {
                scaleAnim.snapTo(1f)
                offsetXAnim.snapTo(0f)
                offsetYAnim.snapTo(0f)
            }
        }
    }
    
    // Scroll Mode boundary jumper accumulator
    var scrollOverscrollY by remember { mutableFloatStateOf(0f) }

    val launchNextChapter = {
        if (tagId != null && !isLoadingImages && hasNextChapter) {
            isLoadingImages = true
            scrollOverscrollY = 0f
            scope.launch {
                var loadedCrossPage = false
                if (currentThreadIndex() >= 0 && currentThreadIndex() < currentThreads.lastIndex) {
                    activeTid = currentThreads[currentThreadIndex() + 1].tid
                    currentPage = 0
                    actualImageList = emptyList()
                    actualPostId = null
                } else if (currentTagPage < (currentTagTotalPages ?: 1)) {
                    val pageToLoad = currentTagPage + 1
                    val cached = tagRepository.getCachedTagPage(tagId, pageToLoad)
                    val result = if (cached != null) YamiboResult.Success(cached) else tagRepository.fetchTagPage(tagId, pageToLoad)
                    if (result is YamiboResult.Success) {
                        currentThreads = result.value.threadSummaries
                        currentTagPage = pageToLoad
                        if (currentThreads.isNotEmpty()) {
                            activeTid = currentThreads.first().tid
                        }
                        currentPage = 0
                        actualImageList = emptyList()
                        actualPostId = null
                        loadedCrossPage = true
                    } else {
                        snackbarHostState.showSnackbar("無法載入下一頁")
                    }
                }
                delay(600.milliseconds)
                isLoadingImages = false
            }
        }
    }

    val launchPrevChapter = {
        if (tagId != null && !isLoadingImages && hasPrevChapter) {
            isLoadingImages = true
            scrollOverscrollY = 0f
            scope.launch {
                var loadedCrossPage = false
                if (currentThreadIndex() > 0) {
                    activeTid = currentThreads[currentThreadIndex() - 1].tid
                    currentPage = 0
                    actualImageList = emptyList()
                    actualPostId = null
                    startFromLastPage = true
                } else if (currentTagPage > 1) {
                    val pageToLoad = currentTagPage - 1
                    val cached = tagRepository.getCachedTagPage(tagId, pageToLoad)
                    val result = if (cached != null) YamiboResult.Success(cached) else tagRepository.fetchTagPage(tagId, pageToLoad)
                    if (result is YamiboResult.Success) {
                        currentThreads = result.value.threadSummaries
                        currentTagPage = pageToLoad
                        if (currentThreads.isNotEmpty()) {
                            activeTid = currentThreads.last().tid
                        }
                        currentPage = 0
                        actualImageList = emptyList()
                        actualPostId = null
                        startFromLastPage = true
                        loadedCrossPage = true
                    } else {
                        snackbarHostState.showSnackbar("無法載入上一頁")
                    }
                }
                delay(600.milliseconds)
                isLoadingImages = false
            }
        }
    }

    /** Handle back press: settings > overlay > touchZonePreview */
    fun handleBack(): Boolean {
        return when {
            showCatalog -> { showCatalog = false; true }
            showSettings -> { showSettings = false; true }
            showOverlay -> { showOverlay = false; true }
            showTouchZonePreview -> { showTouchZonePreview = false; true }
            else -> false
        }
    }

    /** Register back handler */
    DisposableEffect(Unit) {
        val handler = { handleBack() }
        navigator.backHandlers.add(handler)
        onDispose { navigator.backHandlers.remove(handler) }
    }

    /** Handle single tap touch zone logic */
    fun handleSingleTap(xFraction: Float, yFraction: Float) {
        if (showCatalog) { showCatalog = false; return }
        if (showSettings) { showSettings = false; return }
        if (showOverlay) return  // Overlay dismissed via its own scrim
        if (showTouchZonePreview) { showTouchZonePreview = false; return }

        if (touchZoneLayout != TouchZoneLayout.DISABLED) {
            when (getTouchAction(touchZoneLayout, xFraction, yFraction)) {
                TouchAction.PREV -> {
                    if (isScrollMode) {
                        if (currentPage == minPage() && hasPrevChapter) launchPrevChapter()
                        return
                    }
                    if (currentPage > minPage()) { currentPage--; resetZoom() }
                    else if (currentPage == minPage() && hasPrevChapter) { launchPrevChapter() }
                }
                TouchAction.NEXT -> {
                    if (isScrollMode) {
                        if (currentPage == maxPage() && hasNextChapter) launchNextChapter()
                        return
                    }
                    if (currentPage < maxPage()) { currentPage++; resetZoom() }
                    else if (currentPage == maxPage() && hasNextChapter) { launchNextChapter() }
                }
                TouchAction.MENU -> showOverlay = true
                null -> {}
            }
        } else {
            showOverlay = !showOverlay
        }
    }

    /** Handle double tap → animated zoom toggle */
    fun handleDoubleTap(tapOffset: Offset) {
        scope.launch {
            if (scaleAnim.value > 1.1f) {
                // Zoom Out
                launch { scaleAnim.animateTo(1f, tween(280)) }
                launch { offsetXAnim.animateTo(0f, tween(280)) }
                launch { offsetYAnim.animateTo(0f, tween(280)) }
            } else {
                // Zoom In
                val centerX = containerSize.width / 2f
                val centerY = containerSize.height / 2f
                val targetScale = 2.0f
                launch { scaleAnim.animateTo(targetScale, tween(280)) }
                launch { offsetXAnim.animateTo((centerX - tapOffset.x) * (targetScale - 1f), tween(280)) }
                launch { offsetYAnim.animateTo((centerY - tapOffset.y) * (targetScale - 1f), tween(280)) }
            }
        }
    }

    // Scroll Mode boundary jumper (Overscroll Chapter Jump)
    val nestedScrollConnection = remember(hasNextChapter, hasPrevChapter) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && scrollListState.canScrollForward) scrollOverscrollY = 0f
                if (available.y < 0f && scrollListState.canScrollBackward) scrollOverscrollY = 0f
                return Offset.Zero
            }
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source.toString() != "Fling" && source.toString() != "SideEffect") {
                    if (!scrollListState.canScrollForward && available.y < 0f) {
                        scrollOverscrollY += available.y
                        if (scrollOverscrollY < -100f && hasNextChapter) {
                            scrollOverscrollY = 0f
                            launchNextChapter()
                        }
                    } else if (!scrollListState.canScrollBackward && available.y > 0f) {
                        scrollOverscrollY += available.y
                        if (scrollOverscrollY > 100f && hasPrevChapter) {
                            scrollOverscrollY = 0f
                            launchPrevChapter()
                        }
                    } else {
                        scrollOverscrollY = 0f
                    }
                } else {
                    scrollOverscrollY = 0f
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Zoomable Content Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { containerSize = it }
                // 1. Unified Tap Handler
                .pointerInput(touchZoneLayout, readingMode, actualImageList.size) {
                    detectTapGestures(
                        onTap = { offset ->
                            val xFrac = offset.x / size.width.toFloat()
                            val yFrac = offset.y / size.height.toFloat()
                            handleSingleTap(xFrac, yFrac)
                        },
                        onDoubleTap = { handleDoubleTap(it) },
                        onLongPress = {
                            contextMenuImageUrl = actualImageList.getOrElse(currentPage) { "" }
                            showContextMenu = true
                        }
                    )
                }
                // 2. Swipe Handler for Single Page Mode
                .pointerInput(readingMode, actualImageList.size) {
                    if (!isScrollMode) {
                        var dragAccX = 0f
                        var dragAccY = 0f
                        detectDragGestures(
                            onDragStart = { dragAccX = 0f; dragAccY = 0f },
                            onDragEnd = {
                                if (scaleAnim.value <= 1.05f) { // Only turn page if not zoomed in
                                    if (isVerticalMode) {
                                        if (dragAccY < -80f) {
                                            if (currentPage < maxPage()) { currentPage++; resetZoom() }
                                            else if (currentPage == totalContentPages() && hasNextChapter) { launchNextChapter() }
                                        } else if (dragAccY > 80f) {
                                            if (currentPage > minPage()) { currentPage--; resetZoom() }
                                            else if (currentPage == minPage() && hasPrevChapter) { launchPrevChapter() }
                                        }
                                    } else {
                                        val dir = if (isRtl) -1 else 1
                                        val effectiveDrag = dragAccX * dir
                                        if (effectiveDrag < -80f) {
                                            if (currentPage < maxPage()) { currentPage++; resetZoom() }
                                            else if (currentPage == totalContentPages() && hasNextChapter) { launchNextChapter() }
                                        } else if (effectiveDrag > 80f) {
                                            if (currentPage > minPage()) { currentPage--; resetZoom() }
                                            else if (currentPage == minPage() && hasPrevChapter) { launchPrevChapter() }
                                        }
                                    }
                                }
                            }
                        ) { change, dragAmount ->
                            if (scaleAnim.value <= 1.05f) {
                                dragAccX += dragAmount.x
                                dragAccY += dragAmount.y
                                change.consume() // Prevent other gestures from seeing this unzoomed drag
                            }
                        }
                    }
                }
                // 3. Zoom and Pan Handler
                .pointerInput(readingMode) {
                    awaitEachGesture {
                        var pan: Offset
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var currentScale = scaleAnim.value
                        var currentOffsetX = offsetXAnim.value
                        var currentOffsetY = offsetYAnim.value
                        
                        var boundX: Float
                        var boundY: Float
                        var lastMoveTime = down.uptimeMillis
                        var isStaleFling = false
                        var isPanning = false

                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size == 1) {
                                val change = event.changes[0]
                                if (change.pressed) {
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    if ((change.position - change.previousPosition).getDistance() > 1f) {
                                        lastMoveTime = change.uptimeMillis
                                    }
                                } else {
                                    if (change.uptimeMillis - lastMoveTime > 60L) {
                                        isStaleFling = true
                                    }
                                }
                            } else {
                                velocityTracker.resetTracking()
                                isStaleFling = true // Never fling after a multi-touch/pinch release
                            }

                            if (event.changes.size >= 2) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    val newScale = (currentScale * zoomChange).coerceIn(1f, 5f)
                                    val effectiveZoom = newScale / currentScale
                                    currentScale = newScale
                                    
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val centerX = containerSize.width / 2f
                                    val centerY = containerSize.height / 2f
                                    val locCentroidX = centroid.x - centerX
                                    val locCentroidY = centroid.y - centerY
                                    
                                    currentOffsetX = locCentroidX - (locCentroidX - currentOffsetX) * effectiveZoom + panChange.x
                                    currentOffsetY = locCentroidY - (locCentroidY - currentOffsetY) * effectiveZoom + panChange.y
                                    
                                    boundX = if (currentScale > 1f) (containerSize.width * (currentScale - 1f)) / 2f else 0f
                                    boundY = if (currentScale > 1f) (containerSize.height * (currentScale - 1f)) / 2f else 0f
                                    offsetXAnim.updateBounds(-boundX, boundX)
                                    offsetYAnim.updateBounds(-boundY, boundY)
                                    
                                    currentOffsetX = currentOffsetX.coerceIn(-boundX, boundX)
                                    currentOffsetY = currentOffsetY.coerceIn(-boundY, boundY)
                                    
                                    scope.launch { 
                                        scaleAnim.snapTo(currentScale) 
                                        offsetXAnim.snapTo(currentOffsetX)
                                        offsetYAnim.snapTo(currentOffsetY)
                                    }
                                }
                                event.changes.forEach { it.consume() } // consume pinch
                            } else if (currentScale > 1.1f && event.changes.size == 1) {
                                val change = event.changes[0]
                                pan = change.position - change.previousPosition
                                
                                boundX = if (currentScale > 1f) (containerSize.width * (currentScale - 1f)) / 2f else 0f
                                boundY = if (currentScale > 1f) (containerSize.height * (currentScale - 1f)) / 2f else 0f
                                offsetXAnim.updateBounds(-boundX, boundX)
                                offsetYAnim.updateBounds(-boundY, boundY)
                                
                                if (!isPanning && (change.position - down.position).getDistance() > 18f) {
                                    isPanning = true
                                }
                                
                                if (isPanning) {
                                    currentOffsetX = (currentOffsetX + pan.x).coerceIn(-boundX, boundX)
                                    if (!isScrollMode) {
                                        currentOffsetY = (currentOffsetY + pan.y).coerceIn(-boundY, boundY)
                                        change.consume() // Consume 1-finger drag so background lists don't scroll
                                    }
                                    scope.launch {
                                        offsetXAnim.snapTo(currentOffsetX)
                                        if (!isScrollMode) offsetYAnim.snapTo(currentOffsetY)
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        // ── Inertia / Fling Animation ──
                        if (currentScale > 1.1f && !isStaleFling) {
                            val velocity = velocityTracker.calculateVelocity()
                            if (abs(velocity.x) > 300f || (!isScrollMode && abs(velocity.y) > 300f)) {
                                scope.launch {
                                    if (abs(velocity.x) > 300f) {
                                        launch {
                                            offsetXAnim.animateDecay(
                                                initialVelocity = velocity.x * 0.4f,
                                                animationSpec = exponentialDecay()
                                            )
                                        }
                                    }
                                    if (!isScrollMode && abs(velocity.y) > 300f) {
                                        launch {
                                            offsetYAnim.animateDecay(
                                                initialVelocity = velocity.y * 0.4f,
                                                animationSpec = exponentialDecay()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    translationX = offsetXAnim.value
                    translationY = offsetYAnim.value
                }
        ) {
            if (isScrollMode) {
                LazyColumn(
                    state = scrollListState,
                    modifier = Modifier.fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    if (tagId != null) {
                        item {
                            Box(modifier = Modifier.clickable { if (hasPrevChapter) launchPrevChapter() }) {
                                InterstitialCard(
                                    item = ReaderItem.InterstitialItem(
                                        prevTitle = prevThreadTitle,
                                        nextTitle = activeTitle,
                                        isEnd = !hasPrevChapter,
                                        isPrev = true,
                                        readingMode = readingMode
                                    )
                                )
                            }
                        }
                    }

                    if (actualImageList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                if (isLoadingImages) {
                                    CircularProgressIndicator(color = YamiboTheme.colors.brownPrimary)
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = YamiboIcons.Book,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Text(text = "沒有找到圖片", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                                        Spacer(Modifier.height(16.dp))
                                        OutlinedButton(
                                            onClick = { retryTrigger++ },
                                            colors = outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                                        ) {
                                            Text("重新載入")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(actualImageList) { index, url ->
                            ImageViewer(
                                url = url,
                                contentDescription = "第${index + 1}頁",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth(),
                                enableContextMenu = false,
                                isDarkTheme = true,
                                enableCrossfade = false
                            )
                            if (readingMode == ReadingMode.SCROLL_GAP && index < actualImageList.lastIndex) {
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }

                    if (tagId != null) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            Box(modifier = Modifier.clickable { if (hasNextChapter) launchNextChapter() }) {
                                InterstitialCard(
                                    item = ReaderItem.InterstitialItem(
                                        prevTitle = activeTitle,
                                        nextTitle = nextThreadTitle,
                                        isEnd = !hasNextChapter,
                                        isPrev = false,
                                        readingMode = readingMode
                                    )
                                )
                            }
                        }
                    }
                }

                LaunchedEffect(scrollListState, actualImageList.size, activeTid) {
                    snapshotFlow { scrollListState.firstVisibleItemIndex }
                        .collect { index ->
                            val adjustedIndex = if (tagId != null) index - 1 else index
                            currentPage = adjustedIndex.coerceIn(minPage(), maxPage())
                        }
                }
            } else {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1

                        if (snapNextTransition) {
                            snapNextTransition = false
                            EnterTransition.None togetherWith ExitTransition.None
                        } else if (isVerticalMode) {
                            slideInVertically(tween(300)) { it * dir } togetherWith
                                slideOutVertically(tween(300)) { -it * dir }
                        } else {
                            val dirMul = if (isRtl) -1 else 1
                            slideInHorizontally(tween(300)) { it * dirMul * dir } togetherWith
                                slideOutHorizontally(tween(300)) { -it * dirMul * dir }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        -1 -> InterstitialCard(
                            item = ReaderItem.InterstitialItem(
                                prevTitle = prevThreadTitle,
                                nextTitle = activeTitle,
                                isEnd = !hasPrevChapter,
                                isPrev = true,
                                readingMode = readingMode
                            )
                        )
                        totalContentPages() -> InterstitialCard(
                            item = ReaderItem.InterstitialItem(
                                prevTitle = activeTitle,
                                nextTitle = nextThreadTitle,
                                isEnd = !hasNextChapter,
                                isPrev = false,
                                readingMode = readingMode
                            )
                        )
                        else -> {
                            if (actualImageList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (isLoadingImages) {
                                        CircularProgressIndicator(color = YamiboTheme.colors.brownPrimary)
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = YamiboIcons.Book,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(Modifier.height(16.dp))
                                            Text(text = "沒有找到圖片", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                                            Spacer(Modifier.height(16.dp))
                                            OutlinedButton(
                                                onClick = { retryTrigger++ },
                                                colors = outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
                                            ) {
                                                Text("重新載入")
                                            }
                                        }
                                    }
                                }
                            } else {
                                ImageViewer(
                                    url = actualImageList.getOrElse(page) { "" },
                                    contentDescription = "第${page + 1}頁",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize(),
                                    enableContextMenu = false,
                                    isDarkTheme = true,
                                    enableCrossfade = false
                                )
                            }
                        }
                    }
                }
            }
        }

        /** Overlays (Not affected by graphicsLayer zoom) */

        // Page Indicator
        if (!showOverlay && actualImageList.isNotEmpty()) {
            val totalPagesUi = actualImageList.size.coerceAtLeast(1)
            val clampedPage = currentPage.coerceIn(0, totalPagesUi - 1)
            Text(
                text = "${clampedPage + 1} / $totalPagesUi",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Touch zone preview overlay
        TouchZoneOverlay(visible = showTouchZonePreview, layout = touchZoneLayout)

        // Manga overlay (TopBar + BottomBar) — has its own scrim for dismissal
        MangaReaderOverlay(
            visible = showOverlay && !showSettings && !showCatalog,
            title = activeTitle,
            currentPage = currentPage.coerceIn(0, (actualImageList.size - 1).coerceAtLeast(0)),
            totalPages = actualImageList.size,
            isRtl = isRtl,
            onBack = {
                // Ignore back handlers by temporarily hiding overlays so pop() works normally
                showOverlay = false
                showSettings = false
                showTouchZonePreview = false
                showCatalog = false
                navigator.pop()
            },
            onPageChange = { page ->
                currentPage = page.coerceIn(0, actualImageList.lastIndex)
                if (isScrollMode) {
                    val targetIndex = if (tagId != null) currentPage + 1 else currentPage
                    scope.launch { scrollListState.scrollToItem(targetIndex) }
                }
                resetZoom()
            },
            onSettings = { showSettings = true },
            onDismiss = { showOverlay = false },
            onCatalog = if (tagId != null) { { showCatalog = true; showOverlay = false } } else null,
            onNavigateToThread = if (tagId != null) { {
                if (activeFid != null && YamiboForum.isNovelForum(activeFid)) {
                    navigator.navigate(INovelThreadDetailScreen(activeTid, activeTitle, activeAuthorId))
                } else {
                    navigator.navigate(
                        IThreadReaderScreen(
                            tid = activeTid,
                            title = activeTitle,
                            threadType = ReadHistoryRepository.ThreadEntryType.Normal,
                            authorId = activeAuthorId
                        )
                    )
                }
            } } else null,
            subtitle = tagName,
            onShare = {
                val url = YamiboRoute.Thread(activeTid).build()
                shareText(platformContext, url, activeTitle)
            }
        )

        // Catalog Panel
        if (tagId != null) {
            AnimatedVisibility(
                visible = showCatalog,
                enter = slideInHorizontally(tween(250)) { -it } + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(250)) { -it } + fadeOut(tween(200))
            ) {
                TagCatalogPanel(
                    totalPages = currentTagTotalPages ?: 1,
                    loadedThreadsByPage = loadedThreadsByPage,
                    currentTagPage = currentTagPage,
                    currentThreadId = activeTid,
                    onPageOrThreadClick = { page, targetThread ->
                        if (targetThread == null) {
                            if (!loadedThreadsByPage.containsKey(page)) {
                                scope.launch {
                                    val cached = tagRepository.getCachedTagPage(tagId, page)
                                    if (cached != null) {
                                        loadedThreadsByPage[page] = cached.threadSummaries
                                    } else {
                                        val result = tagRepository.fetchTagPage(tagId, page)
                                        if (result is YamiboResult.Success) {
                                            val correctedPageNav = result.value.pageNav?.copy(currentPage = page)
                                            val correctedResult = result.value.copy(pageNav = correctedPageNav)
                                            loadedThreadsByPage[page] = correctedResult.threadSummaries
                                        }
                                    }
                                }
                            }
                        } else {
                            showCatalog = false
                            currentTagPage = page
                            // Navigate to target thread by updating local state
                            if (targetThread.tid != activeTid) {
                                activeTid = targetThread.tid
                                if (page != currentTagPage) {
                                    currentTagPage = page
                                    currentThreads = loadedThreadsByPage[page] ?: emptyList()
                                }
                                currentPage = 0
                                actualImageList = emptyList()
                                actualPostId = null
                            }
                        }
                    },
                    onDismiss = { showCatalog = false }
                )
            }
        }

        // Settings scrim to close panel when tapping outside
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSettings = false }
            )
        }

        // Settings panel
        MangaReaderSettingsPanel(
            visible = showSettings,
            currentReadingMode = readingMode,
            currentTouchZoneLayout = touchZoneLayout,
            onReadingModeChange = { mode -> mangaSettingsRepo.readingMode.setValue(mode); resetZoom() },
            onTouchZoneLayoutChange = { layout -> mangaSettingsRepo.touchZone.setValue(layout); showTouchZonePreview = true },
            onDismiss = { showSettings = false },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Context menu (long press)
        ImageContextMenu(
            visible = showContextMenu,
            imageUrl = contextMenuImageUrl,
            onDismiss = { showContextMenu = false },
            isBottomSheet = true
        )

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
        )
    }
}
