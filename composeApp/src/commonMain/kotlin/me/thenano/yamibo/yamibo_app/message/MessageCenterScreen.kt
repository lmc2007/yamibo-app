package me.thenano.yamibo.yamibo_app.message

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.*
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.controls.YamiboMultiSelectDialog
import me.thenano.yamibo.yamibo_app.components.controls.YamiboSingleSelectRow
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboErrorContent
import me.thenano.yamibo.yamibo_app.components.feedback.YamiboLoadingContent
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboMainTabTopBar
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboMainTabIconAction
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBar
import me.thenano.yamibo.yamibo_app.components.navigation.YamiboTopBarIconAction
import me.thenano.yamibo.yamibo_app.components.user.UserAvatar
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.components.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.userspace.IUserSpaceScreen
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView

enum class MessageCenterTab(val title: String) {
    Updates(i18n("更新")),
    PrivateMessages(i18n("我的消息")),
    Notices(i18n("我的提醒")),
}

private sealed interface MessageCenterState {
    data object Loading : MessageCenterState
    data class Success(val content: MessageCenterContent) : MessageCenterState
    data class Error(val message: String) : MessageCenterState
}

internal sealed interface MessageCenterContent {
    data class Updates(
        val events: List<FavoriteUpdateRepository.UpdateEvent>,
        val filters: List<FavoriteUpdateRepository.FidFilter>,
        val categoryFilters: List<FavoriteUpdateRepository.CategoryFilter>,
        val scopeTargets: List<FavoriteUpdateRepository.ScopeTarget>,
        val runState: FavoriteUpdateRepository.RunState,
    ) : MessageCenterContent
    data class PrivateMessages(val page: UserSpacePrivateMessagePage) : MessageCenterContent
    data class Notices(val page: UserSpaceNoticePage) : MessageCenterContent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageCenterScreen(
    initialTab: MessageCenterTab = MessageCenterTab.PrivateMessages,
    mainTabTopBar: Boolean = false,
    updatesOnly: Boolean = false,
    onPrivateMessageUnreadChange: (Boolean) -> Unit = {},
) {
    val colors = YamiboTheme.colors
    val userSpaceRepository = LocalUserSpaceRepository.current
    val favoriteUpdateRepository = LocalFavoriteUpdateRepository.current
    val favoriteUpdateRunner = LocalFavoriteUpdateRunner.current
    val favoriteUpdateRunState by favoriteUpdateRunner.state.collectAsState()
    val favoriteUpdateRefreshKey = favoriteUpdateRunState.refreshKey()
    val appSettingsRepository = LocalAppSettingsRepository.current
    val favoriteUpdateInterval = appSettingsRepository.favoriteUpdateInterval.state()
    val favoriteUpdateHiddenRunId = appSettingsRepository.favoriteUpdateHiddenRunId.state()
    val authRepository = LocalAuthRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser = authRepository.currentUser()

    var selectedTab by remember { mutableStateOf(initialTab) }
    var currentPage by remember { mutableIntStateOf(1) }
    var state by remember { mutableStateOf<MessageCenterState>(MessageCenterState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedEventIds by remember { mutableStateOf(setOf<Long>()) }
    var showGlobalRefreshConfirm by remember { mutableStateOf(false) }
    var showUpdateScopeDialog by remember { mutableStateOf(false) }
    var showResultFilterDialog by remember { mutableStateOf(false) }
    var selectedResultFilterKeys by remember { mutableStateOf(setOf(UPDATE_RESULT_FILTER_ALL)) }
    val updateContent = (state as? MessageCenterState.Success)?.content as? MessageCenterContent.Updates
    val updateScopeFilterActive = updateContent?.let {
        it.filters.isUpdateScopeFilterRestricted { filter -> filter.enabled } ||
            it.categoryFilters.isUpdateScopeFilterRestricted { filter -> filter.enabled }
    } == true

    suspend fun loadTab(tab: MessageCenterTab, page: Int, preferCache: Boolean = true) {
        if (tab == MessageCenterTab.Updates) {
            val updates = withContext(Dispatchers.Default) {
                MessageCenterContent.Updates(
                    events = favoriteUpdateRepository.getActiveEvents(),
                    filters = favoriteUpdateRepository.getFidFilters(),
                    categoryFilters = favoriteUpdateRepository.getCategoryFilters(),
                    scopeTargets = favoriteUpdateRepository.getScopeTargets(),
                    runState = favoriteUpdateRunState,
                )
            }
            if (tab != selectedTab) return
            currentPage = 1
            state = MessageCenterState.Success(updates)
            return
        }
        if (preferCache) {
            cachedContent(userSpaceRepository, tab, page)?.let {
                if (tab != selectedTab) return
                currentPage = page
                state = MessageCenterState.Success(it)
                if (it is MessageCenterContent.PrivateMessages) {
                    onPrivateMessageUnreadChange(it.page.hasUnreadMessages())
                }
                return
            }
        }
        val result = fetchContent(userSpaceRepository, tab, page)
        if (tab != selectedTab) return
        state = when (result) {
            is YamiboResult.Success -> {
                val content = result.value
                currentPage = content.pageNumber() ?: page
                if (content is MessageCenterContent.PrivateMessages) {
                    onPrivateMessageUnreadChange(content.page.hasUnreadMessages())
                }
                MessageCenterState.Success(content)
            }
            else -> MessageCenterState.Error(i18n(result.message()))
        }
    }

    LaunchedEffect(selectedTab) {
        currentPage = 1
        loadTab(selectedTab, 1)
    }

    LaunchedEffect(favoriteUpdateRefreshKey) {
        if (selectedTab == MessageCenterTab.Updates) {
            loadTab(MessageCenterTab.Updates, 1, preferCache = false)
        }
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
            if (updatesOnly && isSelectMode) {
                UpdatesSelectTopBar(
                    onSelectAll = {
                        val updates = (state as? MessageCenterState.Success)?.content as? MessageCenterContent.Updates
                        if (updates != null) {
                            selectedEventIds = updates.events.map { it.id }.toSet()
                        }
                    },
                    onClearAll = {
                        scope.launch {
                            favoriteUpdateRepository.dismissAllEvents()
                            isSelectMode = false
                            selectedEventIds = emptySet()
                            loadTab(MessageCenterTab.Updates, 1, preferCache = false)
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
                                loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                snackbarHostState.showSnackbar(i18n("已刪除 {} 項紀錄", deletedCount))
                            }
                        }
                    },
                    selectedCount = selectedEventIds.size
                )
            } else if (mainTabTopBar) {
                MessageCenterMainTopBar(
                    title = if (updatesOnly) i18n("更新") else i18n("我的消息"),
                    profile = currentUser,
                    showProfileShortcut = !updatesOnly,
                    trailingContent = {
                        if (updatesOnly) {
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
                    },
                    onSpaceClick = {
                        navigator.navigate(IUserSpaceScreen(currentUser?.uid, currentUser?.username))
                    },
                )
            } else {
                MessageCenterTopBar(
                    title = selectedTab.title,
                    showEdit = selectedTab == MessageCenterTab.PrivateMessages,
                    onBack = { navigator.pop() },
                    onEdit = { navigator.navigate(sendPrivateMessageWebView()) },
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
            if (!updatesOnly) {
                MessageCenterTabRow(
                    selectedTab = selectedTab,
                    includeUpdates = false,
                    onSelect = { tab ->
                        if (tab != selectedTab) {
                            selectedTab = tab
                            currentPage = 1
                            isRefreshing = false
                            state = MessageCenterState.Loading
                        }
                    },
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (val current = state) {
                    MessageCenterState.Loading -> YamiboLoadingContent()
                    is MessageCenterState.Error -> YamiboErrorContent(
                        message = current.message,
                        onRetry = {
                            state = MessageCenterState.Loading
                            scope.launch { loadTab(selectedTab, currentPage, preferCache = false) }
                        },
                    )
                    is MessageCenterState.Success -> PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            scope.launch {
                                loadTab(selectedTab, currentPage, preferCache = false)
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val updateContent = (current.content as? MessageCenterContent.Updates)
                        val resultFilterOptions = remember(updateContent?.events) {
                            buildUpdateResultFilterOptions(updateContent?.events.orEmpty())
                        }
                        val normalizedResultFilterKeys = remember(selectedResultFilterKeys, resultFilterOptions) {
                            normalizeUpdateResultFilterKeys(selectedResultFilterKeys, resultFilterOptions)
                        }
                        val filteredUpdateEvents = remember(updateContent?.events, normalizedResultFilterKeys, resultFilterOptions) {
                            filterUpdateEvents(updateContent?.events.orEmpty(), normalizedResultFilterKeys, resultFilterOptions)
                        }
                        MessageCenterMainContent(
                            content = when (val content = current.content) {
                                is MessageCenterContent.Updates -> content.copy(
                                    events = filteredUpdateEvents,
                                    runState = favoriteUpdateRunState,
                                )
                                else -> content
                            },
                            selectedTab = selectedTab,
                            currentPage = currentPage,
                            onPageChange = { page ->
                                state = MessageCenterState.Loading
                                scope.launch { loadTab(selectedTab, page) }
                            },
                            onUpdateEventClick = { event ->
                                scope.launch { favoriteUpdateRepository.markEventRead(event.id) }
                                navigateFavoriteUpdateEvent(event, navigator)
                            },
                            onGlobalFavoriteUpdate = { showGlobalRefreshConfirm = true },
                            onCancelFavoriteUpdate = { runId ->
                                scope.launch {
                                    favoriteUpdateRunner.cancelUpdate(runId)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                    snackbarHostState.showSnackbar(i18n("已取消收藏更新檢查"), duration = SnackbarDuration.Short)
                                }
                            },
                            onInterruptFavoriteUpdate = { runId ->
                                scope.launch {
                                    favoriteUpdateRunner.interruptUpdate(runId)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                    snackbarHostState.showSnackbar(i18n("已中斷收藏更新檢查"), duration = SnackbarDuration.Short)
                                }
                            },
                            onResumeFavoriteUpdate = {
                                scope.launch {
                                    when (val result = favoriteUpdateRunner.resumeInterruptedUpdate()) {
                                        is FavoriteUpdateRunner.LaunchResult.Started -> {
                                            appSettingsRepository.favoriteUpdateHiddenRunId.setValue("")
                                            loadTab(MessageCenterTab.Updates, 1, preferCache = false)
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
                            onToggleFavoriteUpdateFid = { fid, enabled ->
                                state = state.withUpdatedFavoriteUpdateFilter(fid, enabled)
                                scope.launch {
                                    favoriteUpdateRepository.setFidEnabled(fid, enabled)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                }
                            },
                            onToggleFavoriteUpdateCategory = { categoryId, enabled ->
                                state = state.withUpdatedFavoriteUpdateCategoryFilter(categoryId, enabled)
                                scope.launch {
                                    favoriteUpdateRepository.setCategoryEnabled(categoryId, enabled)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                }
                            },
                            resultFilterActive = isUpdateResultFilterRestricted(normalizedResultFilterKeys, resultFilterOptions),
                            resultFilterLabel = updateResultFilterLabel(normalizedResultFilterKeys, resultFilterOptions),
                            onShowResultFilter = { showResultFilterDialog = true },
                            onShowUpdateScopeFilter = { showUpdateScopeDialog = true },
                            onUserClick = { user -> navigator.navigate(IUserSpaceScreen(user.uid, user.name)) },
                            onNoticeUserClick = { userId -> navigator.navigate(IUserSpaceScreen(userId)) },
                            onOpenPrivateMessage = { user -> navigator.navigate(IPrivateMessageScreen(user.uid, user.name)) },
                            onMessageAction = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(i18n("TODO: 消息互動尚未接入"), duration = SnackbarDuration.Short)
                                }
                            },
                            isSelectMode = isSelectMode,
                            selectedEventIds = selectedEventIds,
                            onToggleEvent = { id ->
                                selectedEventIds = if (id in selectedEventIds) {
                                    selectedEventIds - id
                                } else {
                                    selectedEventIds + id
                                }
                            },
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
            titleContentColor = colors.textStrong,
            textContentColor = colors.textDark,
        )
    }

    val updateSuccess = (state as? MessageCenterState.Success)?.content as? MessageCenterContent.Updates
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
                state = state
                    .withUpdatedFavoriteUpdateFilters(forumChanges)
                    .withUpdatedFavoriteUpdateCategoryFilters(categoryChanges)
                scope.launch {
                    forumChanges.forEach { (fid, enabled) ->
                        favoriteUpdateRepository.setFidEnabled(fid, enabled)
                    }
                    categoryChanges.forEach { (categoryId, enabled) ->
                        favoriteUpdateRepository.setCategoryEnabled(categoryId, enabled)
                    }
                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                    showUpdateScopeDialog = false
                }
            },
        )
    }
}

@Composable
private fun MessageCenterMainTopBar(
    title: String,
    profile: ProfilePage?,
    showProfileShortcut: Boolean,
    trailingContent: @Composable RowScope.() -> Unit = {},
    onSpaceClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    YamiboMainTabTopBar(title = title) {
        if (showProfileShortcut) {
            Surface(onClick = onSpaceClick, shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UserAvatar(profile?.avatarUrl, size = 28)
                    Text(
                        text = i18n("我的空間"),
                        color = colors.brownDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        trailingContent()
    }
}

private inline fun <T> List<T>.isUpdateScopeFilterRestricted(isEnabled: (T) -> Boolean): Boolean {
    if (isEmpty()) return false
    val enabledCount = count(isEnabled)
    return enabledCount in 1 until size
}

@Composable
private fun MessageCenterTopBar(
    title: String,
    showEdit: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    YamiboTopBar(
        title = title,
        applyStatusPadding = true,
        onBack = onBack,
    ) {
        if (showEdit) {
            YamiboTopBarIconAction(YamiboIcons.EditOrSign, i18n("編輯"), onEdit)
        }
    }
}

@Composable
private fun MessageCenterTabRow(
    selectedTab: MessageCenterTab,
    includeUpdates: Boolean,
    onSelect: (MessageCenterTab) -> Unit,
) {
    val colors = YamiboTheme.colors
    val tabs = remember(includeUpdates) {
        MessageCenterTab.entries.filter { includeUpdates || it != MessageCenterTab.Updates }
    }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    ScrollableYamiboTabRow(selectedIndex = selectedIndex) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = tab.title,
                        color = colors.textOnSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun ScrollableYamiboTabRow(
    selectedIndex: Int,
    content: @Composable () -> Unit,
) {
    val colors = YamiboTheme.colors
    @Suppress("DEPRECATION")
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = colors.creamSurface,
        contentColor = colors.textStrong,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = colors.textStrong,
                    height = 2.dp,
                )
            }
        },
        divider = {
            HorizontalDivider(color = colors.brownLight.copy(alpha = 0.45f))
        },
    ) {
        content()
    }
}

private suspend fun fetchContent(
    repository: me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository,
    tab: MessageCenterTab,
    page: Int,
): YamiboResult<MessageCenterContent> = when (tab) {
    MessageCenterTab.Updates -> YamiboResult.Failure(i18n("更新頁不需要網路載入"))
    MessageCenterTab.PrivateMessages -> repository.fetchPrivateMessages(page).mapSuccess { MessageCenterContent.PrivateMessages(it) }
    MessageCenterTab.Notices -> repository.fetchNotices(page).mapSuccess { MessageCenterContent.Notices(it) }
}

private fun cachedContent(
    repository: me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository,
    tab: MessageCenterTab,
    page: Int,
): MessageCenterContent? = when (tab) {
    MessageCenterTab.Updates -> null
    MessageCenterTab.PrivateMessages -> repository.getCachedPrivateMessages(page)?.let { MessageCenterContent.PrivateMessages(it) }
    MessageCenterTab.Notices -> repository.getCachedNotices(page)?.let { MessageCenterContent.Notices(it) }
}

private fun MessageCenterContent.pageNumber(): Int? = when (this) {
    is MessageCenterContent.Updates -> 1
    is MessageCenterContent.PrivateMessages -> page.pageNav?.currentPage
    is MessageCenterContent.Notices -> page.pageNav?.currentPage
}

private fun MessageCenterState.withUpdatedFavoriteUpdateFilter(
    fid: Int,
    enabled: Boolean,
): MessageCenterState {
    val success = this as? MessageCenterState.Success ?: return this
    val updates = success.content as? MessageCenterContent.Updates ?: return this
    return success.copy(
        content = updates.copy(
            filters = updates.filters.map { filter ->
                if (filter.fid == fid) filter.copy(enabled = enabled) else filter
            },
        ),
    )
}

private fun MessageCenterState.withUpdatedFavoriteUpdateFilters(
    changes: Map<Int, Boolean>,
): MessageCenterState {
    val success = this as? MessageCenterState.Success ?: return this
    val updates = success.content as? MessageCenterContent.Updates ?: return this
    return success.copy(
        content = updates.copy(
            filters = updates.filters.map { filter ->
                changes[filter.fid]?.let { filter.copy(enabled = it) } ?: filter
            },
        ),
    )
}

private fun MessageCenterState.withUpdatedFavoriteUpdateCategoryFilter(
    categoryId: Long,
    enabled: Boolean,
): MessageCenterState {
    val success = this as? MessageCenterState.Success ?: return this
    val updates = success.content as? MessageCenterContent.Updates ?: return this
    return success.copy(
        content = updates.copy(
            categoryFilters = updates.categoryFilters.map { filter ->
                if (filter.categoryId == categoryId) filter.copy(enabled = enabled) else filter
            },
        ),
    )
}

private fun MessageCenterState.withUpdatedFavoriteUpdateCategoryFilters(
    changes: Map<Long, Boolean>,
): MessageCenterState {
    val success = this as? MessageCenterState.Success ?: return this
    val updates = success.content as? MessageCenterContent.Updates ?: return this
    return success.copy(
        content = updates.copy(
            categoryFilters = updates.categoryFilters.map { filter ->
                changes[filter.categoryId]?.let { filter.copy(enabled = it) } ?: filter
            },
        ),
    )
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
                                        draftForumIds = toggleDraftSelection(
                                            filter.fid,
                                            draftForumIds,
                                            forumFilters.map { it.fid }.toSet(),
                                        )
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

private fun UserSpacePrivateMessagePage.hasUnreadMessages(): Boolean =
    (unreadCount ?: 0) > 0 || messages.any { (it.unreadCount ?: 0) > 0 }

private fun FavoriteUpdateRepository.RunState.refreshKey(): String = when (this) {
    FavoriteUpdateRepository.RunState.Idle -> "idle"
    is FavoriteUpdateRepository.RunState.Running -> "running:${snapshot.detectedCount}"
    is FavoriteUpdateRepository.RunState.Interrupted -> "interrupted:${snapshot.detectedCount}"
    is FavoriteUpdateRepository.RunState.Failed -> "failed:${snapshot.detectedCount}"
    is FavoriteUpdateRepository.RunState.Completed -> "completed:${snapshot.detectedCount}"
}

private fun <T, R> YamiboResult<T>.mapSuccess(transform: (T) -> R): YamiboResult<R> = when (this) {
    is YamiboResult.Success -> YamiboResult.Success(transform(value))
    is YamiboResult.Failure -> this
    is YamiboResult.NotLoggedIn -> this
    is YamiboResult.NoPermission -> this
    is YamiboResult.Maintenance -> this
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

private fun sendPrivateMessageWebView(): IActionWebView =
    IActionWebView(
        title = i18n("發消息"),
        initialUrl = YamiboRoute.SendPrivateMessagePage.build(),
        successCondition = { url -> isMessageListUrl(url) },
    )

private fun isMessageListUrl(url: String): Boolean {
    val target = YamiboRoute.UserSpace.Notification(
        type = YamiboRoute.UserSpace.NotificationType.MyMessage,
        page = 1,
    ).build()
    return url.startsWith(target.substringBefore("&page=")) &&
        url.contains("mod=space") &&
        url.contains("do=pm") &&
        !url.contains("spacecp")
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
