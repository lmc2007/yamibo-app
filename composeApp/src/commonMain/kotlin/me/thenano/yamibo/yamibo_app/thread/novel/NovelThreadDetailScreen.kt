package me.thenano.yamibo.yamibo_app.thread.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalThreadRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.novel.components.FirstFloorPreview
import me.thenano.yamibo.yamibo_app.thread.novel.components.PostPageSection
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadErrorContent
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadHeader
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadLoadingSkeleton
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadTopBar

/** Thread detail state */
internal sealed interface ThreadState {
    data object Loading : ThreadState
    data class Success(val page: ThreadPage) : ThreadState
    data class Error(val message: String) : ThreadState
}

/** Main Thread Detail Screen */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NovelThreadDetailScreen(tid: ThreadId, title: String, authorId: UserId? = null) {
    val colors = YamiboTheme.colors
    val threadRepository = LocalThreadRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<ThreadState>(ThreadState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pagePostsCache = remember { mutableStateMapOf<Int, List<Post>>() }
    var expandedPages by remember { mutableStateOf(setOf(1)) }

    suspend fun loadThread(page: Int = 1) {
        val result = threadRepository.fetchThread(tid, authorId, page)
        state =
            when (result) {
                is YamiboResult.Success -> {
                    pagePostsCache[page] = result.value.posts
                    ThreadState.Success(result.value)
                }

                is YamiboResult.Failure -> ThreadState.Error(result.reason)
                is YamiboResult.Maintenance -> ThreadState.Error(result.message())
                is YamiboResult.NotLoggedIn -> ThreadState.Error(result.message())
            }
    }

    /** initial load — use cache if available, only fetch on cold start */
    LaunchedEffect(tid) {
        val cached = threadRepository.getCachedThread(tid)
        if (cached != null) {
            pagePostsCache[1] = cached.posts
            state = ThreadState.Success(cached)
            return@LaunchedEffect
        }
        loadThread()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
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
            ThreadTopBar(
                title =
                    when (val s = state) {
                        is ThreadState.Success -> s.page.thread.title
                        else -> title
                    },
                onBack = { navigator.pop() }
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
                is ThreadState.Loading -> ThreadLoadingSkeleton()
                is ThreadState.Error ->
                    ThreadErrorContent(
                        message = current.message,
                        onRetry = {
                            state = ThreadState.Loading
                            scope.launch { loadThread() }
                        }
                    )

                is ThreadState.Success ->
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            threadRepository.clearCachedThread(tid)
                            pagePostsCache.clear() // Clear local cache as well
                            scope.launch {
                                when (val result =
                                    threadRepository.fetchThread(tid, authorId)
                                ) {
                                    is YamiboResult.Success -> {
                                        pagePostsCache[1] = result.value.posts
                                        state = ThreadState.Success(result.value)
                                    }

                                    else -> {
                                        val msg =
                                            when (result) {
                                                is YamiboResult.Failure -> result.reason
                                                is YamiboResult.Maintenance -> "伺服器維護中"
                                                is YamiboResult.NotLoggedIn -> "未登入"
                                            }
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
                        ThreadContent(
                            threadPage = current.page,
                            pagePostsCache = pagePostsCache,
                            expandedPages = expandedPages,
                            onTogglePage = { page ->
                                expandedPages =
                                    if (page in expandedPages) {
                                        expandedPages - page
                                    } else {
                                        expandedPages + page
                                    }
                            },
                            onLoadPage = { page ->
                                scope.launch {
                                    val result =
                                        threadRepository.fetchThread(
                                            tid,
                                            authorId,
                                            page
                                        )
                                    if (result is YamiboResult.Success) {
                                        pagePostsCache[page] = result.value.posts
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = "載入第 $page 頁失敗",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            onFavorite = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "收藏功能開發中",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            snackbarHostState = snackbarHostState,
                            scope = scope
                        )
                    }
            }
        }
    }
}

/** Thread content body */
@Composable
private fun ThreadContent(
    threadPage: ThreadPage,
    pagePostsCache: Map<Int, List<Post>>,
    expandedPages: Set<Int>,
    onTogglePage: (Int) -> Unit,
    onLoadPage: (Int) -> Unit,
    onFavorite: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val colors = YamiboTheme.colors
    val clipboardManager = LocalClipboardManager.current
    val thread = threadPage.thread
    val firstPost = threadPage.posts.firstOrNull()
    val totalPages = threadPage.pageNav?.totalPages ?: 1

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        /** Thread Header */
        item {
            ThreadHeader(
                threadPage = threadPage,
                onFavorite = onFavorite,
                onShare = {
                    val url = YamiboRoute.Thread(thread.tid).build()
                    clipboardManager.setText(AnnotatedString(url))
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "已複製連結",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onCopy = { message ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }

        /** First floor preview */
        if (firstPost != null) {
            item { FirstFloorPreview(post = firstPost) }
        }

        /** Divider */
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = colors.brownPrimary.copy(alpha = 0.15f)
            )
        }

        /** Page sections */
        items(totalPages) { index ->
            val page = index + 1
            val isExpanded = page in expandedPages
            val posts = pagePostsCache[page]

            PostPageSection(
                page = page,
                isExpanded = isExpanded,
                posts = posts,
                isFirstPage = page == 1,
                onToggle = {
                    onTogglePage(page)
                    if (posts == null) {
                        onLoadPage(page)
                    }
                }
            )
        }
    }
}
