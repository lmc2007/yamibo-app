package me.thenano.yamibo.yamibo_app.message

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedMessage
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.LocalUserSpaceRepository
import me.thenano.yamibo.yamibo_app.components.UserAvatar
import me.thenano.yamibo.yamibo_app.components.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.YamiboErrorContent
import me.thenano.yamibo.yamibo_app.components.YamiboLoadingContent
import me.thenano.yamibo.yamibo_app.components.YamiboMainTabTopBar
import me.thenano.yamibo.yamibo_app.components.YamiboTopBar
import me.thenano.yamibo.yamibo_app.components.YamiboTopBarIconAction
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.userspace.IUserSpaceScreen
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView

enum class MessageCenterTab(val title: String) {
    Updates(appString(Res.string.ui_renew)),
    PrivateMessages(appString(Res.string.ui_my_messages)),
    Notices(appString(Res.string.ui_my_notifications)),
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
    var showGlobalRefreshConfirm by remember { mutableStateOf(false) }

    suspend fun loadTab(tab: MessageCenterTab, page: Int, preferCache: Boolean = true) {
        if (tab == MessageCenterTab.Updates) {
            currentPage = 1
            state = MessageCenterState.Success(
                MessageCenterContent.Updates(
                    events = favoriteUpdateRepository.getActiveEvents(),
                    filters = favoriteUpdateRepository.getFidFilters(),
                    runState = favoriteUpdateRunState,
                )
            )
            return
        }
        if (preferCache) {
            cachedContent(userSpaceRepository, tab, page)?.let {
                currentPage = page
                state = MessageCenterState.Success(it)
                if (it is MessageCenterContent.PrivateMessages) {
                    onPrivateMessageUnreadChange(it.page.hasUnreadMessages())
                }
                return
            }
        }
        val result = fetchContent(userSpaceRepository, tab, page)
        state = when (result) {
            is YamiboResult.Success -> {
                val content = result.value
                currentPage = content.pageNumber() ?: page
                if (content is MessageCenterContent.PrivateMessages) {
                    onPrivateMessageUnreadChange(content.page.hasUnreadMessages())
                }
                MessageCenterState.Success(content)
            }
            else -> MessageCenterState.Error(result.localizedMessage())
        }
    }

    LaunchedEffect(selectedTab) {
        currentPage = 1
        state = MessageCenterState.Loading
        loadTab(selectedTab, 1)
    }

    LaunchedEffect(favoriteUpdateRefreshKey) {
        if (selectedTab == MessageCenterTab.Updates) {
            loadTab(MessageCenterTab.Updates, 1, preferCache = false)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = { YamiboSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (mainTabTopBar) {
                MessageCenterMainTopBar(
                    profile = currentUser,
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
            MessageCenterTabRow(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
            )
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "message_center_state",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
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
                        MessageCenterMainContent(
                            content = when (val content = current.content) {
                                is MessageCenterContent.Updates -> content.copy(runState = favoriteUpdateRunState)
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
                            onDismissUpdateEvent = { event ->
                                scope.launch {
                                    favoriteUpdateRepository.dismissEvent(event.id)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                }
                            },
                            onGlobalFavoriteUpdate = { showGlobalRefreshConfirm = true },
                            onCancelFavoriteUpdate = { runId ->
                                scope.launch {
                                    favoriteUpdateRunner.cancelUpdate(runId)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                    snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_update_check_canceled), duration = SnackbarDuration.Short)
                                }
                            },
                            onInterruptFavoriteUpdate = { runId ->
                                scope.launch {
                                    favoriteUpdateRunner.interruptUpdate(runId)
                                    loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                    snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_update_check_interrupted), duration = SnackbarDuration.Short)
                                }
                            },
                            onResumeFavoriteUpdate = {
                                scope.launch {
                                    when (val result = favoriteUpdateRunner.resumeInterruptedUpdate()) {
                                        is FavoriteUpdateRunner.LaunchResult.Started -> {
                                            appSettingsRepository.favoriteUpdateHiddenRunId.setValue("")
                                            loadTab(MessageCenterTab.Updates, 1, preferCache = false)
                                            snackbarHostState.showSnackbar(appString(Res.string.ui_keep_checking_for_favorite_updates), duration = SnackbarDuration.Short)
                                        }
                                        is FavoriteUpdateRunner.LaunchResult.Rejected -> {
                                            snackbarHostState.showSnackbar(result.reason, duration = SnackbarDuration.Short)
                                        }
                                        null -> {
                                            snackbarHostState.showSnackbar(appString(Res.string.ui_there_no_favorite_update_task_continue), duration = SnackbarDuration.Short)
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
                                        appString(Res.string.message_update_interval_changed, interval.localizedLabel()),
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                            favoriteUpdateHiddenRunId = favoriteUpdateHiddenRunId,
                            onHideFavoriteUpdateStatus = { runId ->
                                appSettingsRepository.favoriteUpdateHiddenRunId.setValue(runId)
                            },
                            onToggleFavoriteUpdateFid = { fid, enabled ->
                                scope.launch { favoriteUpdateRepository.setFidEnabled(fid, enabled) }
                            },
                            onUserClick = { user -> navigator.navigate(IUserSpaceScreen(user.uid, user.name)) },
                            onOpenPrivateMessage = { user -> navigator.navigate(IPrivateMessageScreen(user.uid, user.name)) },
                            onMessageAction = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(appString(Res.string.ui_todo_message_interaction_has_not_connected_yet), duration = SnackbarDuration.Short)
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
            title = { Text(appString(Res.string.ui_global_refresh_favorite_update)) },
            text = { Text(appString(Res.string.ui_all_favorites_rechecked_new_update_tasks_created_large_number_error)) },
            confirmButton = {
                YamiboActionChip(text = appString(Res.string.ui_start_refreshing), onClick = {
                    showGlobalRefreshConfirm = false
                    scope.launch {
                        when (val result = favoriteUpdateRunner.startGlobalRefresh()) {
                            is FavoriteUpdateRunner.LaunchResult.Started -> {
                                appSettingsRepository.favoriteUpdateHiddenRunId.setValue("")
                                snackbarHostState.showSnackbar(appString(Res.string.ui_start_global_refresh_favorite_updates), duration = SnackbarDuration.Short)
                            }
                            is FavoriteUpdateRunner.LaunchResult.Rejected -> {
                                snackbarHostState.showSnackbar(result.reason, duration = SnackbarDuration.Short)
                            }
                        }
                    }
                })
            },
            dismissButton = { YamiboActionChip(text = appString(Res.string.common_cancel), onClick = { showGlobalRefreshConfirm = false }) },
            containerColor = colors.creamSurface,
        )
    }
}

@Composable
private fun MessageCenterMainTopBar(
    profile: ProfilePage?,
    onSpaceClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    YamiboMainTabTopBar(title = appString(Res.string.ui_my_messages)) {
        Surface(onClick = onSpaceClick, shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
            Row(
                modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UserAvatar(profile?.avatarUrl, size = 28)
                Text(
                    text = appString(Res.string.ui_my_space),
                    color = colors.brownDeep,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
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
            YamiboTopBarIconAction(YamiboIcons.EditOrSign, appString(Res.string.ui_edit), onEdit)
        }
    }
}

@Composable
private fun MessageCenterTabRow(
    selectedTab: MessageCenterTab,
    onSelect: (MessageCenterTab) -> Unit,
) {
    val colors = YamiboTheme.colors
    val tabs = MessageCenterTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    ScrollableYamiboTabRow(selectedIndex = selectedIndex) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = tab.title,
                        color = colors.brownDeep,
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
        contentColor = colors.brownDeep,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = colors.brownDeep,
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
    MessageCenterTab.Updates -> YamiboResult.Failure(appString(Res.string.ui_the_update_page_does_not_require_network_loading))
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
        title = appString(Res.string.ui_send_message),
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




