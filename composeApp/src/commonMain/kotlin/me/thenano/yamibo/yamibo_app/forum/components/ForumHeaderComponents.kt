package me.thenano.yamibo.yamibo_app.forum.components

import me.thenano.yamibo.yamibo_app.i18n.i18n

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.page.FilterType
import io.github.littlesurvival.dto.page.OrderType
import io.github.littlesurvival.dto.page.PinnedItem
import io.github.littlesurvival.dto.value.ForumId
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/** Forum stats bar (today/theme/rank with red accent) */
@Composable
fun ForumStatsBar(
    forum: ForumSummary,
    selectedOrderType: OrderType? = null,
    selectedFilterType: FilterType? = null,
    showOrder: Boolean = false,
    showFilter: Boolean = false,
    onOrderClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
) {
    val colors = YamiboTheme.colors
    val hasStats = forum.todayCount != null || forum.themeCount != null || forum.rank != null
    if (!hasStats && !showOrder && !showFilter) return

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.brownDeep,
                            colors.brownPrimary.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            forum.todayCount?.let { count -> StatChip(label = i18n("今日"), value = "$count") }
            forum.themeCount?.let { count -> StatChip(label = i18n("主題"), value = "$count") }
            forum.rank?.let { rank -> StatChip(label = i18n("排名"), value = "$rank") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showOrder) {
                ForumFilterChip(i18n("排序: {}", (selectedOrderType?.name ?: i18n("全部"))), onOrderClick)
            }
            if (showFilter) {
                ForumFilterChip(i18n("分類: {}", (selectedFilterType?.name ?: i18n("全部"))), onFilterClick)
            }
        }
    }
}

/** Stat chip with red accent value text */
@Composable
private fun StatChip(label: String, value: String) {
    val colors = YamiboTheme.colors
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.18f)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                color = colors.redAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ForumFilterChip(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = colors.creamSurface) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = colors.textOnSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Sub forum horizontal row */
@Composable
fun SubForumRow(subForums: List<ForumSummary>, onClick: (ForumId, String) -> Unit) {
    val colors = YamiboTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            text = i18n("子版區"),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.brownPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(subForums, key = { it.fid.value }) { sub ->
                Surface(
                    onClick = { onClick(sub.fid, sub.name) },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.creamSurface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📂", fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = sub.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textDark
                        )
                    }
                }
            }
        }
    }
}

/** Pinned items section */
@Composable
fun PinnedSection(items: List<PinnedItem>, onItemClick: (PinnedItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        items.forEach { item ->
            when (item) {
                is PinnedItem.Announcement -> AnnouncementRow(item, onClick = { onItemClick(item) })
                is PinnedItem.Thread -> PinnedThreadRow(item, onClick = { onItemClick(item) })
            }
        }
    }
}

/** Announcement row */
@Composable
private fun AnnouncementRow(announcement: PinnedItem.Announcement, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "announcement_row_press_scale",
    )
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 3.dp)
                .scale(scale)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.announceBg)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(6.dp), color = colors.orangeAccent) {
            Text(
                text = i18n("公告"),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textOnAccent
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = announcement.title,
            fontSize = 14.sp,
            color = colors.textOnTint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Pinned thread row */
@Composable
private fun PinnedThreadRow(thread: PinnedItem.Thread, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(150),
        label = "pinned_thread_row_press_scale",
    )
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 3.dp)
                .scale(scale)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.pinnedBg)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(6.dp), color = colors.brownPrimary) {
            Text(
                text = i18n("置頂"),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textOnPrimary
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = thread.title,
            fontSize = 14.sp,
            color = colors.textOnTint,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
