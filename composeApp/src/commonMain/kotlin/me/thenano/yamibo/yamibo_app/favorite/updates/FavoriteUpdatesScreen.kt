package me.thenano.yamibo.yamibo_app.favorite.updates

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.controls.YamiboMultiSelectDialog
import me.thenano.yamibo.yamibo_app.components.controls.YamiboSingleSelectRow
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboLoadingContent
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboMainTabIconAction
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboMainTabTopBar
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.state

private sealed interface FavoriteUpdatesState {
    data object Loading : FavoriteUpdatesState
    data class Success(
        val events: List<FavoriteUpdateRepository.UpdateEvent>,
        val forumFilters: List<FavoriteUpdateRepository.FidFilter>,
        val categoryFilters: List<FavoriteUpdateRepository.CategoryFilter>,
        val scopeTargets: List<FavoriteUpdateRepository.ScopeTarget>,
    ) : FavoriteUpdatesState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteUpdatesScreen() {
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

    var state by remember { mutableStateOf<FavoriteUpdatesState>(FavoriteUpdatesState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showGlobalRefreshConfirm by remember { mutableStateOf(false) }
    var showUpdateScopeDialog by remember { mutableStateOf(false) }
    var showResultFilterDialog by remember { mutableStateOf(false) }
    var selectedResultFilterKeys by remember { mutableStateOf(setOf(UPDATE_RESULT_FILTER_ALL)) }

    suspend fun loadUpdates() {
        state = FavoriteUpdatesState.Success(
            events = favoriteUpdateRepository.getActiveEvents(),
            forumFilters = favoriteUpdateRepository.getFidFilters(),
            categoryFilters = favoriteUpdateRepository.getCategoryFilters(),
            scopeTargets = favoriteUpdateRepository.getScopeTargets(),
        )
    }

    LaunchedEffect(Unit) {
        loadUpdates()
    }

    LaunchedEffect(favoriteUpdateRefreshKey) {
        if (state is FavoriteUpdatesState.Success) {
            loadUpdates()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = { YamiboSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val success = state as? FavoriteUpdatesState.Success
            YamiboMainTabTopBar(title = i18n("更新")) {
                YamiboMainTabIconAction(
                    icon = YamiboIcons.FilterList,
                    contentDescription = i18n("篩選更新對象"),
                    onClick = { if (success != null) showUpdateScopeDialog = true },
                    iconSize = 26,
                    tint = if (
                        success?.forumFilters.isFilterRestricted { it.enabled } ||
                        success?.categoryFilters.isFilterRestricted { it.enabled }
                    ) {
                        colors.orangeAccent
                    } else {
                        colors.brownDeep
                    },
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(colors.creamBackground),
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "favorite_updates_state",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
                    FavoriteUpdatesState.Loading -> YamiboLoadingContent()
                    is FavoriteUpdatesState.Success -> PullToRefreshBox(
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
                        val normalizedResultFilterKeys = normalizeUpdateResultFilterKeys(
                            selectedResultFilterKeys,
                            resultFilterOptions,
                        )
                        val filteredEvents = remember(current.events, normalizedResultFilterKeys, resultFilterOptions) {
                            filterUpdateEvents(current.events, normalizedResultFilterKeys, resultFilterOptions)
                        }
                        FavoriteUpdatesContent(
                            events = filteredEvents,
                            runState = favoriteUpdateRunState,
                            onUpdateEventClick = { event ->
                                scope.launch { favoriteUpdateRepository.markEventRead(event.id) }
                                navigateFavoriteUpdateEvent(event, navigator)
                            },
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
        )
    }

    val success = state as? FavoriteUpdatesState.Success
    if (showResultFilterDialog && success != null) {
        val options = buildUpdateResultFilterOptions(success.events)
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

    if (showUpdateScopeDialog && success != null) {
        FavoriteUpdateScopeDialog(
            forumFilters = success.forumFilters,
            categoryFilters = success.categoryFilters,
            scopeTargets = success.scopeTargets,
            onDismiss = { showUpdateScopeDialog = false },
            onConfirm = { forumChanges, categoryChanges ->
                state = state
                    .withUpdatedForumFilters(forumChanges)
                    .withUpdatedCategoryFilters(categoryChanges)
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
            if (option.key == UPDATE_RESULT_FILTER_ALL) {
                setOf(option)
            } else {
                val withoutAll = current.filterTo(linkedSetOf()) { it.key != UPDATE_RESULT_FILTER_ALL }
                val next = if (option in withoutAll) withoutAll - option else withoutAll + option
                next.ifEmpty { allOption?.let(::setOf).orEmpty() }
            }
        },
    )
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
        title = { Text(i18n("更新版塊/類別"), color = colors.textStrong, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FavoriteUpdateScopeTabButton(
                        text = i18n("版塊篩選"),
                        selected = selectedTab == UpdateScopeTab.Forum,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = UpdateScopeTab.Forum },
                    )
                    FavoriteUpdateScopeTabButton(
                        text = i18n("收藏夾篩選"),
                        selected = selectedTab == UpdateScopeTab.Category,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = UpdateScopeTab.Category },
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 360.dp)
                        .background(colors.creamBackground, RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (selectedTab) {
                        UpdateScopeTab.Forum -> {
                            item {
                                YamiboSingleSelectRow(
                                    label = i18n("全部 ({})", forumFilters.sumOf { it.itemCount }),
                                    selected = forumAll,
                                    selectedText = i18n("已選擇"),
                                    onClick = { draftForumIds = forumFilters.map { it.fid }.toSet() },
                                )
                            }
                            items(forumFilters, key = { it.fid }) { filter ->
                                val selected = !forumAll && filter.fid in draftForumIds
                                YamiboSingleSelectRow(
                                    label = "${filter.forumName} (${filter.itemCount})",
                                    selected = selected,
                                    selectedText = i18n("已選擇"),
                                    onClick = {
                                        draftForumIds = toggleDraftSelection(filter.fid, draftForumIds, forumFilters.map { it.fid }.toSet())
                                    },
                                )
                            }
                        }
                        UpdateScopeTab.Category -> {
                            item {
                                YamiboSingleSelectRow(
                                    label = i18n("全部 ({})", categoryFilters.sumOf { it.itemCount }),
                                    selected = categoryAll,
                                    selectedText = i18n("已選擇"),
                                    onClick = { draftCategoryIds = categoryFilters.map { it.categoryId }.toSet() },
                                )
                            }
                            items(categoryFilters, key = { it.categoryId }) { filter ->
                                val selected = !categoryAll && filter.categoryId in draftCategoryIds
                                YamiboSingleSelectRow(
                                    label = "${filter.categoryName} (${filter.itemCount})",
                                    selected = selected,
                                    selectedText = i18n("已選擇"),
                                    onClick = {
                                        draftCategoryIds = toggleDraftSelection(
                                            filter.categoryId,
                                            draftCategoryIds,
                                            categoryFilters.map { it.categoryId }.toSet(),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                FavoriteUpdateScopeSummary(
                    forumSummary = summarizeScopeSelection(
                        allSelected = forumAll,
                        selectedLabels = forumFilters.filter { it.fid in draftForumIds }.map { it.forumName },
                    ),
                    categorySummary = summarizeScopeSelection(
                        allSelected = categoryAll,
                        selectedLabels = categoryFilters.filter { it.categoryId in draftCategoryIds }.map { it.categoryName },
                    ),
                    updateCount = updateCount,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YamiboActionChip(i18n("取消"), onClick = onDismiss)
                YamiboActionChip(
                    i18n("確認"),
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
        shape = RoundedCornerShape(0.dp),
        color = if (selected) colors.brownPrimary.copy(alpha = 0.18f) else colors.brownPrimary.copy(alpha = 0.08f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                color = colors.textStrong,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(3.dp)
                    .background(
                        if (selected) colors.brownDeep else Color.Transparent,
                        RoundedCornerShape(999.dp),
                    ),
            )
        }
    }
}

@Composable
private fun FavoriteUpdateScopeSummary(
    forumSummary: String,
    categorySummary: String,
    updateCount: Int,
) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.brownPrimary.copy(alpha = 0.08f),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(i18n("版塊：{}", forumSummary), color = colors.textDark, fontSize = 12.sp)
            Text(i18n("收藏夾：{}", categorySummary), color = colors.textDark, fontSize = 12.sp)
            Text(
                i18n("目前篩選下會更新 {} 個收藏", updateCount),
                color = colors.textStrong,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private enum class UpdateScopeTab {
    Forum,
    Category,
}

private fun <T> toggleDraftSelection(value: T, selected: Set<T>, allValues: Set<T>): Set<T> {
    val normalized = if (selected.isEmpty() || selected.size == allValues.size) emptySet() else selected
    val next = if (value in normalized) normalized - value else normalized + value
    return next.ifEmpty { allValues }
}

private fun summarizeScopeSelection(allSelected: Boolean, selectedLabels: List<String>): String {
    return when {
        allSelected -> i18n("全部")
        selectedLabels.isEmpty() -> i18n("全部")
        selectedLabels.size <= 3 -> selectedLabels.joinToString("、")
        else -> i18n("{} 等 {} 項", selectedLabels.take(3).joinToString("、"), selectedLabels.size)
    }
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
        else -> i18n("{} 等 {} 項", selectedLabels.take(3).joinToString("、"), selectedLabels.size)
    }
}

private fun FavoriteUpdatesState.withUpdatedForumFilters(
    changes: Map<Int, Boolean>,
): FavoriteUpdatesState {
    val success = this as? FavoriteUpdatesState.Success ?: return this
    return success.copy(
        forumFilters = success.forumFilters.map { filter ->
            changes[filter.fid]?.let { filter.copy(enabled = it) } ?: filter
        },
    )
}

private fun FavoriteUpdatesState.withUpdatedCategoryFilters(
    changes: Map<Long, Boolean>,
): FavoriteUpdatesState {
    val success = this as? FavoriteUpdatesState.Success ?: return this
    return success.copy(
        categoryFilters = success.categoryFilters.map { filter ->
            changes[filter.categoryId]?.let { filter.copy(enabled = it) } ?: filter
        },
    )
}

private fun <T> List<T>?.isFilterRestricted(isEnabled: (T) -> Boolean): Boolean {
    if (this.isNullOrEmpty()) return false
    val enabledCount = count(isEnabled)
    return enabledCount > 0 && enabledCount < size
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
                tagId = TagId(event.targetId.toInt()),
                title = event.title,
            )
        )
        LocalFavoriteRepository.FavoriteTargetType.ThreadNovel -> navigator.navigate(
            INovelThreadDetailScreen(
                tid = ThreadId(event.targetId.toInt()),
                title = event.title,
                authorId = event.authorId?.toInt()?.let { UserId(it) },
            )
        )
        LocalFavoriteRepository.FavoriteTargetType.ThreadNormal -> navigator.navigate(
            IThreadReaderScreen(
                tid = ThreadId(event.targetId.toInt()),
                title = event.title,
                threadType = ReadHistoryRepository.ThreadEntryType.Normal,
            )
        )
    }
}
