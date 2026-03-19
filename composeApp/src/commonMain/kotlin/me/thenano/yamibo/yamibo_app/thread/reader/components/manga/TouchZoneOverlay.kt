package me.thenano.yamibo.yamibo_app.thread.reader.components.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** Touch zone layout configuration for the manga reader */
enum class TouchZoneLayout(val label: String) {
    L_SHAPE("L式"),
    KINDLE("Kindle式"),
    EDGE("邊緣式"),
    LEFT_RIGHT("左右式"),
    DISABLED("停用")
}

/** Describes the action area for a touch zone region */
enum class TouchAction(val label: String) {
    PREV("上一頁"),
    NEXT("下一頁"),
    MENU("選單")
}

/**
 * Touch zone preview overlay.
 * Shows colored regions indicating touch areas for navigation.
 * Automatically fades out on the user's next single click.
 */
@Composable
fun TouchZoneOverlay(
    visible: Boolean,
    layout: TouchZoneLayout,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        when (layout) {
            TouchZoneLayout.L_SHAPE -> LShapeZoneLayout()
            TouchZoneLayout.KINDLE -> KindleZoneLayout()
            TouchZoneLayout.EDGE -> EdgeZoneLayout()
            TouchZoneLayout.LEFT_RIGHT -> LeftRightZoneLayout()
            TouchZoneLayout.DISABLED -> { /* No overlay */ }
        }
    }
}

/** Determine touch action based on layout and tap position (fraction: 0..1) */
fun getTouchAction(layout: TouchZoneLayout, xFraction: Float, yFraction: Float): TouchAction? {
    return when (layout) {
        TouchZoneLayout.L_SHAPE -> {
            when {
                yFraction < 0.20f -> TouchAction.PREV
                xFraction < 0.30f -> TouchAction.PREV
                yFraction > 0.80f -> TouchAction.NEXT
                xFraction > 0.70f -> TouchAction.NEXT
                else -> TouchAction.MENU
            }
        }
        TouchZoneLayout.KINDLE -> {
            when {
                yFraction < 0.35f -> TouchAction.MENU
                xFraction < 0.33f -> TouchAction.PREV
                else -> TouchAction.NEXT
            }
        }

        /** Same as DEFAULT */

        // 邊緣式 (圖三): Left/right narrow edges (~15%) = NEXT, bottom strip (~15%) = PREV, center = MENU
        TouchZoneLayout.EDGE -> {
            when {
                yFraction > 0.85f -> TouchAction.PREV
                xFraction < 0.15f -> TouchAction.NEXT
                xFraction > 0.85f -> TouchAction.NEXT
                else -> TouchAction.MENU
            }
        }

        // 左右式 (圖四): Left ~25% = PREV, right ~25% = NEXT, center ~50% = MENU
        TouchZoneLayout.LEFT_RIGHT -> {
            when {
                xFraction < 0.25f -> TouchAction.PREV
                xFraction > 0.75f -> TouchAction.NEXT
                else -> TouchAction.MENU
            }
        }

        TouchZoneLayout.DISABLED -> null
    }
}

/** Kindle : Top 35% = menu, bottom-left 33% = prev, bottom-right 67% = next */
@Composable
private fun KindleZoneLayout() {
    Column(modifier = Modifier.fillMaxSize()) {
        ZoneCell(TouchAction.MENU, Modifier.fillMaxWidth().weight(0.35f))
        Row(modifier = Modifier.weight(0.65f).fillMaxWidth()) {
            ZoneCell(TouchAction.PREV, Modifier.weight(1f).fillMaxHeight())
            ZoneCell(TouchAction.NEXT, Modifier.weight(2f).fillMaxHeight())
        }
    }
}

/** L Shape : Top strip + left column = prev, bottom strip + right column = next, center = menu */
@Composable
private fun LShapeZoneLayout() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top strip: full width PREV
        ZoneCell(TouchAction.PREV, Modifier.fillMaxWidth().weight(0.2f))
        // Middle: left PREV | center MENU | right NEXT
        Row(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
            ZoneCell(TouchAction.PREV, Modifier.weight(0.3f).fillMaxHeight())
            ZoneCell(TouchAction.MENU, Modifier.weight(0.4f).fillMaxHeight())
            ZoneCell(TouchAction.NEXT, Modifier.weight(0.3f).fillMaxHeight())
        }
        // Bottom: left PREV | right NEXT
        Row(modifier = Modifier.weight(0.2f).fillMaxWidth()) {
            ZoneCell(TouchAction.PREV, Modifier.weight(0.3f).fillMaxHeight())
            ZoneCell(TouchAction.NEXT, Modifier.weight(0.7f).fillMaxHeight())
        }
    }
}

/** Edge : Left/right edges = next, bottom strip = prev, center = menu */
@Composable
private fun EdgeZoneLayout() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Center = MENU (base layer)
        ZoneCell(TouchAction.MENU, Modifier.fillMaxSize())
        // Left edge = NEXT
        ZoneCell(TouchAction.NEXT, Modifier.fillMaxHeight().fillMaxWidth(0.15f).align(Alignment.CenterStart))
        // Right edge = NEXT
        ZoneCell(TouchAction.NEXT, Modifier.fillMaxHeight().fillMaxWidth(0.15f).align(Alignment.CenterEnd))
        // Bottom strip = PREV
        ZoneCell(TouchAction.PREV, Modifier.fillMaxWidth().fillMaxHeight(0.15f).align(Alignment.BottomCenter))
    }
}

/** Left-Right : Left 25% = prev, center 50% = menu, right 25% = next */
@Composable
private fun LeftRightZoneLayout() {
    Row(modifier = Modifier.fillMaxSize()) {
        ZoneCell(TouchAction.PREV, Modifier.weight(0.25f).fillMaxHeight())
        ZoneCell(TouchAction.MENU, Modifier.weight(0.50f).fillMaxHeight())
        ZoneCell(TouchAction.NEXT, Modifier.weight(0.25f).fillMaxHeight())
    }
}

@Composable
private fun ZoneCell(action: TouchAction, modifier: Modifier = Modifier) {
    val color = when (action) {
        TouchAction.PREV -> Color(0x44FF9800)
        TouchAction.NEXT -> Color(0x4400BCD4)
        TouchAction.MENU -> Color(0x449C27B0)
    }
    Box(
        modifier = modifier.background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action.label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
