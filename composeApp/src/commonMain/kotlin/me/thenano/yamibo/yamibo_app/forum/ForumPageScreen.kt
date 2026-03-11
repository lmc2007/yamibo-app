package me.thenano.yamibo.yamibo_app.forum

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.ForumPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.YamiboForum
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.IMainScreen
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalForumRepository
import me.thenano.yamibo.yamibo_app.MainTab
import me.thenano.yamibo.yamibo_app.forum.components.*
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen

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
    var showSearch by remember { mutableStateOf(false) }

    suspend fun loadPage(page: Int) {
        val cached = forumRepository.getCachedForumPage(fid, page)
        if (cached != null) {
            currentPage = cached.pageNav?.currentPage ?: page
            state = ForumState.Success(cached)
            return
        }

        val result = forumRepository.fetchForum(fid, page)
        state =
            when (result) {
                is YamiboResult.Success -> {
                    currentPage = result.value.pageNav?.currentPage ?: page
                    ForumState.Success(result.value)
                }

                else -> ForumState.Error(result.message())
            }
    }

    LaunchedEffect(fid) {
        val cached = forumRepository.getCachedForumPage(fid)
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
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colors.brownDeep,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
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
                onSearch = { showSearch = true },
                onStarClick = {
                    val formHash = authRepository.currentUser()?.formHash
                    if (formHash == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "未登入，請先登入後再收藏",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@ForumTopBar
                    }
                    scope.launch {
                        val result = forumRepository.addFavorite(fid, formHash)
                        snackbarHostState.showSnackbar(
                            message = result.message(),
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
                                    forumRepository.fetchForum(fid, currentPage)
                                ) {
                                    is YamiboResult.Success -> {
                                        forumRepository.clearCachedForum(fid)
                                        forumRepository.setCachedForumPage(
                                            fid,
                                            currentPage,
                                            result.value
                                        )
                                        state = ForumState.Success(result.value)
                                    }

                                    else -> {
                                        val msg = result.message()
                                        snackbarHostState.showSnackbar(
                                            message = "刷新失敗：$msg",
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
                            onPageChange = { page ->
                                state = ForumState.Loading
                                scope.launch { loadPage(page) }
                            },
                            onSubForumClick = { subFid, subName ->
                                navigator.navigate(IForumScreen(subFid, subName))
                            },
                            onThreadClick = { thread ->
                                if (isNovelForum(fid)) {
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
                            }
                        )
                    }
            }
        }
    }

    /** Search modal overlay */
    if (showSearch) {
        SearchModal(
            fid = fid,
            onDismiss = {
                /** Just for making ide ignore this issue, the code works well */
                @Suppress("AssignedValueIsNeverRead")
                showSearch = false
            },
            onThreadClick = { thread ->
                if (isNovelForum(fid)) {
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
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForumTopBar(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
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
            IconButton(onClick = onBack) { Text("◀", color = Color.White, fontSize = 20.sp) }
        },
        actions = {
            IconButton(onClick = onSearch, modifier = Modifier.offset(y = 5.dp)) {
                Icon(
                    imageVector = YamiboIcons.Search,
                    contentDescription = "搜尋",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Box(modifier = Modifier.offset(y = 0.dp)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = YamiboIcons.ThreeDots,
                        contentDescription = "更多",
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
                        text = { Text("收藏本版", color = colors.brownDeep) },
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
                        text = { Text("跳轉到首頁", color = colors.brownDeep) },
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
                        text = { Text("跳轉到收藏", color = colors.brownDeep) },
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
    onPageChange: (Int) -> Unit,
    onSubForumClick: (ForumId, String) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        /** forum stats bar */
        item { ForumStatsBar(forum = forumPage.forum) }

        /** sub forums */
        if (forumPage.subForums.isNotEmpty()) {
            item { SubForumRow(subForums = forumPage.subForums, onClick = onSubForumClick) }
        }

        /** pinned items */
        if (forumPage.pinnedItems.isNotEmpty()) {
            item { PinnedSection(items = forumPage.pinnedItems) }
        }

        /** thread list */
        items(forumPage.threads, key = { it.tid.value }) { thread ->
            ThreadCard(thread = thread, onClick = { onThreadClick(thread) })
        }

        /** page navigation */
        if (forumPage.pageNav != null) {
            item { PageNavigation(pageNav = forumPage.pageNav!!, onPageChange = onPageChange) }
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
                    text = "載入失敗",
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
                        text = "重試",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/** Check if a forum is a novel forum that should use author-only thread viewing */
private fun isNovelForum(fid: ForumId): Boolean =
    fid.value == YamiboForum.LITERATURE.id.value ||
        fid.value == YamiboForum.TRANSLATED_LIGHT_NOVEL.id.value
