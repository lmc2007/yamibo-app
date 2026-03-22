import sys

filepath = r"c:\Users\allen\OneDrive\Desktop\Projects\kotlin\yamibo-app\composeApp\src\commonMain\kotlin\me\thenano\yamibo\yamibo_app\thread\reader\ImagesReaderScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    text = f.read()

# We want to replace everything from "Box(" under "// Outer wrapper so nothing outside goes black" 
# up to "LazyColumn(" under "if (isScrollMode) {"

start_marker = """    // Outer wrapper so nothing outside goes black
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
    ) {
        // Zoomable Content Box
        Box(
            modifier = Modifier
                .fillMaxSize()"""

end_marker = """        ) {
            if (isScrollMode) {
                LazyColumn(
                    state = scrollListState,
                    modifier = Modifier.fillMaxSize()
                ) {"""

if start_marker not in text:
    print("Start marker not found!")
    sys.exit(1)

if end_marker not in text:
    print("End marker not found!")
    sys.exit(1)

start_idx = text.find(start_marker)
end_idx = text.find(end_marker) + len(end_marker)

replacement = """
    // Scroll Mode boundary jumper (Overscroll Chapter Jump)
    var scrollOverscrollY by remember { mutableFloatStateOf(0f) }
    val nestedScrollConnection = remember(hasNextChapter, hasPrevChapter) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f && scrollListState.canScrollForward) scrollOverscrollY = 0f
                if (available.y < 0f && scrollListState.canScrollBackward) scrollOverscrollY = 0f
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (source.toString() != "Fling" && source.toString() != "SideEffect") {
                    if (!scrollListState.canScrollForward && available.y < 0f) {
                        scrollOverscrollY += available.y
                        if (scrollOverscrollY < -150f && hasNextChapter) {
                            scrollOverscrollY = 0f
                            launchNextChapter()
                        }
                    } else if (!scrollListState.canScrollBackward && available.y > 0f) {
                        scrollOverscrollY += available.y
                        if (scrollOverscrollY > 150f && hasPrevChapter) {
                            scrollOverscrollY = 0f
                            launchPrevChapter()
                        }
                    } else {
                        scrollOverscrollY = 0f
                    }
                } else {
                    scrollOverscrollY = 0f
                }
                return androidx.compose.ui.geometry.Offset.Zero
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
                // 1. Unified Tap Handler
                .pointerInput(touchZoneLayout, readingMode, actualImageList.size) {
                    androidx.compose.foundation.gestures.detectTapGestures(
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
                        androidx.compose.foundation.gestures.detectDragGestures(
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
                        var pan = androidx.compose.ui.geometry.Offset.Zero
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var currentScale = scaleAnim.value
                        var currentOffsetX = offsetXAnim.value
                        var currentOffsetY = offsetYAnim.value
                        
                        var boundX = 0f
                        var boundY = 0f
                        var lastMoveTime = down.uptimeMillis
                        var isStaleFling = false

                        val velocityTracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
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
                            if (kotlin.math.abs(velocity.x) > 300f || (!isScrollMode && kotlin.math.abs(velocity.y) > 300f)) {
                                scope.launch {
                                    if (kotlin.math.abs(velocity.x) > 300f) {
                                        launch {
                                            offsetXAnim.animateDecay(
                                                initialVelocity = velocity.x * 0.4f,
                                                animationSpec = androidx.compose.animation.core.exponentialDecay()
                                            )
                                        }
                                    }
                                    if (!isScrollMode && kotlin.math.abs(velocity.y) > 300f) {
                                        launch {
                                            offsetYAnim.animateDecay(
                                                initialVelocity = velocity.y * 0.4f,
                                                animationSpec = androidx.compose.animation.core.exponentialDecay()
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
                        .androidx.compose.ui.input.nestedscroll.nestedScroll(nestedScrollConnection)
                ) {"""

new_text = text[:start_idx] + replacement.strip('\n') + text[end_idx:]

with open(filepath, "w", encoding="utf-8") as f:
    f.write(new_text)

print("Replacement successful!")
