package me.thenano.yamibo.yamibo_app.updates

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.controls.YamiboMultiSelectDialog
import me.thenano.yamibo.yamibo_app.components.controls.YamiboSingleSelectDialog
import me.thenano.yamibo.yamibo_app.components.controls.YamiboSingleSelectRow
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboEmptyContent
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboErrorContent
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboLoadingContent
import me.thenano.yamibo.yamibo_app.components.feedback.resolvedContentCoverUrl
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboMainTabIconAction
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboMainTabTopBar
import me.thenano.yamibo.yamibo_app.components.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateStatusCard
import me.thenano.yamibo.yamibo_app.favorite.updates.snapshotOrNull
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.util.time.formatRelativeTime

private sealed interface UpdatesScreenState {
    data object Loading : UpdatesScreenState
    data class Success(
        val events: List<FavoriteUpdateRepository.UpdateEvent>,
        val filters: List<FavoriteUpdateRepository.FidFilter>,
        val categoryFilters: List<FavoriteUpdateRepository.CategoryFilter>,
        val scopeTargets: List<FavoriteUpdateRepository.ScopeTarget>,
    ) : UpdatesScreenState
    data class Error(val message: String) : UpdatesScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen() {
    val colors = YamiboTheme.colors
    val favoriteUpdateRepository = LocalFavoriteUpdateRepository.current
    val favoriteUpdateRunner = LocalFavoriteUpdateRunner.current
    val favoriteUpdateRunState by favoriteUpdateRunner.state.collectAsState()
    val favoriteUpdateRefreshKey = favoriteUpdateRunState.refreshKey()
    val appSettingsRepository = LocalAppSettingsRepository.current
    val favoriteUpdateInterval = appSettingsRepository.favoriteUpdateInterval.state()
    val favoriteUpdateHiddenRunId = appSettingsRepository.favoriteUpdateHiddenRunId.state()
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<UpdatesScreenState>(UpdatesScreenState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedEventIds by remember { mutableStateOf(setOf<Long>()) }
    var showGlobalRefreshConfirm by remember { mutableStateOf(false) }
    var showUpdateScopeDialog by remember { mutableStateOf(false) }
    var showResultFilterDialog by remember { mutableStateOf(false) }
    var selectedResultFilterKeys by remember { mutableStateOf(setOf(UPDATE_RESULT_FILTER_ALL)) }

    val updateContent = state as? UpdatesScreenState.Success
    val updateScopeFilterActive = updateContent?.let {
        it.filters.isUpdateScopeFilterRestricted { filter -> filter.enabled } ||
            it.categoryFilters.isUpdateScopeFilterRestricted { filter -> filter.enabled }
    } == true

    suspend fun loadUpdates() {
        val updates = withContext(Dispatchers.Default) {
            UpdatesScreenState.Success(
                events = favoriteUpdateRepository.getActiveEvents(),
                filters = favoriteUpdateRepository.getFidFilters(),
                categoryFilters = favoriteUpdateRepository.getCategoryFilters(),
                scopeTargets = favoriteUpdateRepository.getScopeTargets(),
            )
        }
        state = updates
    }

    LaunchedEffect(Unit) {
        loadUpdates()
    }

    LaunchedEffect(favoriteUpdateRefreshKey) {
        loadUpdates()
    }

    DisposableEffect(isSelectMode, navigator) {
        if (!isSelectMode) {
            onDispose { }
        } else {
            val handler = {
                isSelectMode = false
                selectedEventIds = emptySet()
                true
            }
            navigator.backHandlers.add(handler)
            onDispose { navigator.backHandlers.remove(handler) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = { YamiboSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isSelectMode) {
                UpdatesSelectTopBar(
                    onSelectAll = {
                        val success = state as? UpdatesScreenState.Success
                        if (success != null) {
                            selectedEventIds = success.events.map { it.id }.toSet()
                        }
                    },
                    onClearAll = {
                        scope.launch {
                            favoriteUpdateRepository.dismissAllEvents()
                            isSelectMode = false
                            selectedEventIds = emptySet()
                            loadUpdates()
                            snackbarHostState.showSnackbar(i18n("已刪除全部更新紀錄"))
                        }
                    },
                    onCancel = {
                        isSelectMode = false
                        selectedEventIds = emptySet()
                    },
                    onDeleteSelected = {
                        if (selectedEventIds.isNotEmpty()) {
                            scope.launch {
                                val deletedCount = selectedEventIds.size
                                favoriteUpdateRepository.dismissEvents(selectedEventIds.toList())
                                isSelectMode = false
                                selectedEventIds = emptySet()
                                loadUpdates()
                                snackbarHostState.showSnackbar(i18n("已刪除 {} 項紀錄", deletedCount))
                            }
                        }
                    },
                    selectedCount = selectedEventIds.size
                )
            } else {
                YamiboMainTabTopBar(title = i18n("更新")) {
                    YamiboMainTabIconAction(
                        icon = YamiboIcons.FilterList,
                        contentDescription = i18n("範圍"),
                        onClick = { showUpdateScopeDialog = true },
                        iconSize = 26,
                        tint = if (updateScopeFilterActive) colors.orangeAccent else colors.textOnBackground,
                    )
                    YamiboMainTabIconAction(
                        icon = YamiboIcons.Trashcan,
                        contentDescription = i18n("多選刪除"),
                        onClick = {
                            isSelectMode = true
                            selectedEventIds = emptySet()
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(colors.creamBackground),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (val current = state) {
                    UpdatesScreenState.Loading -> YamiboLoadingContent()
                    is UpdatesScreenState.Error -> YamiboErrorContent(
                        message = current.message,
                        onRetry = {
                            state = UpdatesScreenState.Loading
                            scope.launch { loadUpdates() }
                        },
                    )
                    is UpdatesScreenState.Success -> PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            scope.launch {
                                loadUpdates()
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val resultFilterOptions = remember(current.events) {
                            buildUpdateResultFilterOptions(current.events)
                        }
                        val normalizedResultFilterKeys = remember(selectedResultFilterKeys, resultFilterOptions) {
                            normalizeUpdateResultFilterKeys(selectedResultFilterKeys, resultFilterOptions)
                        }
                        val filteredUpdateEvents = remember(current.events, normalizedResultFilterKeys, resultFilterOptions) {
                            filterUpdateEvents(current.events, normalizedResultFilterKeys, resultFilterOptions)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            item {
                                FavoriteUpdateHeader(
                                    runState = favoriteUpdateRunState,
                                    onGlobalFavoriteUpdate = { showGlobalRefreshConfirm = true },
                                    onCancelFavoriteUpdate = { runId ->
                                        scope.launch {
                                            favoriteUpdateRunner.cancelUpdate(runId)
                                            loadUpdates()
                                            snackbarHostState.showSnackbar(i18n("已取消收藏更新檢查"), duration = SnackbarDuration.Short)
                                        }
                                    },
                                    onInterruptFavoriteUpdate = { runId ->
                                        scope.launch {
                                            favoriteUpdateRunner.interruptUpdate(runId)
                                            loadUpdates()
                                            snackbarHostState.showSnackbar(i18n("已中斷收藏更新檢查"), duration = SnackbarDuration.Short)
                                        }
                                    },
                                    onResumeFavoriteUpdate = {
                                        scope.launch {
                                            when (val result = favoriteUpdateRunner.resumeInterruptedUpdate()) {
                                                is FavoriteUpdateRunner.LaunchResult.Started -> {
                                                    appSettingsRepository.favoriteUpdateHiddenRunId.setValue("")
                                                    loadUpdates()
                                                    snackbarHostState.showSnackbar(i18n("繼續檢查收藏更新"), duration = SnackbarDuration.Short)
                                                }
                                                is FavoriteUpdateRunner.LaunchResult.Rejected -> {
                                                    snackbarHostState.showSnackbar(i18n(result.reason), duration = SnackbarDuration.Short)
                                                }
                                                null -> {
                                                    snackbarHostState.showSnackbar(i18n("沒有可繼續的收藏更新任務"), duration = SnackbarDuration.Short)
                                                }
                                            }
                                        }
                                    },
                                    favoriteUpdateInterval = favoriteUpdateInterval,
                                    onFavoriteUpdateIntervalChange = { interval ->
                                        scope.launch {
                                            appSettingsRepository.favoriteUpdateInterval.setValue(interval)
                                            favoriteUpdateRunner.schedulePeriodicUpdate(interval)
                                            snackbarHostState.showSnackbar(
                                                i18n("刷新週期已改為 {}", interval.localizedLabel()),
                                                duration = SnackbarDuration.Short,
                                            )
                                        }
                                    },
                                    favoriteUpdateHiddenRunId = favoriteUpdateHiddenRunId,
                                    onHideFavoriteUpdateStatus = { runId ->
                                        appSettingsRepository.favoriteUpdateHiddenRunId.setValue(runId)
                                    },
                                    resultFilterActive = isUpdateResultFilterRestricted(normalizedResultFilterKeys, resultFilterOptions),
                                    resultFilterLabel = updateResultFilterLabel(normalizedResultFilterKeys, resultFilterOptions),
                                    onShowResultFilter = { showResultFilterDialog = true },
                                )
                            }
                            if (filteredUpdateEvents.isEmpty()) {
                                item {
                                    YamiboEmptyContent(
                                        message = i18n("沒有偵測到更新"),
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 80.dp)
                                    )
                                }
                            } else {
                                items(filteredUpdateEvents, key = { it.id }) { event ->
                                    FavoriteUpdateCard(
                                        event = event,
                                        isSelected = selectedEventIds.contains(event.id),
                                        onClick = {
                                            if (isSelectMode) {
                                                selectedEventIds = if (event.id in selectedEventIds) {
                                                    selectedEventIds - event.id
                                                } else {
                                                    selectedEventIds + event.id
                                                }
                                            } else {
                                                scope.launch { favoriteUpdateRepository.markEventRead(event.id) }
                                                navigateFavoriteUpdateEvent(event, navigator)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGlobalRefreshConfirm) {
        AlertDialog(
            onDismissRequest = { showGlobalRefreshConfirm = false },
            title = { Text(i18n("全域刷新收藏更新")) },
            text = { Text(i18n("將重新檢查所有收藏並建立新的更新任務。網站維護中可能會產生大量錯誤記錄。")) },
            confirmButton = {
                YamiboActionChip(text = i18n("開始刷新"), onClick = {
                    showGlobalRefreshConfirm = false
                    scope.launch {
                        when (val result = favoriteUpdateRunner.startGlobalRefresh()) {
                            is FavoriteUpdateRunner.LaunchResult.Started -> {
                                appSettingsRepository.favoriteUpdateHiddenRunId.setValue("")
                                snackbarHostState.showSnackbar(
                                    i18n("開始全域刷新收藏更新"),
                                    duration = SnackbarDuration.Short
                                )
                            }
                            is FavoriteUpdateRunner.LaunchResult.Rejected -> {
                                snackbarHostState.showSnackbar(i18n(result.reason), duration = SnackbarDuration.Short)
                            }
                        }
                    }
                })
            },
            dismissButton = { YamiboActionChip(text = i18n("取消"), onClick = { showGlobalRefreshConfirm = false }) },
            containerColor = colors.creamSurface,
            titleContentColor = colors.textStrong,
            textContentColor = colors.textDark,
        )
    }

    val updateSuccess = state as? UpdatesScreenState.Success
    if (showResultFilterDialog && updateSuccess != null) {
        val options = buildUpdateResultFilterOptions(updateSuccess.events)
        UpdateResultFilterDialog(
            options = options,
            selectedKeys = normalizeUpdateResultFilterKeys(selectedResultFilterKeys, options),
            onDismiss = { showResultFilterDialog = false },
            onConfirm = { selected ->
                selectedResultFilterKeys = normalizeUpdateResultFilterKeys(selected, options)
                showResultFilterDialog = false
            },
        )
    }

    if (showUpdateScopeDialog && updateSuccess != null) {
        FavoriteUpdateScopeDialog(
            forumFilters = updateSuccess.filters,
            categoryFilters = updateSuccess.categoryFilters,
            scopeTargets = updateSuccess.scopeTargets,
            onDismiss = { showUpdateScopeDialog = false },
            onConfirm = { forumChanges, categoryChanges ->
                val newFilters = updateSuccess.filters.map { filter ->
                    forumChanges[filter.fid]?.let { filter.copy(enabled = it) } ?: filter
                }
                val newCategoryFilters = updateSuccess.categoryFilters.map { filter ->
                    categoryChanges[filter.categoryId]?.let { filter.copy(enabled = it) } ?: filter
                }
                state = updateSuccess.copy(
                    filters = newFilters,
                    categoryFilters = newCategoryFilters,
                )
                scope.launch {
                    forumChanges.forEach { (fid, enabled) ->
                        favoriteUpdateRepository.setFidEnabled(fid, enabled)
                    }
                    categoryChanges.forEach { (categoryId, enabled) ->
                        favoriteUpdateRepository.setCategoryEnabled(categoryId, enabled)
                    }
                    loadUpdates()
                    showUpdateScopeDialog = false
                }
            },
        )
    }
}

@Composable
private fun FavoriteUpdateHeader(
    runState: FavoriteUpdateRepository.RunState,
    onGlobalFavoriteUpdate: () -> Unit,
    onCancelFavoriteUpdate: (String) -> Unit,
    onInterruptFavoriteUpdate: (String) -> Unit,
    onResumeFavoriteUpdate: () -> Unit,
    favoriteUpdateInterval: FavoriteUpdateInterval,
    onFavoriteUpdateIntervalChange: (FavoriteUpdateInterval) -> Unit,
    favoriteUpdateHiddenRunId: String,
    onHideFavoriteUpdateStatus: (String) -> Unit,
    resultFilterActive: Boolean,
    resultFilterLabel: String,
    onShowResultFilter: () -> Unit,
) {
    val colors = YamiboTheme.colors
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
            Text(i18n("收藏更新"), color = colors.textStrong, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                YamiboActionChip(
                    i18n("結果: {}", resultFilterLabel),
                    onClick = onShowResultFilter,
                    selected = resultFilterActive,
                )
                YamiboActionChip(i18n("刷新週期: {}", favoriteUpdateInterval.localizedLabel()), onClick = { showIntervalDialog = true })
                when {
                    interrupted != null -> YamiboActionChip(i18n("繼續"), onClick = onResumeFavoriteUpdate)
                    running == null -> YamiboActionChip(i18n("全域刷新"), onClick = onGlobalFavoriteUpdate)
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
    YamiboSingleSelectDialog(
        title = i18n("刷新週期"),
        options = FavoriteUpdateInterval.entries,
        selected = selected,
        onDismiss = onDismiss,
        onSelect = onSelect,
        label = { it.localizedLabel() },
        dismissOnSelect = true,
    )
}

@Composable
private fun FavoriteUpdateCard(
    event: FavoriteUpdateRepository.UpdateEvent,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = colors.brownDeep,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
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
                val coverUrl = resolvedContentCoverUrl(event.targetType, event.targetId, event.coverUrl)
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
                    text = event.latestPostTitle?.takeIf { it.isNotBlank() } ?: event.summary,
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
                    text = "${event.summary} / ${formatRelativeTime(event.detectedAt)}",
                    color = colors.textDark.copy(alpha = 0.48f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private inline fun <T> List<T>.isUpdateScopeFilterRestricted(isEnabled: (T) -> Boolean): Boolean {
    if (isEmpty()) return false
    val enabledCount = count(isEnabled)
    return enabledCount in 1 until size
}

@Composable
private fun FavoriteUpdateScopeDialog(
    forumFilters: List<FavoriteUpdateRepository.FidFilter>,
    categoryFilters: List<FavoriteUpdateRepository.CategoryFilter>,
    scopeTargets: List<FavoriteUpdateRepository.ScopeTarget>,
    onDismiss: () -> Unit,
    onConfirm: (Map<Int, Boolean>, Map<Long, Boolean>) -> Unit,
) {
    val colors = YamiboTheme.colors
    var selectedTab by remember { mutableStateOf(UpdateScopeTab.Forum) }
    var draftForumIds by remember(forumFilters) {
        mutableStateOf(forumFilters.filter { it.enabled }.map { it.fid }.toSet())
    }
    var draftCategoryIds by remember(categoryFilters) {
        mutableStateOf(categoryFilters.filter { it.enabled }.map { it.categoryId }.toSet())
    }
    val forumAll = draftForumIds.isEmpty() || draftForumIds.size == forumFilters.size
    val categoryAll = draftCategoryIds.isEmpty() || draftCategoryIds.size == categoryFilters.size
    val updateCount = remember(scopeTargets, draftForumIds, draftCategoryIds, forumAll, categoryAll) {
        scopeTargets.count { target ->
            val forumMatches = forumAll || target.fid in draftForumIds
            val categoryMatches = categoryAll || target.categoryIds.any { it in draftCategoryIds }
            forumMatches && categoryMatches
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(i18n("更新範圍"), color = colors.textStrong, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FavoriteUpdateScopeTabButton(
                        text = i18n("版塊"),
                        selected = selectedTab == UpdateScopeTab.Forum,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = UpdateScopeTab.Forum },
                    )
                    FavoriteUpdateScopeTabButton(
                        text = i18n("收藏夾"),
                        selected = selectedTab == UpdateScopeTab.Category,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = UpdateScopeTab.Category },
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (selectedTab) {
                        UpdateScopeTab.Forum -> {
                            FilterSelectionSection(
                                filters = forumFilters,
                                draftSelection = draftForumIds,
                                isAllSelected = forumAll,
                                getId = { it.fid },
                                getLabel = { it.forumName },
                                getItemCount = { it.itemCount },
                                onSelectAll = { draftForumIds = forumFilters.map { it.fid }.toSet() },
                                onToggle = { id ->
                                    draftForumIds = toggleDraftSelection(
                                        id,
                                        draftForumIds,
                                        forumFilters.map { it.fid }.toSet(),
                                    )
                                }
                            )
                        }
                        UpdateScopeTab.Category -> {
                            FilterSelectionSection(
                                filters = categoryFilters,
                                draftSelection = draftCategoryIds,
                                isAllSelected = categoryAll,
                                getId = { it.categoryId },
                                getLabel = { it.categoryName },
                                getItemCount = { it.itemCount },
                                onSelectAll = { draftCategoryIds = categoryFilters.map { it.categoryId }.toSet() },
                                onToggle = { id ->
                                    draftCategoryIds = toggleDraftSelection(
                                        id,
                                        draftCategoryIds,
                                        categoryFilters.map { it.categoryId }.toSet(),
                                    )
                                }
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.brownPrimary.copy(alpha = 0.08f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            i18n(
                                "版塊：{}",
                                summarizeScopeSelection(
                                    allSelected = forumAll,
                                    selectedLabels = forumFilters.filter { it.fid in draftForumIds }.map { it.forumName },
                                )
                            ),
                            color = colors.textDark,
                            fontSize = 12.sp,
                        )
                        Text(
                            i18n(
                                "收藏夾：{}",
                                summarizeScopeSelection(
                                    allSelected = categoryAll,
                                    selectedLabels = categoryFilters.filter { it.categoryId in draftCategoryIds }.map { it.categoryName },
                                )
                            ),
                            color = colors.textDark,
                            fontSize = 12.sp,
                        )
                        Text(
                            i18n("目前範圍會檢查 {} 個收藏", updateCount),
                            color = colors.textStrong,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YamiboActionChip(i18n("取消"), onClick = onDismiss)
                YamiboActionChip(
                    i18n("套用"),
                    onClick = {
                        val forumChanges = forumFilters.mapNotNull { filter ->
                            val enabled = forumAll || filter.fid in draftForumIds
                            if (enabled != filter.enabled) filter.fid to enabled else null
                        }.toMap()
                        val categoryChanges = categoryFilters.mapNotNull { filter ->
                            val enabled = categoryAll || filter.categoryId in draftCategoryIds
                            if (enabled != filter.enabled) filter.categoryId to enabled else null
                        }.toMap()
                        onConfirm(forumChanges, categoryChanges)
                    },
                )
            }
        },
        dismissButton = {},
        containerColor = colors.creamSurface,
        titleContentColor = colors.textStrong,
        textContentColor = colors.textDark,
    )
}

@Composable
private fun FavoriteUpdateScopeTabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.10f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 10.dp),
            color = if (selected) colors.textOnDeepHigh else colors.textOnTint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

private enum class UpdateScopeTab {
    Forum,
    Category,
}

private fun <T> toggleDraftSelection(value: T, selected: Set<T>, allValues: Set<T>): Set<T> {
    val normalized = if (selected.isEmpty() || selected.size == allValues.size) emptySet() else selected
    val updated = if (value in normalized) normalized - value else normalized + value
    return if (updated.isEmpty() || updated.size == allValues.size) allValues else updated
}

private fun summarizeScopeSelection(allSelected: Boolean, selectedLabels: List<String>): String {
    return when {
        allSelected -> i18n("全部")
        selectedLabels.isEmpty() -> i18n("全部")
        selectedLabels.size <= 3 -> selectedLabels.joinToString("、")
        else -> i18n("{} 項", selectedLabels.size)
    }
}

private const val UPDATE_RESULT_FILTER_ALL = "all"

private data class UpdateResultFilterOption(
    val key: String,
    val label: String,
    val count: Int,
)

@Composable
private fun UpdateResultFilterDialog(
    options: List<UpdateResultFilterOption>,
    selectedKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val allOption = options.firstOrNull { it.key == UPDATE_RESULT_FILTER_ALL }
    val selected = options.filter { it.key in selectedKeys }.toSet().ifEmpty { allOption?.let(::setOf).orEmpty() }
    YamiboMultiSelectDialog(
        title = i18n("篩選更新結果"),
        options = options,
        selected = selected,
        onConfirm = { selectedOptions ->
            onConfirm(selectedOptions.map { it.key }.toSet())
        },
        onCancel = onDismiss,
        label = { "${it.label} (${it.count})" },
        toggleSelection = { option, current ->
            when {
                option.key == UPDATE_RESULT_FILTER_ALL -> setOf(option)
                current.any { it.key == UPDATE_RESULT_FILTER_ALL } -> setOf(option)
                option in current -> current - option
                else -> current + option
            }
        },
    )
}

private fun buildUpdateResultFilterOptions(
    events: List<FavoriteUpdateRepository.UpdateEvent>,
): List<UpdateResultFilterOption> {
    val options = mutableListOf(UpdateResultFilterOption(UPDATE_RESULT_FILTER_ALL, i18n("全部"), events.size))
    val tagCount = events.count { it.targetType == LocalFavoriteRepository.FavoriteTargetType.TagManga }
    if (tagCount > 0) {
        options += UpdateResultFilterOption("tag", i18n("標籤"), tagCount)
    }
    options += events
        .filter { it.targetType != LocalFavoriteRepository.FavoriteTargetType.TagManga }
        .mapNotNull { event ->
            val fid = event.fid ?: return@mapNotNull null
            val label = event.forumName?.takeIf { it.isNotBlank() } ?: i18n("版塊 {}", fid)
            fid to label
        }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<Pair<Int, String>, Int>> { it.value }.thenBy { it.key.second })
        .map { (fidAndLabel, count) ->
            val (fid, label) = fidAndLabel
            UpdateResultFilterOption("fid:$fid", label, count)
        }
    return options
}

private fun normalizeUpdateResultFilterKeys(
    keys: Set<String>,
    options: List<UpdateResultFilterOption>,
): Set<String> {
    val validKeys = options.map { it.key }.toSet()
    val normalized = keys.filterTo(linkedSetOf()) { it in validKeys }
    return if (normalized.isEmpty() || UPDATE_RESULT_FILTER_ALL in normalized || normalized.size == validKeys.size - 1) {
        setOf(UPDATE_RESULT_FILTER_ALL)
    } else {
        normalized
    }
}

private fun filterUpdateEvents(
    events: List<FavoriteUpdateRepository.UpdateEvent>,
    selectedKeys: Set<String>,
    options: List<UpdateResultFilterOption>,
): List<FavoriteUpdateRepository.UpdateEvent> {
    if (!isUpdateResultFilterRestricted(selectedKeys, options)) return events
    return events.filter { event ->
        val key = if (event.targetType == LocalFavoriteRepository.FavoriteTargetType.TagManga) {
            "tag"
        } else {
            event.fid?.let { "fid:$it" }
        }
        key != null && key in selectedKeys
    }
}

private fun isUpdateResultFilterRestricted(
    selectedKeys: Set<String>,
    options: List<UpdateResultFilterOption>,
): Boolean {
    if (options.size <= 1) return false
    return UPDATE_RESULT_FILTER_ALL !in selectedKeys
}

private fun updateResultFilterLabel(
    selectedKeys: Set<String>,
    options: List<UpdateResultFilterOption>,
): String {
    if (!isUpdateResultFilterRestricted(selectedKeys, options)) return i18n("全部")
    val selectedLabels = options.filter { it.key in selectedKeys }.map { it.label }
    return when {
        selectedLabels.isEmpty() -> i18n("全部")
        selectedLabels.size == 1 -> selectedLabels.first()
        selectedLabels.size <= 3 -> selectedLabels.joinToString("、")
        else -> i18n("{} 項", selectedLabels.size)
    }
}

private fun FavoriteUpdateRepository.RunState.refreshKey(): String = when (this) {
    FavoriteUpdateRepository.RunState.Idle -> "idle"
    is FavoriteUpdateRepository.RunState.Running -> "running:${snapshot.detectedCount}"
    is FavoriteUpdateRepository.RunState.Interrupted -> "interrupted:${snapshot.detectedCount}"
    is FavoriteUpdateRepository.RunState.Failed -> "failed:${snapshot.detectedCount}"
    is FavoriteUpdateRepository.RunState.Completed -> "completed:${snapshot.detectedCount}"
}

private fun navigateFavoriteUpdateEvent(
    event: FavoriteUpdateRepository.UpdateEvent,
    navigator: ComposableNavigator,
) {
    when (event.targetType) {
        LocalFavoriteRepository.FavoriteTargetType.TagManga -> navigator.navigate(
            ITagDetailScreen(
                tagId = io.github.littlesurvival.dto.value.TagId(event.targetId.toInt()),
                title = event.title,
            )
        )
        LocalFavoriteRepository.FavoriteTargetType.ThreadNovel -> navigator.navigate(
            INovelThreadDetailScreen(
                tid = io.github.littlesurvival.dto.value.ThreadId(event.targetId.toInt()),
                title = event.title,
                authorId = event.authorId?.toInt()?.let { io.github.littlesurvival.dto.value.UserId(it) },
            )
        )
        LocalFavoriteRepository.FavoriteTargetType.ThreadNormal -> navigator.navigate(
            IThreadReaderScreen(
                tid = io.github.littlesurvival.dto.value.ThreadId(event.targetId.toInt()),
                title = event.title,
                threadType = ReadHistoryRepository.ThreadEntryType.Normal,
            )
        )
    }
}

@Composable
private fun UpdatesSelectTopBar(
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onCancel: () -> Unit,
    onDeleteSelected: () -> Unit,
    selectedCount: Int,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = i18n("已選 {} 項", selectedCount),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textStrong,
            modifier = Modifier.weight(1f),
        )
        Surface(onClick = onSelectAll, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text(i18n("全選"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textStrong)
        }
        if (selectedCount > 0) {
            Surface(onClick = onDeleteSelected, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.15f)) {
                Text(i18n("刪除"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
            }
        }
        Surface(onClick = onClearAll, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.1f)) {
            Text(i18n("清空"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
        }
        Surface(onClick = onCancel, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text(i18n("取消"), modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textStrong)
        }
    }
}

private fun <ID, T> LazyListScope.FilterSelectionSection(
    filters: List<T>,
    draftSelection: Set<ID>,
    isAllSelected: Boolean,
    getId: (T) -> ID,
    getLabel: (T) -> String,
    getItemCount: (T) -> Int,
    onSelectAll: () -> Unit,
    onToggle: (ID) -> Unit,
) {
    item {
        YamiboSingleSelectRow(
            label = i18n("全部 ({})", filters.sumOf(getItemCount)),
            selected = isAllSelected,
            selectedText = i18n("已選擇"),
            onClick = onSelectAll,
        )
    }
    items(filters, key = { getId(it) as Any }) { filter ->
        val id = getId(filter)
        val selected = !isAllSelected && id in draftSelection
        YamiboSingleSelectRow(
            label = "${getLabel(filter)} (${getItemCount(filter)})",
            selected = selected,
            selectedText = i18n("已選擇"),
            onClick = { onToggle(id) },
        )
    }
}
