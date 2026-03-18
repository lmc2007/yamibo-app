package me.thenano.yamibo.yamibo_app.thread.reader

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.reader.components.manga.*

@Composable
fun MangaReaderScreen(tid: ThreadId, threadTitle: String, imageList: List<String>) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    /** State */
    var readingMode by remember { mutableStateOf(ReadingMode.DEFAULT) }
    var touchZoneLayout by remember { mutableStateOf(TouchZoneLayout.DEFAULT) }
    var showOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTouchZonePreview by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuImageUrl by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }

    /** Zoom state for single-page modes */
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val isZoomed = scale > 1.1f

    /** Reset zoom */
    fun resetZoom() {
        scale = 1f
        offset = Offset.Zero
    }

    /** Handle back press priority */
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

    /** Handle single tap with touch zone logic */
    fun handleSingleTap(xFraction: Float, yFraction: Float) {
        // Dismiss touch zone preview on any tap
        if (showTouchZonePreview) {
            showTouchZonePreview = false
            return
        }

        // Priority: exit settings > exit overlay
        if (showSettings) {
            showSettings = false
            return
        }

        if (showOverlay) {
            showOverlay = false
            return
        }

        // Touch zone navigation
        if (touchZoneLayout != TouchZoneLayout.DISABLED) {
            val action = getTouchAction(touchZoneLayout, xFraction, yFraction)
            when (action) {
                TouchAction.PREV -> {
                    if (currentPage > 0) {
                        currentPage--
                        resetZoom()
                    }
                }
                TouchAction.NEXT -> {
                    if (currentPage < imageList.size - 1) {
                        currentPage++
                        resetZoom()
                    }
                }
                TouchAction.MENU -> {
                    showOverlay = true
                }
                null -> { /* disabled */ }
            }
        } else {
            showOverlay = !showOverlay
        }
    }

    /** Handle double tap → zoom toggle */
    fun handleDoubleTap(tapOffset: Offset) {
        if (isZoomed) {
            resetZoom()
        } else {
            scale = 2.5f
            // Center zoom on tap point
            val centerX = containerSize.width / 2f
            val centerY = containerSize.height / 2f
            offset = Offset(
                x = (centerX - tapOffset.x) * 1.5f,
                y = (centerY - tapOffset.y) * 1.5f
            )
        }
    }

    /** Determine if we're in scroll mode */
    val isScrollMode = readingMode == ReadingMode.SCROLL_CONTINUOUS || readingMode == ReadingMode.SCROLL_GAP

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
    ) {
        if (imageList.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = YamiboIcons.Book,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "沒有找到圖片",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else if (isScrollMode) {
            // Scroll mode (continuous or with gaps)
            val scrollListState = rememberLazyListState()

            LazyColumn(
                state = scrollListState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(touchZoneLayout) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val xFrac = tapOffset.x / size.width.toFloat()
                                val yFrac = tapOffset.y / size.height.toFloat()
                                handleSingleTap(xFrac, yFrac)
                            },
                            onLongPress = { _ ->
                                // Find visible image for context menu
                                val visibleIndex = scrollListState.firstVisibleItemIndex.coerceIn(0, imageList.lastIndex)
                                contextMenuImageUrl = imageList[visibleIndex]
                                showContextMenu = true
                            }
                        )
                    }
            ) {
                itemsIndexed(imageList) { index, url ->
                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = "第${index + 1}頁",
                        contentScale = ContentScale.FillWidth,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = colors.brownPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("圖片載入失敗", color = Color.White.copy(alpha = 0.5f))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gap between images in gap mode
                    if (readingMode == ReadingMode.SCROLL_GAP && index < imageList.lastIndex) {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Track current page from scroll position
            LaunchedEffect(scrollListState) {
                snapshotFlow { scrollListState.firstVisibleItemIndex }
                    .collect { index -> currentPage = index }
            }
        } else {
            // Single-page mode
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                if (isZoomed) {
                    offset += panChange
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentPage, touchZoneLayout) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                if (isZoomed) {
                                    resetZoom()
                                } else {
                                    val xFrac = tapOffset.x / size.width.toFloat()
                                    val yFrac = tapOffset.y / size.height.toFloat()
                                    handleSingleTap(xFrac, yFrac)
                                }
                            },
                            onDoubleTap = { tapOffset ->
                                handleDoubleTap(tapOffset)
                            },
                            onLongPress = { _ ->
                                contextMenuImageUrl = imageList.getOrElse(currentPage) { "" }
                                showContextMenu = true
                            }
                        )
                    }
                    .pointerInput(currentPage, readingMode) {
                        // Swipe detection based on reading mode (observing passively!)
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var totalX = 0f
                            var totalY = 0f
                            
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Final)
                                // Only accumulate if not zoomed to avoid huge totals from panning
                                if (!isZoomed) {
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        val delta = change.position - change.previousPosition
                                        totalX += delta.x
                                        totalY += delta.y
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            
                            // Pointer released
                            if (!isZoomed) {
                                val isVerticalMode = readingMode == ReadingMode.SINGLE_TTB
                                if (isVerticalMode) {
                                    val threshold = size.height * 0.15f
                                    if (totalY < -threshold && currentPage < imageList.size - 1) {
                                        currentPage++; resetZoom()
                                    } else if (totalY > threshold && currentPage > 0) {
                                        currentPage--; resetZoom()
                                    }
                                } else {
                                    val threshold = size.width * 0.15f
                                    val isRtl = readingMode == ReadingMode.SINGLE_RTL || readingMode == ReadingMode.DEFAULT
                                    if (isRtl) {
                                        // RTL: swipe right = next, swipe left = prev
                                        if (totalX > threshold && currentPage < imageList.size - 1) {
                                            currentPage++; resetZoom()
                                        } else if (totalX < -threshold && currentPage > 0) {
                                            currentPage--; resetZoom()
                                        }
                                    } else {
                                        // LTR: swipe left = next, swipe right = prev
                                        if (totalX < -threshold && currentPage < imageList.size - 1) {
                                            currentPage++; resetZoom()
                                        } else if (totalX > threshold && currentPage > 0) {
                                            currentPage--; resetZoom()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .transformable(state = transformState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = imageList.getOrElse(currentPage) { "" }
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "第${currentPage + 1}頁",
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = colors.brownPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("圖片載入失敗", color = Color.White.copy(alpha = 0.5f))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Touch zone preview overlay
        TouchZoneOverlay(
            visible = showTouchZonePreview,
            layout = touchZoneLayout
        )
        // Manga overlay (TopBar + BottomBar)
        MangaReaderOverlay(
            visible = showOverlay && !showSettings,
            title = threadTitle,
            currentPage = currentPage,
            totalPages = imageList.size,
            onBack = { navigator.pop() },
            onPageChange = { page ->
                currentPage = page.coerceIn(0, imageList.lastIndex)
                resetZoom()
                if (isScrollMode) {
                    scope.launch {
                        val scrollState = /* handled by scroll mode */ Unit
                    }
                }
            },
            onSettings = { showSettings = true }
        )

        // Settings panel
        MangaReaderSettingsPanel(
            visible = showSettings,
            currentReadingMode = readingMode,
            currentTouchZoneLayout = touchZoneLayout,
            onReadingModeChange = { mode ->
                readingMode = mode
                resetZoom()
            },
            onTouchZoneLayoutChange = { layout ->
                touchZoneLayout = layout
                showTouchZonePreview = true
            },
            onDismiss = { showSettings = false },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Context menu (long press)
        MangaImageContextMenu(
            visible = showContextMenu,
            imageUrl = contextMenuImageUrl,
            onCopy = {
                clipboardManager.setText(AnnotatedString(contextMenuImageUrl))
                showContextMenu = false
                scope.launch { snackbarHostState.showSnackbar("已複製圖片連結") }
            },
            onShare = {
                clipboardManager.setText(AnnotatedString(contextMenuImageUrl))
                showContextMenu = false
                scope.launch { snackbarHostState.showSnackbar("已複製圖片連結 (分享)") }
            },
            onSave = {
                showContextMenu = false
                scope.launch { snackbarHostState.showSnackbar("儲存功能開發中") }
            },
            onDismiss = { showContextMenu = false }
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