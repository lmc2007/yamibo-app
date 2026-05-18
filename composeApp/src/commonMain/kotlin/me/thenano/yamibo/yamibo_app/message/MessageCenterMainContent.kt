package me.thenano.yamibo.yamibo_app.message

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.User
import me.thenano.yamibo.yamibo_app.components.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.YamiboEmptyContent
import me.thenano.yamibo.yamibo_app.components.YamiboPageNavigation
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateStatusCard
import me.thenano.yamibo.yamibo_app.favorite.updates.snapshotOrNull
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

@Composable
internal fun MessageCenterMainContent(
    content: MessageCenterContent,
    selectedTab: MessageCenterTab,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    onUpdateEventClick: (FavoriteUpdateRepository.UpdateEvent) -> Unit,
    onDismissUpdateEvent: (FavoriteUpdateRepository.UpdateEvent) -> Unit,
    onGlobalFavoriteUpdate: () -> Unit,
    onCancelFavoriteUpdate: (String) -> Unit,
    onInterruptFavoriteUpdate: (String) -> Unit,
    onResumeFavoriteUpdate: () -> Unit,
    favoriteUpdateInterval: FavoriteUpdateInterval,
    onFavoriteUpdateIntervalChange: (FavoriteUpdateInterval) -> Unit,
    favoriteUpdateHiddenRunId: String,
    onHideFavoriteUpdateStatus: (String) -> Unit,
    onToggleFavoriteUpdateFid: (Int, Boolean) -> Unit,
    onUserClick: (User) -> Unit,
    onOpenPrivateMessage: (User) -> Unit,
    onMessageAction: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        when (content) {
            is MessageCenterContent.Updates -> {
                item {
                    FavoriteUpdateHeader(
                        filters = content.filters,
                        runState = content.runState,
                        onGlobalFavoriteUpdate = onGlobalFavoriteUpdate,
                        onCancelFavoriteUpdate = onCancelFavoriteUpdate,
                        onInterruptFavoriteUpdate = onInterruptFavoriteUpdate,
                        onResumeFavoriteUpdate = onResumeFavoriteUpdate,
                        favoriteUpdateInterval = favoriteUpdateInterval,
                        onFavoriteUpdateIntervalChange = onFavoriteUpdateIntervalChange,
                        favoriteUpdateHiddenRunId = favoriteUpdateHiddenRunId,
                        onHideFavoriteUpdateStatus = onHideFavoriteUpdateStatus,
                        onToggleFavoriteUpdateFid = onToggleFavoriteUpdateFid,
                    )
                }
                if (content.events.isEmpty()) item { MessageCenterEmptyListMessage(appString(Res.string.ui_no_updates_detected)) }
                items(content.events, key = { it.id }) { event ->
                    FavoriteUpdateCard(event = event, onClick = { onUpdateEventClick(event) })
                }
            }
            is MessageCenterContent.PrivateMessages -> {
                if (content.page.messages.isEmpty()) item { MessageCenterEmptyListMessage(emptyMessage(selectedTab)) }
                items(content.page.messages, key = { "${it.user.uid.value}_${it.timeInfo.text}" }) { message ->
                    PrivateMessageCard(
                        message,
                        onUserClick = { onUserClick(message.user) },
                        onAction = { onOpenPrivateMessage(message.user) },
                    )
                }
                content.page.pageNav?.let { nav -> item { MessageCenterPageNavigation(nav, currentPage, onPageChange) } }
            }
            is MessageCenterContent.Notices -> {
                if (content.page.notices.isEmpty()) item { MessageCenterEmptyListMessage(emptyMessage(selectedTab)) }
                items(content.page.notices, key = { it.noticeId.value }) { notice ->
                    NoticeCard(notice, onAction = onMessageAction)
                }
                content.page.pageNav?.let { nav -> item { MessageCenterPageNavigation(nav, currentPage, onPageChange) } }
            }
        }
    }
}

@Composable
private fun MessageCenterEmptyListMessage(message: String) {
    YamiboEmptyContent(message = message, modifier = Modifier.padding(horizontal = 24.dp, vertical = 80.dp))
}

@Composable
private fun FavoriteUpdateHeader(
    filters: List<FavoriteUpdateRepository.FidFilter>,
    runState: FavoriteUpdateRepository.RunState,
    onGlobalFavoriteUpdate: () -> Unit,
    onCancelFavoriteUpdate: (String) -> Unit,
    onInterruptFavoriteUpdate: (String) -> Unit,
    onResumeFavoriteUpdate: () -> Unit,
    favoriteUpdateInterval: FavoriteUpdateInterval,
    onFavoriteUpdateIntervalChange: (FavoriteUpdateInterval) -> Unit,
    favoriteUpdateHiddenRunId: String,
    onHideFavoriteUpdateStatus: (String) -> Unit,
    onToggleFavoriteUpdateFid: (Int, Boolean) -> Unit,
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    val running = (runState as? FavoriteUpdateRepository.RunState.Running)?.snapshot
    val interrupted = (runState as? FavoriteUpdateRepository.RunState.Interrupted)?.snapshot
    val snapshot = runState.snapshotOrNull()
    val statusVisible = snapshot != null &&
        (runState is FavoriteUpdateRepository.RunState.Running || favoriteUpdateHiddenRunId != snapshot.runId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(appString(Res.string.ui_favorite_updates), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                YamiboActionChip(appString(Res.string.message_update_interval_chip, favoriteUpdateInterval.localizedLabel()), onClick = { showIntervalDialog = true })
                YamiboActionChip(appString(Res.string.read_history_filter_prefix), onClick = { showFilterDialog = true })
                when {
                    interrupted != null -> YamiboActionChip(appString(Res.string.ui_continue), onClick = onResumeFavoriteUpdate)
                    running == null -> YamiboActionChip(appString(Res.string.ui_global_refresh), onClick = onGlobalFavoriteUpdate)
                }
            }
        }

        if (statusVisible) {
            FavoriteUpdateStatusCard(
                state = runState,
                modifier = Modifier.fillMaxWidth(),
                onCancel = onCancelFavoriteUpdate,
                onInterrupt = onInterruptFavoriteUpdate,
                onResume = onResumeFavoriteUpdate,
                onHide = { onHideFavoriteUpdateStatus(snapshot.runId) },
            )
        }
    }

    if (showFilterDialog) {
        FavoriteUpdateFidFilterDialog(
            filters = filters,
            onDismiss = { showFilterDialog = false },
            onToggle = onToggleFavoriteUpdateFid,
        )
    }
    if (showIntervalDialog) {
        FavoriteUpdateIntervalDialog(
            selected = favoriteUpdateInterval,
            onDismiss = { showIntervalDialog = false },
            onSelect = onFavoriteUpdateIntervalChange,
        )
    }
}

@Composable
private fun FavoriteUpdateIntervalDialog(
    selected: FavoriteUpdateInterval,
    onDismiss: () -> Unit,
    onSelect: (FavoriteUpdateInterval) -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appString(Res.string.ui_refresh_cycle), color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(FavoriteUpdateInterval.entries, key = { it.name }) { interval ->
                    Surface(
                        onClick = { onSelect(interval) },
                        color = if (interval == selected) colors.brownPrimary.copy(alpha = 0.16f) else colors.creamSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = interval.localizedLabel(),
                                color = colors.textDark,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                            if (interval == selected) {
                                Text(appString(Res.string.ui_selected), color = colors.brownDeep, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { YamiboActionChip(appString(Res.string.common_close), onDismiss) },
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun FavoriteUpdateFidFilterDialog(
    filters: List<FavoriteUpdateRepository.FidFilter>,
    onDismiss: () -> Unit,
    onToggle: (Int, Boolean) -> Unit,
) {
    val colors = YamiboTheme.colors
    var localFilters by remember(filters) { mutableStateOf(filters) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appString(Res.string.ui_filter_categories), color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(localFilters, key = { it.fid }) { filter ->
                    Surface(
                        onClick = {
                            val enabled = !filter.enabled
                            localFilters = localFilters.map { if (it.fid == filter.fid) it.copy(enabled = enabled) else it }
                            onToggle(filter.fid, enabled)
                        },
                        color = if (filter.enabled) colors.brownPrimary.copy(alpha = 0.16f) else colors.creamSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = filter.enabled,
                                onCheckedChange = { checked ->
                                    localFilters = localFilters.map { if (it.fid == filter.fid) it.copy(enabled = checked) else it }
                                    onToggle(filter.fid, checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.brownDeep,
                                    uncheckedColor = colors.brownDeep.copy(alpha = 0.6f),
                                    checkmarkColor = colors.creamSurface,
                                ),
                            )
                            Text(
                                text = "${filter.forumName} (${filter.itemCount})",
                                color = colors.textDark,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { YamiboActionChip(appString(Res.string.common_close), onDismiss) },
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun FavoriteUpdateCard(
    event: FavoriteUpdateRepository.UpdateEvent,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(82.dp)
                    .aspectRatio(0.72f),
                colors = CardDefaults.cardColors(containerColor = colors.brownLight.copy(alpha = 0.2f)),
            ) {
                val coverUrl = event.coverUrl
                if (!coverUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = rememberImageRequest(url = coverUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = event.title,
                            color = colors.brownDeep.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = event.title,
                    color = colors.textDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = event.latestPostTitle?.takeIf { it.isNotBlank() } ?: localizedAppMessage(event.summary),
                    color = colors.textDark.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.forumName?.takeIf { it.isNotBlank() }?.let {
                    Text("#$it", color = colors.textDark.copy(alpha = 0.56f), fontSize = 12.sp)
                }
                Text(
                    text = "${localizedAppMessage(event.summary)} / ${formatUpdateRelativeTime(event.detectedAt)}",
                    color = colors.textDark.copy(alpha = 0.48f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatUpdateRelativeTime(timestamp: Long): String {
    val elapsed = (currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = elapsed / 1000L / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        days > 0L -> appString(Res.string.time_days_ago, days)
        hours > 0L -> appString(Res.string.time_hours_ago, hours)
        minutes > 0L -> appString(Res.string.time_minutes_ago, minutes)
        else -> appString(Res.string.ui_just)
    }
}

private fun emptyMessage(tab: MessageCenterTab): String = when (tab) {
    MessageCenterTab.Updates -> appString(Res.string.ui_no_updates_detected)
    MessageCenterTab.PrivateMessages -> appString(Res.string.ui_no_message_found)
    MessageCenterTab.Notices -> appString(Res.string.ui_reminder_not_found)
}

@Composable
private fun MessageCenterPageNavigation(pageNav: PageNav, currentPage: Int, onPageChange: (Int) -> Unit) {
    YamiboPageNavigation(pageNav = pageNav, currentPage = currentPage, onPageChange = onPageChange)
}


