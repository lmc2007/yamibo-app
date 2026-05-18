package me.thenano.yamibo.yamibo_app.forum.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

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
            forum.todayCount?.let { count -> StatChip(label = appString(Res.string.ui_today), value = "$count") }
            forum.themeCount?.let { count -> StatChip(label = appString(Res.string.ui_theme), value = "$count") }
            forum.rank?.let { rank -> StatChip(label = appString(Res.string.ui_ranking), value = "$rank") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showOrder) {
                ForumFilterChip(appString(Res.string.forum_order_chip, selectedOrderType?.name ?: appString(Res.string.common_all)), onOrderClick)
            }
            if (showFilter) {
                ForumFilterChip(appString(Res.string.forum_filter_chip, selectedFilterType?.name ?: appString(Res.string.common_all)), onFilterClick)
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
            color = colors.brownDeep,
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
            text = appString(Res.string.ui_subreddit_area),
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
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.announceBg)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(6.dp), color = colors.orangeAccent) {
            Text(
                text = appString(Res.string.ui_announcement),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = announcement.title,
            fontSize = 14.sp,
            color = colors.brownDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Pinned thread row */
@Composable
private fun PinnedThreadRow(thread: PinnedItem.Thread, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 3.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.pinnedBg)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(6.dp), color = colors.brownPrimary) {
            Text(
                text = appString(Res.string.ui_pin_top),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = thread.title,
            fontSize = 14.sp,
            color = colors.brownDeep,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


