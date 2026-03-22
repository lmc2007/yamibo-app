package me.thenano.yamibo.yamibo_app.thread.reader.components.manga

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Reading mode configuration */
enum class ReadingMode(val label: String) {
    SINGLE_LTR("單頁(左至右)"),
    SINGLE_RTL("單頁(右至左)"),
    SINGLE_TTB("單頁(上至下)"),
    SCROLL_CONTINUOUS("捲動(連續)"),
    SCROLL_GAP("捲動(留空)")
}

/**
 * Settings panel for the manga reader.
 * Slides up from the bottom and contains reading mode and touch zone layout options.
 */
@Composable
fun MangaReaderSettingsPanel(
    visible: Boolean,
    currentReadingMode: ReadingMode,
    currentTouchZoneLayout: TouchZoneLayout,
    onReadingModeChange: (ReadingMode) -> Unit,
    onTouchZoneLayoutChange: (TouchZoneLayout) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = YamiboTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            color = colors.brownDeep,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                // Prevent touches from falling through to the scrim
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Reading mode
                SectionTitle("閱讀模式")
                ChipGroup(
                    options = ReadingMode.entries.toList(),
                    selectedOption = currentReadingMode,
                    labelExtractor = { it.label },
                    onSelect = onReadingModeChange
                )

                Spacer(Modifier.height(20.dp))

                // Touch zone layout
                SectionTitle("輕觸區域")
                ChipGroup(
                    options = TouchZoneLayout.entries.toList(),
                    selectedOption = currentTouchZoneLayout,
                    labelExtractor = { it.label },
                    onSelect = onTouchZoneLayoutChange
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun <T> ChipGroup(
    options: List<T>,
    selectedOption: T,
    labelExtractor: (T) -> String,
    onSelect: (T) -> Unit
) {
    // Use FlowRow-like layout with wrapping
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val chunked = options.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { option ->
                    Chip(
                        option = option,
                        isSelected = option == selectedOption,
                        label = labelExtractor(option),
                        onSelect = onSelect
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> Chip(
    option: T,
    isSelected: Boolean,
    label: String,
    onSelect: (T) -> Unit
) {
    val colors = YamiboTheme.colors
    val bgColor = if (isSelected) colors.brownPrimary else Color.White.copy(alpha = 0.1f)
    val textColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)

    Text(
        text = label,
        color = textColor,
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onSelect(option) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
