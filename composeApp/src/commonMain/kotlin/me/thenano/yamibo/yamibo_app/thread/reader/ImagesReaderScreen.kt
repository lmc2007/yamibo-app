package me.thenano.yamibo.yamibo_app.thread.reader

import YamiboIcons
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.thread.image.ImageContextMenu
import me.thenano.yamibo.yamibo_app.thread.image.ImageViewer
import me.thenano.yamibo.yamibo_app.thread.reader.components.manga.*
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.math.abs

@Composable
fun ImagesReaderScreen(
    tid: ThreadId,
    postId: PostId,
    fid: ForumId?,
    threadTitle: String,
    imageList: List<String>,
    initialPage: Int = 1,
    loadHistory: Boolean = false
) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val historyRepo = LocalReadHistoryRepository.current
    val isMangaForum = remember(fid) { fid?.let { YamiboForum.isMangaForum(it) } == true }

    /** State */
    var readingMode by remember { mutableStateOf(ReadingMode.SINGLE_LTR) }
    var touchZoneLayout by remember { mutableStateOf(TouchZoneLayout.L_SHAPE) }
    var showOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTouchZonePreview by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuImageUrl by remember { mutableStateOf("") }
    
    val scrollListState = rememberLazyListState()
    var currentPage by remember { mutableIntStateOf((initialPage - 1).coerceIn(0, (imageList.size - 1).coerceAtLeast(0))) }

    LaunchedEffect(Unit) {
        if (isMangaForum && loadHistory) {
            val history = historyRepo.getImagePosition(postId)
            if (history != null) {
                currentPage = history.pageIndex.coerceIn(0, imageList.lastIndex)
                if (history.firstVisibleItemIndex != null) {
                    scrollListState.scrollToItem(history.firstVisibleItemIndex!!, history.firstVisibleItemOffset ?: 0)
                }
            }
        }
    }

    LaunchedEffect(isMangaForum) {
        if (!isMangaForum) return@LaunchedEffect
        
        snapshotFlow { 
            Triple(currentPage, scrollListState.firstVisibleItemIndex, scrollListState.firstVisibleItemScrollOffset)
        }.collectLatest { (page, idx, offset) ->
            delay(1000) // Debounce saves
            val isScroll = readingMode == ReadingMode.SCROLL_CONTINUOUS || readingMode == ReadingMode.SCROLL_GAP
            val (finalIdx, finalOffset) = if (isScroll) {
                idx to offset
            } else {
                null to null
            }
            historyRepo.saveImagePosition(
                ReadHistoryRepository.ImageReadingHistory(
                    postId = postId,
                    threadId = tid,
                    pageIndex = page,
                    totalPages = imageList.size,
                    firstVisibleItemIndex = finalIdx,
                    firstVisibleItemOffset = finalOffset,
                    lastVisitTime = currentTimeMillis()
                )
            )
        }
    }

    DisposableEffect(isMangaForum) {
        onDispose {
            if (isMangaForum) {
                val isScroll = readingMode == ReadingMode.SCROLL_CONTINUOUS || readingMode == ReadingMode.SCROLL_GAP
                val finalIdx = if (isScroll) scrollListState.firstVisibleItemIndex else null
                val finalOffset = if (isScroll) scrollListState.firstVisibleItemScrollOffset else null
                val snapshotPage = currentPage
                val total = imageList.size
                
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch {
                    historyRepo.saveImagePosition(
                        ReadHistoryRepository.ImageReadingHistory(
                            postId = postId,
                            threadId = tid,
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

    /** Animated zoom state */
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val isRtl = readingMode == ReadingMode.SINGLE_RTL
    val isVerticalMode = readingMode == ReadingMode.SINGLE_TTB
    val isScrollMode = readingMode == ReadingMode.SCROLL_CONTINUOUS || readingMode == ReadingMode.SCROLL_GAP

    /** Track previous page for slide direction (Single-page mode only) */
    var previousPage by remember { mutableIntStateOf(currentPage) }
    val slideDirection = if (currentPage >= previousPage) 1 else -1
    LaunchedEffect(currentPage) { previousPage = currentPage }

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

    /** Handle back press: settings > overlay > touchZonePreview */
    fun handleBack(): Boolean {
        return when {
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
        if (showSettings) { showSettings = false; return }
        if (showOverlay) return  // Overlay dismissed via its own scrim
        if (showTouchZonePreview) { showTouchZonePreview = false; return }

        if (touchZoneLayout != TouchZoneLayout.DISABLED) {
            when (getTouchAction(touchZoneLayout, xFraction, yFraction)) {
                TouchAction.PREV -> {
                    if (isScrollMode) return // Scroll mode uses native scroll, prev zone optional
                    if (currentPage > 0) { currentPage--; resetZoom() }
                }
                TouchAction.NEXT -> {
                    if (isScrollMode) return // Scroll mode uses native scroll, next zone optional
                    if (currentPage < imageList.size - 1) { currentPage++; resetZoom() }
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

    // Outer wrapper so nothing outside goes black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
    ) {
        // Zoomable Content Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                // ── Single Unified Gesture Handler for Both Modes ──
                .pointerInput(touchZoneLayout, readingMode) {
                    // Persist state across iterations
                    var lastTapTime = 0L
                    var pendingTapJob: Job? = null

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // If overlay/settings is open, don't handle any gestures here
                        if (showOverlay || showSettings) {
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }

                        val downTime = currentTimeMillis()
                        val downPos = down.position
                        var totalDrag = Offset.Zero
                        var isLongPress = false
                        var longPressCancelled = false

                        val longPressJob = scope.launch {
                            delay(400)
                            if (!longPressCancelled) {
                                isLongPress = true
                                contextMenuImageUrl = imageList.getOrElse(currentPage) { "" }
                                showContextMenu = true
                            }
                        }

                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                totalDrag += change.position - change.previousPosition
                            }
                            // Cancel long press if moved > 15px
                            if ((abs(totalDrag.x) + abs(totalDrag.y)) > 15f && !longPressCancelled) {
                                longPressCancelled = true
                                longPressJob.cancel()
                            }
                        } while (event.changes.any { it.pressed })

                        longPressJob.cancel()
                        if (isLongPress) return@awaitEachGesture

                        val dragDist = abs(totalDrag.x) + abs(totalDrag.y)
                        val isTap = dragDist < 30f

                        if (isTap) {
                            val elapsed = downTime - lastTapTime
                            if (elapsed in 1L..<320L) {
                                // Double tap
                                lastTapTime = 0L
                                pendingTapJob?.cancel()
                                pendingTapJob = null
                                handleDoubleTap(downPos)
                            } else {
                                // Defer single tap
                                lastTapTime = downTime
                                pendingTapJob?.cancel()
                                pendingTapJob = scope.launch {
                                    delay(320)
                                    val xFrac = downPos.x / size.width.toFloat()
                                    val yFrac = downPos.y / size.height.toFloat()
                                    handleSingleTap(xFrac, yFrac)
                                }
                            }
                        } else if (scaleAnim.value <= 1.1f && !isScrollMode) {
                            // Swipe gesture (Single-page mode only)
                            pendingTapJob?.cancel()
                            pendingTapJob = null
                            if (isVerticalMode) {
                                val threshold = size.height * 0.15f
                                if (totalDrag.y < -threshold && currentPage < imageList.size - 1) {
                                    currentPage++; resetZoom()
                                } else if (totalDrag.y > threshold && currentPage > 0) {
                                    currentPage--; resetZoom()
                                }
                            } else {
                                val threshold = size.width * 0.15f
                                if (isRtl) {
                                    if (totalDrag.x > threshold && currentPage < imageList.size - 1) {
                                        currentPage++; resetZoom()
                                    } else if (totalDrag.x < -threshold && currentPage > 0) {
                                        currentPage--; resetZoom()
                                    }
                                } else {
                                    if (totalDrag.x < -threshold && currentPage < imageList.size - 1) {
                                        currentPage++; resetZoom()
                                    } else if (totalDrag.x > threshold && currentPage > 0) {
                                        currentPage--; resetZoom()
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(readingMode) {
                    awaitEachGesture {
                        var pan = Offset.Zero
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var currentScale = scaleAnim.value
                        var currentOffsetX = offsetXAnim.value
                        var currentOffsetY = offsetYAnim.value
                        
                        var boundX = 0f
                        var boundY = 0f
                        var lastMoveTime = down.uptimeMillis
                        var isStaleFling = false

                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { change ->
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
                            }

                            if (event.changes.size >= 2) {
                                val firstChange = event.changes[0]
                                val secondChange = event.changes[1]
                                val prevDist = (firstChange.previousPosition - secondChange.previousPosition).getDistance()
                                val currDist = (firstChange.position - secondChange.position).getDistance()
                                if (prevDist > 0f) {
                                    val zoom = currDist / prevDist
                                    currentScale = (currentScale * zoom).coerceIn(1f, 5f)
                                    
                                    boundX = if (currentScale > 1f) (containerSize.width * (currentScale - 1f)) / 2f else 0f
                                    boundY = if (currentScale > 1f && !isScrollMode) (containerSize.height * (currentScale - 1f)) / 2f else 0f
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
                                boundY = if (currentScale > 1f && !isScrollMode) (containerSize.height * (currentScale - 1f)) / 2f else 0f
                                offsetXAnim.updateBounds(-boundX, boundX)
                                offsetYAnim.updateBounds(-boundY, boundY)
                                
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
                        } while (event.changes.any { it.pressed })

                        // ── Inertia / Fling Animation ──
                        if (currentScale > 1.1f && !isStaleFling) {
                            val velocity = velocityTracker.calculateVelocity()
                            if (abs(velocity.x) > 300f || (!isScrollMode && abs(velocity.y) > 300f)) {
                                scope.launch {
                                    if (abs(velocity.x) > 300f) {
                                        launch {
                                            offsetXAnim.animateDecay(
                                                initialVelocity = velocity.x * 0.4f, // Reduce inertia
                                                animationSpec = exponentialDecay()
                                            )
                                        }
                                    }
                                    if (!isScrollMode && abs(velocity.y) > 300f) {
                                        launch {
                                            offsetYAnim.animateDecay(
                                                initialVelocity = velocity.y * 0.4f, // Reduce inertia
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
            if (imageList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = YamiboIcons.Book,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(text = "沒有找到圖片", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else if (isScrollMode) {


                LazyColumn(
                    state = scrollListState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(imageList) { index, url ->
                        ImageViewer(
                            url = url,
                            contentDescription = "第${index + 1}頁",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth(),
                            enableContextMenu = false,
                            isDarkTheme = true,
                            enableCrossfade = false
                        )
                        if (readingMode == ReadingMode.SCROLL_GAP && index < imageList.lastIndex) {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                LaunchedEffect(scrollListState) {
                    snapshotFlow { scrollListState.firstVisibleItemIndex }
                        .collect { index -> currentPage = index }
                }
            } else {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (isVerticalMode) {
                            slideInVertically(tween(300)) { it * slideDirection } togetherWith
                                slideOutVertically(tween(300)) { -it * slideDirection }
                        } else {
                            val dirMul = if (isRtl) -1 else 1
                            slideInHorizontally(tween(300)) { it * slideDirection * dirMul } togetherWith
                                slideOutHorizontally(tween(300)) { -it * slideDirection * dirMul }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    ImageViewer(
                        url = imageList.getOrElse(page) { "" },
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

        /** Overlays (Not affected by graphicsLayer zoom) */

        // Page Indicator
        if (!showOverlay && imageList.isNotEmpty()) {
            Text(
                text = "${currentPage + 1} / ${imageList.size}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Touch zone preview overlay
        TouchZoneOverlay(visible = showTouchZonePreview, layout = touchZoneLayout)

        // Manga overlay (TopBar + BottomBar) — has its own scrim for dismissal
        MangaReaderOverlay(
            visible = showOverlay && !showSettings,
            title = threadTitle,
            currentPage = currentPage,
            totalPages = imageList.size,
            isRtl = isRtl,
            onBack = {
                // Ignore back handlers by temporarily hiding overlays so pop() works normally
                showOverlay = false
                showSettings = false
                showTouchZonePreview = false
                navigator.pop()
            },
            onPageChange = { page ->
                currentPage = page.coerceIn(0, imageList.lastIndex)
                resetZoom()
            },
            onSettings = { showSettings = true },
            onDismiss = { showOverlay = false }
        )

        // Settings scrim to close panel when tapping outside
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showSettings = false }
            )
        }

        // Settings panel
        MangaReaderSettingsPanel(
            visible = showSettings,
            currentReadingMode = readingMode,
            currentTouchZoneLayout = touchZoneLayout,
            onReadingModeChange = { mode -> readingMode = mode; resetZoom() },
            onTouchZoneLayoutChange = { layout -> touchZoneLayout = layout; showTouchZonePreview = true },
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