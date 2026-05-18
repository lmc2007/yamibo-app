package me.thenano.yamibo.yamibo_app.forum

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.FilterType
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.page.OrderType
import io.github.littlesurvival.dto.page.PinnedItem
import io.github.littlesurvival.dto.value.ForumId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.IMainScreen
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalForumRepository
import me.thenano.yamibo.yamibo_app.MainTab
import me.thenano.yamibo.yamibo_app.forum.components.*
import me.thenano.yamibo.yamibo_app.forum.search.ISearchScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.userspace.IUserSpaceScreen
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView

/** Forum page state */
private sealed interface ForumState {
    data object Loading : ForumState
    data class Success(val page: ForumPage) : ForumState
    data class Error(val message: String) : ForumState
}

/** Main Forum Screen Entry */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumPageScreen(fid: ForumId, name: String) {
    val colors = YamiboTheme.colors
    val forumRepository = LocalForumRepository.current
    val authRepository = LocalAuthRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<ForumState>(ForumState.Loading) }
    var currentPage by remember { mutableIntStateOf(1) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedOrderType by remember { mutableStateOf<OrderType?>(null) }
    var selectedFilterType by remember { mutableStateOf<FilterType?>(null) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    suspend fun loadPage(
        page: Int,
        filterType: FilterType? = selectedFilterType,
        orderType: OrderType? = selectedOrderType,
        preferCache: Boolean = true,
    ) {
        val cached = if (preferCache) forumRepository.getCachedForumPage(fid, page, filterType, orderType) else null
        if (cached != null) {
            currentPage = cached.pageNav?.currentPage ?: page
            state = ForumState.Success(cached)
            return
        }

        val result = forumRepository.fetchForum(fid, page, filterType, orderType)
        state =
            when (result) {
                is YamiboResult.Success -> {
                    currentPage = result.value.pageNav?.currentPage ?: page
                    ForumState.Success(result.value)
                }

                else -> ForumState.Error(result.localizedMessage())
            }
    }

    LaunchedEffect(fid) {
        val cached = forumRepository.getCachedForumPage(fid, filterType = selectedFilterType, orderType = selectedOrderType)
        if (cached != null) {
            currentPage = cached.pageNav?.currentPage ?: 1
            state = ForumState.Success(cached)
            return@LaunchedEffect
        }
        loadPage(1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = {
            YamiboSnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            /** Sticky top bar: back + forum name (always visible) */
            val forumName =
                when (val s = state) {
                    is ForumState.Success -> s.page.forum.name
                    else -> name
                }
            ForumTopBar(
                title = forumName,
                onBack = { navigator.pop() },
                onSearch = { navigator.navigate(ISearchScreen(fid)) },
                onPostThread = {
                    navigator.navigate(
                        IActionWebView(
                            title = appString(Res.string.ui_post_2),
                            initialUrl = YamiboRoute.PostThread(fid).build(),
                            successCondition = { url -> url.contains("mod=forumdisplay") && url.contains("fid=") },
                            onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(appString(Res.string.ui_post_successfully))
                                }
                            },
                        )
                    )
                },
                onStarClick = {
                    val formHash = authRepository.currentUser()?.formHash
                    if (formHash == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = appString(Res.string.ui_not_logged_in_log_in_first_adding_favorites),
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@ForumTopBar
                    }
                    scope.launch {
                        val result = forumRepository.addFavorite(fid, formHash)
                        snackbarHostState.showSnackbar(
                            message = result.localizedMessage(),
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onHomeClick = {
                    navigator.popToRoot()
                    navigator.replace(IMainScreen(MainTab.Home))
                },
                onFavoriteClick = {
                    navigator.popToRoot()
                    navigator.replace(IMainScreen(MainTab.Favorite))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier =
                Modifier.padding(paddingValues)
                    .fillMaxSize()
                    .background(colors.creamBackground)
        ) {
            when (val current = state) {
                is ForumState.Loading -> ForumLoadingSkeleton()
                is ForumState.Error ->
                    ForumErrorContent(
                        message = current.message,
                        onRetry = {
                            state = ForumState.Loading
                            scope.launch { loadPage(currentPage) }
                        }
                    )

                is ForumState.Success ->
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            scope.launch {
                                when (val result =
                                    forumRepository.fetchForum(fid, currentPage, selectedFilterType, selectedOrderType)
                                ) {
                                    is YamiboResult.Success -> {
                                        forumRepository.setCachedForumPage(
                                            fid,
                                            currentPage,
                                            result.value,
                                            selectedFilterType,
                                            selectedOrderType,
                                        )
                                        state = ForumState.Success(result.value)
                                    }

                                    else -> {
                                        val msg = result.localizedMessage()
                                        snackbarHostState.showSnackbar(
                                            message = appString(Res.string.forum_refresh_failed, msg),
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ForumContent(
                            forumPage = current.page,
                            selectedOrderType = selectedOrderType,
                            selectedFilterType = selectedFilterType,
                            onShowOrderDialog = { showOrderDialog = true },
                            onShowFilterDialog = { showFilterDialog = true },
                            onPageChange = { page ->
                                state = ForumState.Loading
                                scope.launch { loadPage(page) }
                            },
                            onSubForumClick = { subFid, subName ->
                                navigator.navigate(IForumScreen(subFid, subName))
                            },
                            onPinnedItemClick = { item ->
                                when (item) {
                                    is PinnedItem.Thread -> {
                                        navigator.navigate(
                                            IThreadReaderScreen(
                                                tid = item.tid,
                                                title = item.title
                                            )
                                        )
                                    }
                                    is PinnedItem.Announcement -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                appString(Res.string.ui_jumping_announcement_page_not_supported_yet),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            },
                            onThreadClick = { thread ->
                                if (YamiboForum.isNovelForum(fid)) {
                                    navigator.navigate(
                                        INovelThreadDetailScreen(
                                            thread.tid,
                                            thread.title,
                                            thread.author?.uid
                                        )
                                    )
                                } else {
                                    navigator.navigate(
                                        IThreadReaderScreen(
                                            tid = thread.tid,
                                            title = thread.title
                                        )
                                    )
                                }
                            },
                            onAuthorClick = { user ->
                                navigator.navigate(IUserSpaceScreen(user.uid, user.name))
                            },
                        )
                    }
            }
        }
    }

    val currentForumPage = (state as? ForumState.Success)?.page
    if (showOrderDialog && currentForumPage != null) {
        ForumOrderDialog(
            options = currentForumPage.orderType.orEmpty(),
            selected = selectedOrderType,
            onDismiss = { showOrderDialog = false },
            onSelect = { orderType ->
                showOrderDialog = false
                selectedOrderType = orderType
                state = ForumState.Loading
                scope.launch { loadPage(currentPage, selectedFilterType, orderType) }
            },
        )
    }
    if (showFilterDialog && currentForumPage != null) {
        ForumFilterTypeDialog(
            options = currentForumPage.filterTypes.orEmpty(),
            selected = selectedFilterType,
            onDismiss = { showFilterDialog = false },
            onSelect = { filterType ->
                showFilterDialog = false
                selectedFilterType = filterType
                state = ForumState.Loading
                scope.launch { loadPage(currentPage, filterType, selectedOrderType) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForumTopBar(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onPostThread: () -> Unit,
    onStarClick: () -> Unit,
    onHomeClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val colors = YamiboTheme.colors
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp) }
        },
        actions = {
            IconButton(onClick = onSearch, modifier = Modifier.offset(y = 5.dp)) {
                Icon(
                    imageVector = YamiboIcons.Search,
                    contentDescription = appString(Res.string.read_history_search),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Box(modifier = Modifier.offset(y = 0.dp)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = YamiboIcons.ThreeDots,
                        contentDescription = appString(Res.string.ui_more),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(colors.creamSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.ui_post_2), color = colors.brownDeep) },
                        leadingIcon = {
                            Icon(
                                imageVector = YamiboIcons.EditOrSign,
                                contentDescription = null,
                                tint = colors.brownPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onPostThread()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.ui_bookmark_edition), color = colors.brownDeep) },
                        leadingIcon = {
                            Icon(
                                imageVector = YamiboIcons.StarOutline,
                                contentDescription = null,
                                tint = colors.brownPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onStarClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.ui_jump_homepage), color = colors.brownDeep) },
                        leadingIcon = {
                            Icon(
                                imageVector = YamiboIcons.Home,
                                contentDescription = null,
                                tint = colors.brownPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onHomeClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appString(Res.string.ui_jump_favorites), color = colors.brownDeep) },
                        leadingIcon = {
                            Icon(
                                imageVector = YamiboIcons.Explore,
                                contentDescription = null,
                                tint = colors.brownPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onFavoriteClick()
                        }
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colors.brownDeep,
                scrolledContainerColor = colors.brownDeep
            )
    )
}

/** Forum Content (scrollable body below sticky top bar) */
@Composable
private fun ForumContent(
    forumPage: ForumPage,
    selectedOrderType: OrderType?,
    selectedFilterType: FilterType?,
    onShowOrderDialog: () -> Unit,
    onShowFilterDialog: () -> Unit,
    onPageChange: (Int) -> Unit,
    onSubForumClick: (ForumId, String) -> Unit,
    onPinnedItemClick: (PinnedItem) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit,
    onAuthorClick: (User) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        /** forum stats bar */
        item {
            ForumStatsBar(
                forum = forumPage.forum,
                selectedOrderType = selectedOrderType,
                selectedFilterType = selectedFilterType,
                showOrder = !forumPage.orderType.isNullOrEmpty(),
                showFilter = !forumPage.filterTypes.isNullOrEmpty(),
                onOrderClick = onShowOrderDialog,
                onFilterClick = onShowFilterDialog,
            )
        }

        /** sub forums */
        if (forumPage.subForums.isNotEmpty()) {
            item { SubForumRow(subForums = forumPage.subForums, onClick = onSubForumClick) }
        }

        /** pinned items */
        if (forumPage.pinnedItems.isNotEmpty()) {
            item { PinnedSection(items = forumPage.pinnedItems, onItemClick = onPinnedItemClick) }
        }

        /** thread list */
        items(forumPage.threads, key = { it.tid.value }) { thread ->
            ThreadCard(
                thread = thread,
                onClick = { onThreadClick(thread) },
                onAuthorClick = onAuthorClick,
            )
        }

        /** page navigation */
        if (forumPage.pageNav != null) {
            item { PageNavigation(pageNav = forumPage.pageNav!!, onPageChange = onPageChange) }
        }
    }
}

@Composable
private fun ForumOrderDialog(
    options: List<OrderType>,
    selected: OrderType?,
    onDismiss: () -> Unit,
    onSelect: (OrderType?) -> Unit,
) {
    ForumOptionDialog(
        title = appString(Res.string.ui_sort),
        defaultLabel = appString(Res.string.common_all),
        options = options,
        selected = selected,
        optionLabel = { it.name },
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
private fun ForumFilterTypeDialog(
    options: List<FilterType>,
    selected: FilterType?,
    onDismiss: () -> Unit,
    onSelect: (FilterType?) -> Unit,
) {
    ForumOptionDialog(
        title = appString(Res.string.ui_classification),
        defaultLabel = appString(Res.string.common_all),
        options = options,
        selected = selected,
        optionLabel = { it.name },
        onDismiss = onDismiss,
        onSelect = onSelect,
    )
}

@Composable
private fun <T> ForumOptionDialog(
    title: String,
    defaultLabel: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T?) -> Unit,
) {
    val colors = YamiboTheme.colors
    val visibleOptions = options
        .distinctBy(optionLabel)
        .filterNot { optionLabel(it) == defaultLabel }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = colors.brownDeep, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ForumOptionRow(
                    label = defaultLabel,
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
                visibleOptions.forEach { option ->
                    ForumOptionRow(
                        label = optionLabel(option),
                        selected = selected == option,
                        onClick = { onSelect(option) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun ForumOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = colors.textDark,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text("✓", color = colors.brownDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Loading skeleton */
@Composable
private fun ForumLoadingSkeleton() {
    val colors = YamiboTheme.colors
    val shimmerColor = colors.brownLight

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val shimmerAnim = rememberInfiniteTransition(label = "forum_shimmer")
        val shimmerX by
        shimmerAnim.animateFloat(
            initialValue = -widthPx,
            targetValue = widthPx * 2f,
            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
            label = "forum_shimmer_x"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.creamBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(70.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shimmer(shimmerX, shimmerColor)
                )
            }
            items(6) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .shimmer(shimmerX, shimmerColor)
                )
            }
        }
    }
}

/** Shimmer modifier */
private fun Modifier.shimmer(translateX: Float, baseColor: Color): Modifier =
    this.drawBehind {
        val brush =
            Brush.linearGradient(
                colors =
                    listOf(
                        baseColor.copy(alpha = 0.25f),
                        baseColor.copy(alpha = 0.50f),
                        baseColor.copy(alpha = 0.25f),
                    ),
                start = Offset(translateX, 0f),
                end = Offset(translateX + size.width, size.height)
            )
        drawRect(brush)
    }

/** Error content */
@Composable
private fun ForumErrorContent(message: String, onRetry: () -> Unit) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.creamBackground).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(6.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = appString(Res.string.ui_loading_failed),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.brownDeep
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = colors.brownPrimary.copy(alpha = 0.75f),
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                    onClick = onRetry,
                    shape = RoundedCornerShape(50),
                    color = colors.brownDeep,
                    contentColor = Color.White
                ) {
                    Text(
                        text = appString(Res.string.ui_try_again),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}



