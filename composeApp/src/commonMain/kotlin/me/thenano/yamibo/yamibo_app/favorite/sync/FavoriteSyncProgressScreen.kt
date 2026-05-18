package me.thenano.yamibo.yamibo_app.favorite.sync

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.*
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.time.Duration.Companion.milliseconds

@Serializable
private data class FavoriteSyncProgressRestorePayload(
    val runId: String,
)
@RestorableScreenEntry
class IFavoriteSyncProgressScreen(
    val runId: String,
) : RestorableNavigatable {
    override val id = buildId(runId)
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = FavoriteSyncProgressRestorePayload(runId = runId),
    )

    @Composable
    override fun Content() {
        FavoriteSyncProgressScreen(runId)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<IFavoriteSyncProgressScreen>(IFavoriteSyncProgressScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<FavoriteSyncProgressRestorePayload>(payload)
            return IFavoriteSyncProgressScreen(runId = data.runId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteSyncProgressScreen(runId: String) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val runner = LocalFavoriteSyncRunner.current
    val state by runner.state.collectAsState()
    val snapshot = state.snapshotOrNull()
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints {
        val messageMaxHeight = maxHeight / 4

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = appString(Res.string.ui_synchronous_yamibo_favorite),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.brownDeep,
                        scrolledContainerColor = colors.brownDeep,
                    ),
                )
            },
            containerColor = colors.creamBackground,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    snapshot == null || snapshot.runId != runId -> {
                        Text(
                            text = appString(Res.string.ui_the_synchronization_task_cannot_found),
                            color = colors.textDark,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    else -> {
                        FavoriteSyncStatusCard(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                            messageMaxHeight = messageMaxHeight,
                            onInterrupt = if (state is FavoriteSyncState.Running) {
                                {
                                    coroutineScope.launch {
                                        runner.interruptImport(runId)
                                    }
                                }
                            } else {
                                null
                            },
                        )
                        Text(
                            text = appString(Res.string.ui_closing_screen_not_abort_synchronization_can_still_check_progress),
                            color = colors.textDark.copy(alpha = 0.72f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteSyncStatusCard(
    state: FavoriteSyncState,
    modifier: Modifier = Modifier,
    onOpenProgress: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onInterrupt: (() -> Unit)? = null,
    messageMaxHeight: Dp = 180.dp,
) {
    val colors = YamiboTheme.colors
    val favoriteRepository = LocalFavoriteRepository.current
    val coroutineScope = rememberCoroutineScope()
    val snapshot = state.snapshotOrNull() ?: return
    var now by remember(state, snapshot.runId) { mutableLongStateOf(currentTimeMillis()) }
    LaunchedEffect(state, snapshot.runId) {
        if (state is FavoriteSyncState.Running) {
            while (true) {
                now = currentTimeMillis()
                delay(1000.milliseconds)
            }
        } else {
            now = currentTimeMillis()
        }
    }
    val progressUi = remember(snapshot, now, state) { snapshot.toProgressUi() }
    val displayedElapsedDuration = if (state is FavoriteSyncState.Running) {
        (now - snapshot.startedAt).coerceAtLeast(0L)
    } else {
        snapshot.elapsedDurationMs
    }
    val lastSyncTimestamp = snapshot.lastCompletedAt ?: snapshot.updatedAt
    var categoryName by remember(snapshot.targetCategoryId) { mutableStateOf<String?>(null) }

    LaunchedEffect(snapshot.targetCategoryId) {
        categoryName = withContext(Dispatchers.Default) {
            favoriteRepository.getCategories().firstOrNull { it.id == snapshot.targetCategoryId }?.name
        }
    }

    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color = colors.creamSurface,
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title(),
                        color = colors.brownDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = appString(Res.string.favorite_sync_target_category, categoryName?.let(::localizedAppMessage) ?: appString(Res.string.ui_default)),
                        color = colors.textDark.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = "${(progressUi.progress * 100).toInt()}%",
                    color = colors.brownDeep,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = progressUi.label,
                    color = colors.textDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                LinearProgressIndicator(
                    progress = { progressUi.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = colors.brownDeep,
                    trackColor = colors.brownPrimary.copy(alpha = 0.18f),
                )
                SyncMetricRow(appString(Res.string.ui_last_sync_time), formatSyncDateTime(lastSyncTimestamp))
                SyncMetricRow(appString(Res.string.ui_synchronization_takes_time), formatSyncDuration(displayedElapsedDuration))
                progressUi.lines.forEach { line ->
                    SyncMetricRow(line.first, line.second)
                }
            }

            snapshot.logMessage?.takeIf { it.isNotBlank() }?.let {
                SyncMessageBlock(
                    title = appString(Res.string.ui_blog_traditional_label),
                    message = localizedAppMessage(it),
                    tint = colors.brownPrimary,
                    maxHeight = messageMaxHeight,
                )
            }

            snapshot.warningMessage?.takeIf { it.isNotBlank() }?.let {
                SyncMessageBlock(
                    title = appString(Res.string.ui_remind),
                    message = localizedAppMessage(it),
                    tint = colors.brownDeep,
                    maxHeight = messageMaxHeight,
                )
            }

            snapshot.errorMessage?.takeIf { it.isNotBlank() }?.let {
                SyncMessageBlock(
                    title = appString(Res.string.ui_mistake),
                    message = localizedAppMessage(it),
                    tint = Color(0xFFB74D42),
                    maxHeight = messageMaxHeight,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onOpenProgress != null) {
                    Button(
                        onClick = onOpenProgress,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brownDeep,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(if (state is FavoriteSyncState.Running) appString(Res.string.ui_view_progress) else appString(Res.string.ui_open_progress))
                    }
                }
                if (onInterrupt != null && state is FavoriteSyncState.Running) {
                    Button(
                        onClick = onInterrupt,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8C4B3B),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(appString(Res.string.ui_interrupt_synchronization))
                    }
                }
                if (onResume != null && state !is FavoriteSyncState.Running) {
                    Button(
                        onClick = { coroutineScope.launch { onResume() } },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brownPrimary.copy(alpha = 0.18f),
                            contentColor = colors.brownDeep,
                        ),
                    ) {
                        Text(if (state is FavoriteSyncState.Completed) appString(Res.string.ui_resync) else appString(Res.string.ui_continue_syncing))
                    }
                }
                if (onDismiss != null) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.textDark.copy(alpha = 0.06f),
                            contentColor = colors.textDark.copy(alpha = 0.82f),
                        ),
                    ) {
                        Text(appString(Res.string.ui_hide))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncMetricRow(label: String, value: String) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textDark.copy(alpha = 0.62f),
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = colors.textDark,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun SyncMessageBlock(
    title: String,
    message: String,
    tint: Color,
    maxHeight: Dp,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val lines = remember(message) {
        message
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
    }
    val viewportPx = with(density) { maxHeight.roundToPx() }.toFloat().coerceAtLeast(1f)
    val thumbMetrics by remember(scrollState, viewportPx) {
        derivedStateOf {
            val totalScrollablePx = scrollState.maxValue.toFloat()
            val totalContentPx = viewportPx + totalScrollablePx
            val heightFraction =
                if (totalContentPx <= 0f) 1f else (viewportPx / totalContentPx).coerceIn(0.18f, 1f)
            val offsetFraction =
                if (totalScrollablePx <= 0f) 0f else (scrollState.value.toFloat() / totalScrollablePx).coerceIn(0f, 1f)
            heightFraction to offsetFraction
        }
    }
    val thumbHeightFraction = thumbMetrics.first
    val thumbOffsetFraction = thumbMetrics.second

    LaunchedEffect(message, lines.size, scrollState.maxValue) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.16f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 10.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    lines.forEach { line ->
                        Text(
                            text = line,
                            color = tint,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(tint.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(thumbHeightFraction)
                            .offset(y = maxHeight * ((1f - thumbHeightFraction) * thumbOffsetFraction))
                            .background(tint.copy(alpha = 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
                    )
                }
            }
        }
    }
}


private fun formatSyncDateTime(timestamp: Long): String {
    val totalDays = timestamp / (24 * 60 * 60 * 1000L)
    var year = 1970
    var remainingDays = totalDays + (8 * 60 * 60 * 1000L / (24 * 60 * 60 * 1000L))
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366L else 365L
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }
    val monthDays = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (days in monthDays) {
        if (remainingDays < days) break
        remainingDays -= days
        month++
    }
    val day = remainingDays.toInt() + 1
    val adjustedMs = timestamp + 8 * 60 * 60 * 1000L
    val totalMinutes = (adjustedMs / (60 * 1000L)) % (24 * 60)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')} ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun formatSyncDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        appString(Res.string.favorite_sync_elapsed_hms, hours, minutes, seconds)
    } else if (minutes > 0) {
        appString(Res.string.favorite_sync_elapsed_ms, minutes, seconds)
    } else {
        appString(Res.string.favorite_sync_elapsed_s, seconds)
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}


