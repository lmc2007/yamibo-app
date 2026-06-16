package me.thenano.yamibo.yamibo_app.favorite.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import kotlin.math.roundToInt

@Composable
fun FavoriteListScrollbar(
    state: LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier,
) {
    val firstVisibleIndex by remember(state) {
        derivedStateOf { state.firstVisibleItemIndex }
    }
    val visibleCount by remember(state) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }

    FavoriteScrollbar(
        modifier = modifier,
        totalItems = totalItems,
        firstVisibleIndex = firstVisibleIndex,
        visibleCount = visibleCount,
        onScrollToIndex = { index -> state.scrollToItem(index) },
    )
}

@Composable
fun FavoriteGridScrollbar(
    state: LazyGridState,
    totalItems: Int,
    modifier: Modifier = Modifier,
) {
    val firstVisibleIndex by remember(state) {
        derivedStateOf { state.firstVisibleItemIndex }
    }
    val visibleCount by remember(state) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }

    FavoriteScrollbar(
        modifier = modifier,
        totalItems = totalItems,
        firstVisibleIndex = firstVisibleIndex,
        visibleCount = visibleCount,
        onScrollToIndex = { index -> state.scrollToItem(index) },
    )
}

@Composable
fun FavoriteStaggeredScrollbar(
    state: LazyStaggeredGridState,
    totalItems: Int,
    modifier: Modifier = Modifier,
) {
    val firstVisibleIndex by remember(state) {
        derivedStateOf { state.firstVisibleItemIndex }
    }
    val visibleCount by remember(state) {
        derivedStateOf { state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) }
    }

    FavoriteScrollbar(
        modifier = modifier,
        totalItems = totalItems,
        firstVisibleIndex = firstVisibleIndex,
        visibleCount = visibleCount,
        onScrollToIndex = { index -> state.scrollToItem(index) },
    )
}

@Composable
private fun FavoriteScrollbar(
    totalItems: Int,
    firstVisibleIndex: Int,
    visibleCount: Int,
    onScrollToIndex: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalItems <= visibleCount || totalItems <= 0) return

    val colors = YamiboTheme.colors
    val scope = rememberCoroutineScope()

    // Track whether the user is actively dragging the thumb
    var isDragging by remember { mutableStateOf(false) }

    // Animate thumb width: wider when dragging for better visibility
    val thumbWidth = remember { Animatable(6f) }
    LaunchedEffect(isDragging) {
        thumbWidth.animateTo(
            targetValue = if (isDragging) 10f else 6f,
            animationSpec = tween(durationMillis = 150),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(36.dp)
            .padding(end = 4.dp, top = 10.dp, bottom = 92.dp),
    ) {
        val trackHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val hiddenCount = (totalItems - visibleCount).coerceAtLeast(1)
        val thumbHeightPx = (trackHeightPx * (visibleCount.toFloat() / totalItems.toFloat()))
            .coerceAtLeast(with(LocalDensity.current) { 48.dp.toPx() })
            .coerceAtMost(trackHeightPx * 0.5f)
        val travelPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
        val progress = (firstVisibleIndex.toFloat() / hiddenCount.toFloat()).coerceIn(0f, 1f)
        val thumbOffsetPx = travelPx * progress

        fun scrollFromTouch(y: Float) {
            val ratio = ((y - thumbHeightPx / 2f) / travelPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val targetIndex = (ratio * hiddenCount.toFloat()).roundToInt().coerceIn(0, totalItems - 1)
            scope.launch { onScrollToIndex(targetIndex) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Tap anywhere on the track to jump to that position
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        scrollFromTouch(offset.y)
                    }
                }
                // Use a stable key (Unit) so the gesture detector is NOT reset
                // on every scroll frame — this prevents jitter and dropped drags.
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            scrollFromTouch(offset.y)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            scrollFromTouch(change.position.y)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    )
                },
        ) {
            // Track background — wider for better visibility
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(thumbWidth.value.dp)
                    .background(colors.brownPrimary.copy(alpha = 0.10f), RoundedCornerShape(999.dp)),
            )
            // Thumb — wider, taller minimum, more opaque for easy touch targeting
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffsetPx.roundToInt()) }
                    .align(Alignment.TopEnd)
                    .width(thumbWidth.value.dp)
                    .height(with(LocalDensity.current) { thumbHeightPx.toDp() })
                    .background(colors.brownDeep.copy(alpha = 0.80f), RoundedCornerShape(999.dp)),
            )
        }
    }
}
