package me.thenano.yamibo.yamibo_app.favorite.sync

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.*
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.time.Duration.Companion.milliseconds

class IFavoriteSyncProgressScreen(
    private val runId: String,
) : Navigatable {
    override val id = buildId(runId)

    @Composable
    override fun Content() {
        FavoriteSyncProgressScreen(runId)
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
                            text = "同步百合會收藏",
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
                            text = "找不到這個同步任務。",
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
                            text = "關閉這個畫面不會中止同步，你之後仍可從收藏頁或設定頁重新查看進度。",
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
                        text = "目標類別：${categoryName ?: "預設"}",
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
                SyncMetricRow("最後一次同步時間", formatSyncDateTime(lastSyncTimestamp))
                SyncMetricRow("同步花費時間", formatSyncDuration(displayedElapsedDuration))
                progressUi.lines.forEach { line ->
                    SyncMetricRow(line.first, line.second)
                }
            }

            snapshot.logMessage?.takeIf { it.isNotBlank() }?.let {
                SyncMessageBlock(
                    title = "日誌",
                    message = it,
                    tint = colors.brownPrimary,
                    maxHeight = messageMaxHeight,
                )
            }

            snapshot.warningMessage?.takeIf { it.isNotBlank() }?.let {
                SyncMessageBlock(
                    title = "提醒",
                    message = it,
                    tint = colors.brownDeep,
                    maxHeight = messageMaxHeight,
                )
            }

            snapshot.errorMessage?.takeIf { it.isNotBlank() }?.let {
                SyncMessageBlock(
                    title = "錯誤",
                    message = it,
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
                        Text(if (state is FavoriteSyncState.Running) "查看進度" else "打開進度")
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
                        Text("中斷同步")
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
                        Text(if (state is FavoriteSyncState.Completed) "重新同步" else "繼續同步")
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
                        Text("隱藏")
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
        "${hours}時 ${minutes}分 ${seconds}秒"
    } else if (minutes > 0) {
        "${minutes}分 ${seconds}秒"
    } else {
        "${seconds}秒"
    }
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}

private fun FavoriteSyncState.snapshotOrNull(): FavoriteSyncSnapshot? {
    return when (this) {
        FavoriteSyncState.Idle -> null
        is FavoriteSyncState.Running -> snapshot
        is FavoriteSyncState.Interrupted -> snapshot
        is FavoriteSyncState.Failed -> snapshot
        is FavoriteSyncState.Completed -> snapshot
    }
}

private fun FavoriteSyncState.title(): String {
    return when (this) {
        FavoriteSyncState.Idle -> "尚未開始同步"
        is FavoriteSyncState.Running -> "背景同步中"
        is FavoriteSyncState.Interrupted -> "同步已中斷"
        is FavoriteSyncState.Failed -> "同步失敗"
        is FavoriteSyncState.Completed -> "同步完成"
    }
}

private data class SyncProgressUi(
    val progress: Float,
    val label: String,
    val lines: List<Pair<String, String>>,
)

private fun FavoriteSyncSnapshot.toProgressUi(): SyncProgressUi {
    val safeScannedCount = scannedCount.coerceAtLeast(1)
    val importedProcessed = (importedCount + failedCount).coerceAtMost(safeScannedCount)
    val uploadProgress = when {
        uploadTargetCount <= 0 && phase != FavoriteSyncPhase.PREPARING && phase != FavoriteSyncPhase.FETCHING_REMOTE -> 1f
        uploadTargetCount > 0 -> (uploadedCount.toFloat() / uploadTargetCount.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }
    val fetchProgress = totalPages
        ?.takeIf { it > 0 }
        ?.let { (currentPage.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        ?: if (currentPage > 0) 0.15f else 0f

    val progress = when (phase) {
        FavoriteSyncPhase.PREPARING -> 0.03f
        FavoriteSyncPhase.FETCHING_REMOTE -> 0.05f + (0.35f * fetchProgress)
        FavoriteSyncPhase.IMPORTING_REMOTE -> 0.40f + (0.40f * (importedProcessed.toFloat() / safeScannedCount.toFloat()))
        FavoriteSyncPhase.UPLOADING_LOCAL -> 0.80f + (0.15f * uploadProgress)
        FavoriteSyncPhase.RECONCILING_REMOTE -> 0.95f
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> snapshotFrozenProgress()
        FavoriteSyncPhase.COMPLETED -> 1f
    }.coerceIn(0f, 1f)

    return when (phase) {
        FavoriteSyncPhase.PREPARING -> SyncProgressUi(
            progress = progress,
            label = "準備同步任務",
            lines = listOf("狀態" to "正在建立同步任務"),
        )

        FavoriteSyncPhase.FETCHING_REMOTE -> SyncProgressUi(
            progress = progress,
            label = "開始同步",
            lines = buildList {
                add("頁數" to if (currentPage <= 0) "正在取得收藏頁" else "${currentPage}/${totalPages ?: "?"} 頁")
                add("已取得" to "$scannedCount 項收藏")
            },
        )

        FavoriteSyncPhase.IMPORTING_REMOTE,
        FavoriteSyncPhase.UPLOADING_LOCAL,
        FavoriteSyncPhase.RECONCILING_REMOTE,
        FavoriteSyncPhase.COMPLETED,
        FavoriteSyncPhase.INTERRUPTED,
        FavoriteSyncPhase.FAILED -> SyncProgressUi(
            progress = progress,
            label = if (phase == FavoriteSyncPhase.COMPLETED) "同步完成" else "匯入網站帖子",
            lines = listOf(
                "已匯入到本地" to "${importedCount}/${scannedCount}",
                "已同步至百合會" to uploadedCount.toString(),
                "同步失敗" to failedCount.toString(),
            ),
        )
    }
}

private fun FavoriteSyncSnapshot.snapshotFrozenProgress(): Float {
    val fetchProgress = totalPages
        ?.takeIf { it > 0 }
        ?.let { (currentPage.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
        ?: if (currentPage > 0) 0.15f else 0f
    val safeScannedCount = scannedCount.coerceAtLeast(1)
    val importProgress = (importedCount + failedCount).coerceAtMost(safeScannedCount).toFloat() / safeScannedCount.toFloat()
    val uploadProgress = when {
        uploadTargetCount <= 0 && importedCount > 0 -> 1f
        uploadTargetCount > 0 -> (uploadedCount.toFloat() / uploadTargetCount.toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    return when {
        uploadedCount > 0 || uploadTargetCount > 0 -> 0.80f + (0.15f * uploadProgress)
        importedCount > 0 || failedCount > 0 -> 0.40f + (0.40f * importProgress)
        currentPage > 0 || totalPages != null -> 0.05f + (0.35f * fetchProgress)
        else -> 0.03f
    }.coerceIn(0f, 0.99f)
}
