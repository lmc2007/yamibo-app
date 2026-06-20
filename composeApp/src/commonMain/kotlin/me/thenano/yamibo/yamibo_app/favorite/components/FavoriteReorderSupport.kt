package me.thenano.yamibo.yamibo_app.favorite.components

import me.thenano.yamibo.yamibo_app.components.navigation.NavigationBackSymbol
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

@Composable
internal fun rememberReorderGapOffset(
    key: Long,
    index: Int,
    draggingKey: Long,
    draggingStartIndex: Int,
    draggingTargetIndex: Int,
    rowStridePx: Float,
    animate: Boolean,
): Float {
    remember(key) { key }
    val targetOffset = when {
        draggingKey == 0L -> 0f
        draggingStartIndex == -1 || draggingTargetIndex == -1 -> 0f
        key == draggingKey -> 0f
        draggingStartIndex < draggingTargetIndex &&
            index in (draggingStartIndex + 1)..draggingTargetIndex -> -rowStridePx
        draggingTargetIndex < draggingStartIndex &&
            index in draggingTargetIndex until draggingStartIndex -> rowStridePx
        else -> 0f
    }
    if (!animate) {
        return targetOffset
    }
    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = if (targetOffset == 0f) 120 else 180),
        label = "favoriteReorderGapOffset",
    )
    return animatedOffset
}

internal fun Modifier.fastReorderDrag(
    enabled: Boolean,
    key: Any,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (pointerY: Float, dragY: Float) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var latest = down
            var released = false
            val triggered = withTimeoutOrNull(180L) {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                    if (!change.pressed) return@withTimeoutOrNull false
                    latest = change
                }
            } == null

            if (!triggered) {
                onDragCancel()
                return@awaitEachGesture
            }

            onDragStart()
            val finished = false
            while (!finished) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                latest = change
                if (!change.pressed) {
                    released = true
                    break
                }
                val delta = change.position - change.previousPosition
                if (delta != Offset.Zero) {
                    change.consume()
                    onDrag(change.position.y, delta.y)
                }
            }

            if (released || !latest.pressed) onDragEnd() else onDragCancel()
        }
    }
}

internal fun calculateReorderTargetIndex(
    draggingId: Long,
    draggingOverlayTop: Float,
    draggingTravelY: Float,
    itemTopMap: Map<Long, Float>,
    orderedIds: List<Long>,
    itemHeightPx: Float,
): Int {
    if (orderedIds.isEmpty()) return -1
    val draggedCenter = draggingOverlayTop + draggingTravelY + itemHeightPx / 2f
    val slotCenters = orderedIds.map { id ->
        val top = itemTopMap[id] ?: 0f
        top + itemHeightPx / 2f
    }

    var targetIndex = slotCenters.indexOfFirst { draggedCenter < it }
    if (targetIndex == -1) {
        targetIndex = orderedIds.lastIndex
    }

    val currentIndex = orderedIds.indexOf(draggingId)
    if (currentIndex == -1) return targetIndex
    return targetIndex.coerceIn(0, orderedIds.lastIndex)
}

internal fun <T> reorderedList(
    items: List<T>,
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return items
    return items.toMutableList().apply {
        add(toIndex, removeAt(fromIndex))
    }
}

@Composable
internal fun ReorderActionChip(
    text: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        color = if (emphasized) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (emphasized) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.18f)),
    ) {
        Text(
            text = text,
            color = if (emphasized) colors.creamBackground else colors.brownDeep,
            fontSize = if (text == NavigationBackSymbol) 18.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
