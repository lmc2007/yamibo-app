package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalThreadRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadErrorContent
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadLoadingSkeleton
import me.thenano.yamibo.yamibo_app.thread.novel.components.ThreadTopBar
import me.thenano.yamibo.yamibo_app.thread.render.PostRenderer

internal sealed interface ReaderState {
    data object Loading : ReaderState
    data object Success : ReaderState
    data class Error(val message: String) : ReaderState
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("AssignedValueIsNeverRead")
@Composable
internal fun ThreadReaderScreen(
    tid: ThreadId,
    title: String,
    authorId: UserId? = null,
    initialPage: Int = 1,
    targetPid: PostId? = null
) {
    val colors = YamiboTheme.colors
    val threadRepository = LocalThreadRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var state by remember { mutableStateOf<ReaderState>(ReaderState.Loading) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var threadInfo by remember { mutableStateOf<ThreadInfo?>(null) }
    var loadedPages by remember { mutableStateOf(setOf<Int>()) }
    var currentPageFetching by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var isLoadingNextPage by remember { mutableStateOf(false) }

    // local state to mapping page -> posts for the catalog drawer
    val loadedPostsByPage = remember { mutableStateMapOf<Int, List<Post>>() }

    var showMenu by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val authRepo = LocalAuthRepository.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun getFormHash(): FormHash? {
        return authRepo.currentUser()?.formHash
    }

    // Logic to load a specific page
    suspend fun loadPage(page: Int) {
        if (page in loadedPages) return

        isLoadingNextPage = true

        // Check cache first for page 1
        if (page == 1) {
            val cached = threadRepository.getCachedThread(tid, page)
            if (cached != null) {
                posts = cached.posts
                loadedPostsByPage[page] = cached.posts
                threadInfo = cached.thread
                totalPages = cached.pageNav?.totalPages ?: 1
                loadedPages = loadedPages + page
                state = ReaderState.Success
                isLoadingNextPage = false
                return
            }
        }

        when (val result = threadRepository.fetchThread(tid, authorId, page)) {
            is YamiboResult.Success -> {
                val newPosts = result.value.posts
                // Merge and sort posts by PID or simply append
                posts = (posts + newPosts).distinctBy { it.pid }.sortedBy { it.floor }
                loadedPostsByPage[page] = newPosts
                totalPages = result.value.pageNav?.totalPages ?: 1
                loadedPages = loadedPages + page

                if (page == 1) {
                    threadInfo = result.value.thread
                    state = ReaderState.Success
                }
            }

            else -> {
                if (page == 1) {
                    state = ReaderState.Error(result.message())
                }
            }
        }
        isLoadingNextPage = false
    }

    // Initial load
    LaunchedEffect(tid, initialPage, targetPid) {
        loadPage(initialPage)

        // After loading the page, try to scroll to the target post
        if (targetPid != null && posts.isNotEmpty()) {
            val targetIndex = posts.indexOfFirst { it.pid == targetPid }
            if (targetIndex >= 0) {
                // Animate slightly or snap
                listState.scrollToItem(targetIndex)
            }
        }
    }

    // Infinite scroll detection
    LaunchedEffect(listState, state, isLoadingNextPage) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                if (state != ReaderState.Success || isLoadingNextPage) return@collect

                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@collect

                val lastVisibleItemIndex = visibleItems.last().index
                val totalItems = layoutInfo.totalItemsCount

                // If we are near the bottom (e.g. 5 items away)
                if (lastVisibleItemIndex >= totalItems - 5) {
                    val nextPage = (loadedPages.maxOrNull() ?: 0) + 1
                    if (nextPage <= totalPages) {
                        currentPageFetching = nextPage
                        scope.launch { loadPage(nextPage) }
                    }
                }
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.creamBackground,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                ThreadCatalogPanel(
                    totalPages = totalPages,
                    loadedPostsByPage = loadedPostsByPage,
                    onPageOrPostClick = { page, post ->
                        scope.launch {
                            if (post != null) {
                                // Specific post clicked: close and scroll
                                drawerState.close()
                                if (page !in loadedPages) {
                                    loadPage(page)
                                }
                                val targetIndex = posts.indexOfFirst { it.pid == post.pid }
                                if (targetIndex >= 0) listState.animateScrollToItem(targetIndex)
                            } else {
                                // Page toggle or navigation
                                if (page !in loadedPages) {
                                    // Not loaded: load it and stay in drawer
                                    loadPage(page)
                                } else {
                                    // Already loaded: close and scroll to start of page
                                    drawerState.close()
                                    val targetIndex =
                                        posts.indexOfFirst { loadedPostsByPage[page]?.contains(it) == true }
                                    if (targetIndex >= 0) listState.animateScrollToItem(targetIndex)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.creamBackground)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Accept the down even if already consumed by a child (e.g. link, button)
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        // Only toggle menu when:
                        // 1. Touch ended (not cancelled)
                        // 2. No child consumed the up event (links/buttons will consume theirs)
                        // 3. Tap is in center 1/3 of screen width
                        if (up != null && !up.isConsumed) {
                            val x = up.position.x
                            val width = size.width
                            if (x in (width / 3f)..(width * 2f / 3f)) {
                                showMenu = !showMenu
                            }
                        }
                    }
                }
        ) {
            when (val currentState = state) {
                is ReaderState.Loading -> Box(
                    modifier = Modifier.systemBarsPadding().fillMaxSize()
                ) { ThreadLoadingSkeleton() }

                is ReaderState.Error -> Box(modifier = Modifier.systemBarsPadding().fillMaxSize()) {
                    ThreadErrorContent(
                        message = currentState.message,
                        onRetry = {
                            state = ReaderState.Loading
                            scope.launch { loadPage(1) }
                        }
                    )
                }

                is ReaderState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 40.dp
                        )
                    ) {
                        itemsIndexed(posts, key = { _, post -> post.pid.value }) { index, post ->
                            PostRenderer(
                                post = post,
                                onVote = { optionIds ->
                                    val formHash = getFormHash()
                                    val fId = threadInfo?.forum?.fid
                                    if (formHash == null || fId == null) {
                                        scope.launch { snackbarHostState.showSnackbar("獲取登入資訊失敗，請重新登入") }
                                        return@PostRenderer
                                    }
                                    scope.launch {
                                        when (val res = threadRepository.votePoll(fId, tid, optionIds, formHash)) {
                                            is YamiboResult.Success -> snackbarHostState.showSnackbar("投票成功")
                                            else -> snackbarHostState.showSnackbar("投票失敗: ${res.message()}")
                                        }
                                    }
                                },
                                onRate = { score, reason ->
                                    val formHash = getFormHash()
                                    if (formHash == null) {
                                        scope.launch { snackbarHostState.showSnackbar("獲取登入資訊失敗，請重新登入") }
                                        return@PostRenderer
                                    }
                                    scope.launch {
                                        when (val res =
                                            threadRepository.ratePost(tid, post.pid, score, reason, formHash)) {
                                            is YamiboResult.Success -> snackbarHostState.showSnackbar("評分成功，刷新後更新評分/點評狀態")
                                            else -> snackbarHostState.showSnackbar("評分失敗: ${res.message()}")
                                        }
                                    }
                                },
                                onComment = { message ->
                                    val formHash = getFormHash()
                                    if (formHash == null) {
                                        scope.launch { snackbarHostState.showSnackbar("獲取登入資訊失敗，請重新登入") }
                                        return@PostRenderer
                                    }
                                    scope.launch {
                                        when (val res =
                                            threadRepository.commentPost(tid, post.pid, message, formHash)) {
                                            is YamiboResult.Success -> snackbarHostState.showSnackbar("點評成功，刷新後更新評分/點評狀態")
                                            else -> snackbarHostState.showSnackbar("點評失敗: ${res.message()}")
                                        }
                                    }
                                }
                            )

                            // Separator between posts
                            if (index < posts.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = colors.brownPrimary.copy(alpha = 0.15f)
                                )
                            }
                        }

                        if (isLoadingNextPage) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.brownPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        if (loadedPages.size == totalPages && posts.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "- 沒有更多內容了 -",
                                        color = colors.textDark.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Overlay Menu (TopBar)
            // Snackbar host for feedback messages
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp)
            )

            // Overlay Menu (TopBar)
            AnimatedVisibility(
                visible = showMenu,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = colors.brownDeep,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThreadTopBar(
                        title = title,
                        onBack = { navigator.pop() },
                        windowInsets = WindowInsets.statusBars,
                        actions = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "☰",
                                    color = Color.White,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ThreadCatalogPanel(
    totalPages: Int,
    loadedPostsByPage: Map<Int, List<Post>>,
    onPageOrPostClick: (Int, Post?) -> Unit
) {
    val colors = YamiboTheme.colors
    var expandedPages by remember { mutableStateOf(setOf<Int>()) }

    Column(modifier = Modifier.fillMaxSize().background(colors.creamBackground).systemBarsPadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.brownDeep)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "目錄 (Catalog)",
                color = colors.creamBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(totalPages) { index ->
                val page = index + 1
                val isExpanded = expandedPages.contains(page)
                val isLoaded = loadedPostsByPage.containsKey(page)

                val rotation by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "page_chevron"
                )

                Column {
                    // Page Header
                    Surface(
                        color = if (isExpanded) colors.brownLight.copy(alpha = 0.1f) else colors.creamBackground,
                        onClick = {
                            expandedPages = if (isExpanded) expandedPages - page else expandedPages + page
                            if (!isLoaded) {
                                onPageOrPostClick(page, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第 $page 頁",
                                color = colors.brownPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "▲",
                                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                                color = colors.brownPrimary.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Content List (Expanded)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isExpanded,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        if (!isLoaded) {
                            // Loading state
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = colors.brownDeep,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            // Posts List
                            val pagePosts = loadedPostsByPage[page] ?: emptyList()
                            Column(modifier = Modifier.fillMaxWidth().background(colors.creamSurface)) {
                                pagePosts.forEach { post ->
                                    Surface(
                                        color = colors.creamSurface,
                                        onClick = { onPageOrPostClick(page, post) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                            Text(
                                                text = "${post.floor}#",
                                                color = colors.brownPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.width(40.dp)
                                            )
                                            Text(
                                                text = post.title.ifEmpty { "..." },
                                                color = colors.textDark,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = colors.brownLight.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(start = 24.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.2f))
                }
            }
        }
    }
}
