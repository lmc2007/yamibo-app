package me.thenano.yamibo.yamibo_app.thread.reader

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.LocalThreadRepository
import me.thenano.yamibo.yamibo_app.__info__tag
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadErrorContent
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadLoadingSkeleton
import me.thenano.yamibo.yamibo_app.thread.reader.components.CommentBanner
import me.thenano.yamibo.yamibo_app.thread.reader.components.tag.ITagListScreen
import me.thenano.yamibo.yamibo_app.thread.reader.components.ReaderCatalogPanel
import me.thenano.yamibo.yamibo_app.thread.reader.components.ReaderOverlayMenu
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.PostRenderer
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView
import kotlin.math.abs
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageClickListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalImageDoubleClickListener
import me.thenano.yamibo.yamibo_app.thread.image.LocalReaderOverlayVisible

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
    targetPid: PostId? = null,
    isAuthorOnly: Boolean = false
) {
    val colors = YamiboTheme.colors
    val threadRepository = LocalThreadRepository.current
    val readHistoryRepo = LocalReadHistoryRepository.current
    val navigator = LocalNavigator.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var state by remember { mutableStateOf<ReaderState>(ReaderState.Loading) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var threadInfo by remember { mutableStateOf<ThreadInfo?>(null) }
    var loadedPages by remember { mutableStateOf(setOf<Int>()) }
    var currentPageFetching by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(1) }
    var isLoadingNextPage by remember { mutableStateOf(false) }

    val loadedPostsByPage = remember { mutableStateMapOf<Int, List<Post>>() }

    var showMenu by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val authRepo = LocalAuthRepository.current
    val snackbarHostState = remember { SnackbarHostState() }

    /** Debounced save job reference */
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var hasRestoredPosition by remember { mutableStateOf(false) }

    /** Extract image URL for thread avatar based on forum type */
    var coverUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(loadedPostsByPage[1], threadInfo) {
        if (coverUrl != null) return@LaunchedEffect // Lock once found for stability
        
        val fid = threadInfo?.forum?.fid
        val isMangaThread = fid?.let { YamiboForum.isMangaForum(it) } == true

        // Find the posts on the first page
        val firstPagePosts = loadedPostsByPage[1] ?: return@LaunchedEffect

        // Use the author from the very first post of the thread
        val threadAuthorId = firstPagePosts.firstOrNull()?.author?.uid ?: return@LaunchedEffect

        // Get posts by the thread author on the first page
        val authorPosts = firstPagePosts.filter { it.author.uid == threadAuthorId }

        val attachedImageUrl = if (isMangaThread) {
            val candidateImages = authorPosts.take(2).flatMap { it.images }
            candidateImages.getOrNull(1)?.url ?: candidateImages.getOrNull(0)?.url
        } else {
            authorPosts.firstOrNull()?.images?.firstOrNull()?.url
        } ?: return@LaunchedEffect

        if (attachedImageUrl.contains("none.gif") || attachedImageUrl.contains("smiley/") || attachedImageUrl.contains("face")) return@LaunchedEffect
        coverUrl = if (attachedImageUrl.startsWith("http")) attachedImageUrl else "${YamiboRoute.Domain.build()}$attachedImageUrl"
    }

    fun getFormHash(): FormHash? {
        return authRepo.currentUser()?.formHash
    }

    val handleVote: (List<PollOptionId>) -> Unit = { optionIds ->
        val formHash = getFormHash()
        val fId = threadInfo?.forum?.fid
        if (formHash == null || fId == null) {
            scope.launch { snackbarHostState.showSnackbar("獲取登入資訊失敗，請重新登入") }
        } else {
            scope.launch {
                when (val res = threadRepository.votePoll(fId, tid, optionIds, formHash)) {
                    is YamiboResult.Success -> snackbarHostState.showSnackbar("投票成功")
                    else -> snackbarHostState.showSnackbar("投票失敗: ${res.message()}")
                }
            }
        }
    }

    val handleRate: (PostId, Int, String) -> Unit = { pid, score, reason ->
        val formHash = getFormHash()
        if (formHash == null) {
            scope.launch { snackbarHostState.showSnackbar("獲取登入資訊失敗，請重新登入") }
        } else {
            scope.launch {
                when (val res = threadRepository.ratePost(tid, pid, score, reason, formHash)) {
                    is YamiboResult.Success -> snackbarHostState.showSnackbar("評分成功，刷新後更新評分/點評狀態")
                    else -> snackbarHostState.showSnackbar("評分失敗: ${res.message()}")
                }
            }
        }
    }

    val handleComment: (PostId, String) -> Unit = { pid, message ->
        val formHash = getFormHash()
        if (formHash == null) {
            scope.launch { snackbarHostState.showSnackbar("獲取登入資訊失敗，請重新登入") }
        } else {
            scope.launch {
                when (val res = threadRepository.commentPost(tid, pid, message, formHash)) {
                    is YamiboResult.Success -> snackbarHostState.showSnackbar("點評成功，刷新後更新評分/點評狀態")
                    else -> snackbarHostState.showSnackbar("點評失敗: ${res.message()}")
                }
            }
        }
    }

    val handleReply: (PostId) -> Unit = { pid ->
        val replyPageUrl = YamiboRoute.PostReply(tid, pid).build()
        navigator.navigate(
            IActionWebView(
                title = "發表回復",
                initialUrl = replyPageUrl,
                successCondition = { url -> url.contains("mod=viewthread") && url.contains("tid=") },
                onSuccess = { scope.launch { snackbarHostState.showSnackbar("回復成功") } },
            )
        )
    }

    fun rebuildPosts() {
        val allPostsMutable = mutableListOf<Post>()
        loadedPostsByPage.keys.sorted().forEach { p ->
            allPostsMutable.addAll(loadedPostsByPage[p]!!)
        }
        posts = allPostsMutable.distinctBy { it.pid }.sortedBy { it.floor }
    }

    /** Build a reading history snapshot from current scroll state (does NOT save) */
    fun buildHistory(): ThreadReadingHistory? {
        if (posts.isEmpty()) return null
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return null

        /** Find visible post at viewport center */
        val viewportTop = layoutInfo.viewportStartOffset
        val viewportBottom = layoutInfo.viewportEndOffset
        val viewportCenter = (viewportTop + viewportBottom) / 2

        val centerItem = visibleItems.firstOrNull { item ->
            item.offset <= viewportCenter && item.offset + item.size >= viewportCenter
        } ?: visibleItems.first()

        val centerPostIndex = centerItem.index.coerceIn(0, posts.lastIndex)
        val centerPost = posts[centerPostIndex]

        /** Calculate ratio within this post */
        val postTop = centerItem.offset
        val postSize = centerItem.size.coerceAtLeast(1)
        val anchorPostRatio = ((viewportCenter - postTop).toFloat() / postSize.toFloat()).coerceIn(0f, 1f)

        /** Find which page this post is on */
        val postPage = loadedPostsByPage.entries
            .firstOrNull { (_, pagePosts) -> pagePosts.any { it.pid == centerPost.pid } }
            ?.key ?: initialPage

        val forumInfo = threadInfo?.forum
        val firstVisible = listState.firstVisibleItemIndex
        val firstVisibleOffset = listState.firstVisibleItemScrollOffset
        Log.i(__info__tag("ThreadReaderScreen"), "$title : $coverUrl")
        return ThreadReadingHistory(
            threadName = title,
            threadId = tid,
            threadCover = coverUrl,
            forumName = forumInfo?.name,
            forumId = forumInfo?.fid,
            authorId = authorId,
            page = postPage,
            postId = centerPost.pid,
            postTitle = centerPost.title,
            anchorPostId = centerPost.pid.value.toLong(),
            anchorPostRatio = anchorPostRatio,
            anchorBlockId = null,
            anchorBlockType = null,
            anchorBlockRatio = null,
            globalScrollY = null,
            viewportHeight = (viewportBottom - viewportTop),
            firstVisibleItemIndex = firstVisible,
            firstVisibleItemOffset = firstVisibleOffset,
            lastVisitTime = currentTimeMillis()
        )
    }

    /** Debounced save - wait 2 seconds after scroll stops */
    fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(2000)
            val history = buildHistory() ?: return@launch
            try {
                readHistoryRepo.savePosition(history)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun loadPage(page: Int, forceRefresh: Boolean = false) {
        if (!forceRefresh && page in loadedPages) return
        isLoadingNextPage = true

        fun loadFromCache(): Boolean {
            val cached = threadRepository.getCachedThread(tid, authorId, page)
            if (cached != null) {
                loadedPostsByPage[page] = cached.posts
                rebuildPosts()
                if (threadInfo == null) threadInfo = cached.thread
                totalPages = cached.pageNav?.totalPages ?: 1
                loadedPages = loadedPages + page
                if (page == initialPage || page == 1) state = ReaderState.Success
                return true
            }
            return false
        }

        if (forceRefresh) {
            when (val result = threadRepository.fetchThread(tid, authorId, page)) {
                is YamiboResult.Success -> {
                    loadedPostsByPage[page] = result.value.posts
                    rebuildPosts()
                    totalPages = result.value.pageNav?.totalPages ?: 1
                    loadedPages = loadedPages + page
                    if (threadInfo == null) threadInfo = result.value.thread
                    if (page == initialPage || page == 1) state = ReaderState.Success
                }

                else -> {
                    snackbarHostState.showSnackbar("刷新失敗: ${result.message()}，嘗試讀取快取")
                    if (!loadFromCache() && (page == initialPage || page == 1)) {
                        state = ReaderState.Error(result.message())
                    }
                }
            }
        } else {
            if (loadFromCache()) {
                isLoadingNextPage = false
                return
            }

            when (val result = threadRepository.fetchThread(tid, authorId, page)) {
                is YamiboResult.Success -> {
                    loadedPostsByPage[page] = result.value.posts
                    rebuildPosts()
                    totalPages = result.value.pageNav?.totalPages ?: 1
                    loadedPages = loadedPages + page
                    if (threadInfo == null) threadInfo = result.value.thread
                    if (page == initialPage || page == 1) state = ReaderState.Success
                }

                else -> {
                    if (page == initialPage || page == 1) state = ReaderState.Error(result.message())
                    else snackbarHostState.showSnackbar("載入失敗: ${result.message()}")
                }
            }
        }
        isLoadingNextPage = false
    }

    suspend fun fallbackNearestPost(targetPidLong: Long, fallbackPage: Int) {
        if (posts.isEmpty() || state is ReaderState.Error) return
        var targetPage = fallbackPage
        val maxPid = posts.maxOfOrNull { it.pid.value.toLong() } ?: return
        val minPid = posts.minOfOrNull { it.pid.value.toLong() } ?: return

        if (targetPidLong > maxPid && targetPage < totalPages) {
            targetPage++
            if (targetPage !in loadedPages) loadPage(targetPage)
        } else if (targetPidLong < minPid && targetPage > 1) {
            targetPage--
            if (targetPage !in loadedPages) loadPage(targetPage)
        }

        val nearestIndex =
            posts.indices.minByOrNull { abs(posts[it].pid.value.toLong() - targetPidLong) } ?: -1
        if (nearestIndex >= 0) {
            listState.scrollToItem(nearestIndex)
            hasRestoredPosition = true
            if (posts[nearestIndex].pid.value.toLong() != targetPidLong) {
                snackbarHostState.showSnackbar("找不到指定的樓層，已跳轉至最接近的樓層")
            }
        }
    }

    // Initial load + position restore
    LaunchedEffect(tid, initialPage, targetPid) {
        loadPage(initialPage)

        if (targetPid != null && posts.isNotEmpty()) {
            val targetIndex = posts.indexOfFirst { it.pid == targetPid }
            if (targetIndex >= 0) {
                listState.scrollToItem(targetIndex)
                hasRestoredPosition = true
            } else {
                fallbackNearestPost(targetPid.value.toLong(), initialPage)
            }
        }

        /** Restore position from history if no explicit targetPid */
        if (!hasRestoredPosition && targetPid == null) {
            try {
                val savedPosition = readHistoryRepo.getPosition(tid)
                if (savedPosition != null) {
                    // Ensure the saved page is loaded
                    if (savedPosition.page != initialPage) {
                        loadPage(savedPosition.page)
                    }

                    // Restore by firstVisibleItemIndex/offset (most reliable) if post matches
                    val savedIndex = savedPosition.firstVisibleItemIndex
                    val savedOffset = savedPosition.firstVisibleItemOffset
                    if (savedIndex != null && savedIndex >= 0 && savedIndex < posts.size) {
                        val postAtSavedIndex = posts[savedIndex]
                        if (postAtSavedIndex.pid.value.toLong() == savedPosition.anchorPostId) {
                            listState.scrollToItem(savedIndex, savedOffset ?: 0)
                            hasRestoredPosition = true
                        }
                    }

                    // Fallback: restore by post ID
                    if (!hasRestoredPosition && savedPosition.anchorPostId > 0) {
                        val postIndex = posts.indexOfFirst {
                            it.pid.value.toLong() == savedPosition.anchorPostId
                        }
                        if (postIndex >= 0) {
                            listState.scrollToItem(postIndex)
                            hasRestoredPosition = true
                        } else {
                            fallbackNearestPost(savedPosition.anchorPostId, savedPosition.page)
                        }
                    }
                }
            } catch (_: Exception) {
            }
            hasRestoredPosition = true
        }
    }

    /** Scroll detection → debounced position save */
    LaunchedEffect(listState, state) {
        if (state != ReaderState.Success) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { scheduleSave() }
    }

    // Infinite scroll detection
    LaunchedEffect(listState, state, isLoadingNextPage) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                if (state != ReaderState.Success || isLoadingNextPage) return@collect

                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@collect

                val firstVisibleItemIndex = visibleItems.first().index
                val lastVisibleItemIndex = visibleItems.last().index

                // Detect if we are close to the beginning of the CURRENT loaded page block
                val firstPost = posts.getOrNull(firstVisibleItemIndex)
                if (firstPost != null) {
                    val page =
                        loadedPostsByPage.entries.firstOrNull { (_, list) -> list.any { it.pid == firstPost.pid } }?.key
                            ?: 1
                    val firstIndex =
                        posts.indexOfFirst { p -> loadedPostsByPage[page]?.any { it.pid == p.pid } == true }
                    if (firstVisibleItemIndex - firstIndex <= 5) {
                        val prevPage = page - 1
                        if (prevPage >= 1 && prevPage !in loadedPages) {
                            currentPageFetching = prevPage
                            scope.launch { loadPage(prevPage) }
                            return@collect
                        }
                    }
                }

                // Detect if we are close to the end of the CURRENT loaded page block
                val lastPost = posts.getOrNull(lastVisibleItemIndex)
                if (lastPost != null) {
                    val page =
                        loadedPostsByPage.entries.firstOrNull { (_, list) -> list.any { it.pid == lastPost.pid } }?.key
                            ?: 1
                    val lastIndex = posts.indexOfLast { p -> loadedPostsByPage[page]?.any { it.pid == p.pid } == true }
                    if (lastIndex - lastVisibleItemIndex <= 5) {
                        val nextPage = page + 1
                        if (nextPage <= totalPages && nextPage !in loadedPages) {
                            currentPageFetching = nextPage
                            scope.launch { loadPage(nextPage) }
                            return@collect
                        }
                    }
                }
            }
    }

    /** Save on leaving screen — use runBlocking since scope is already canceled */
    DisposableEffect(tid) {
        onDispose {
            saveJob?.cancel()
            val history = buildHistory()
            if (history != null) {
                runBlocking {
                    try {
                        readHistoryRepo.savePosition(history)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    val currentVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val currentVisiblePost = posts.getOrNull(currentVisibleItemIndex)
    val currentPage = currentVisiblePost?.let { p ->
        loadedPostsByPage.entries.firstOrNull { (_, list) -> list.any { it.pid == p.pid } }?.key
    } ?: initialPage
    val currentPid = currentVisiblePost?.pid

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = colors.creamBackground,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                ReaderCatalogPanel(
                    totalPages = totalPages,
                    loadedPostsByPage = loadedPostsByPage,
                    currentPage = currentPage,
                    currentPid = currentPid,
                    onPageOrPostClick = { page, post ->
                        scope.launch {
                            if (post != null) {
                                drawerState.close()
                                if (page !in loadedPages) {
                                    loadPage(page)
                                    delay(50) // Wait briefly for Compose to layout the new items
                                }
                                val targetIndex = posts.indexOfFirst { it.pid == post.pid }
                                if (targetIndex >= 0) listState.scrollToItem(targetIndex)
                            } else {
                                // User just clicked the page header to expand catalog, load the page, don't close drawer
                                if (page !in loadedPages) {
                                    loadPage(page)
                                }
                            }
                        }
                    }
                )
            }
        }
    ) {
        val handleImageDoubleTap: (String) -> Unit = { url ->
            val post = posts.firstOrNull { p -> p.images.any { it.url.endsWith(url) || url.endsWith(it.url) } }
            if (post != null) {
                val imageList = post.images.map { img ->
                    if (img.url.startsWith("http")) img.url else "${YamiboRoute.Domain.build()}${img.url}"
                }
                val cleanUrl = if (url.startsWith("http")) url else "${YamiboRoute.Domain.build()}$url"
                val initialIndex = imageList.indexOfFirst { it == cleanUrl }.coerceAtLeast(0)

                navigator.navigate(
                    IImageReaderScreen(
                        tid = tid,
                        postId = post.pid,
                        fid = threadInfo?.forum?.fid,
                        authorId = authorId,
                        threadTitle = title,
                        imageList = imageList,
                        initialPage = initialIndex + 1
                    )
                )
            }
        }

        CompositionLocalProvider(
            LocalReaderOverlayVisible provides showMenu,
            LocalImageClickListener provides { showMenu = !showMenu },
            LocalImageDoubleClickListener provides handleImageDoubleTap
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.creamBackground)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = waitForUpOrCancellation()
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
                                    threadTitle = title,
                                    onVote = { optionIds -> handleVote(optionIds) },
                                    onRate = { score, reason -> handleRate(post.pid, score, reason) },
                                    onComment = { message -> handleComment(post.pid, message) },
                                    onReply = { handleReply(post.pid) }
                                )

                                // Tag Banner (Always shown in the first post of the first page, tags are fetched via API by TagListScreen)
                                if (post.floor == 1 && currentPage == 1) {
                                    val fid = threadInfo?.forum?.fid
                                    val isManga = fid?.let { YamiboForum.isMangaForum(it) } == true
                                    val isNovel = fid?.let { YamiboForum.isNovelForum(it) } == true

                                    // Manga forum / Other forum (non-novel, non-authorOnly): Place after PostRenderer
                                    if (isManga || (!isNovel && !isAuthorOnly)) {
                                        CommentBanner(
                                            text = "查看標籤列表",
                                            icon = "🏷️",
                                            onClick = {
                                                navigator.navigate(
                                                    ITagListScreen(
                                                        tid = tid,
                                                        initialTags = post.tags.value
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }

                                // Author-only mode: tag banner + comment banner
                                if (isAuthorOnly) {
                                    // Novel forum: Tag Banner is above the comments button
                                    if (post.floor == 1 && currentPage == 1) {
                                        val fid = threadInfo?.forum?.fid
                                        val isNovel = fid?.let { YamiboForum.isNovelForum(it) } == true
                                        if (isNovel) {
                                            CommentBanner(
                                                text = "查看標籤列表",
                                                icon = "🏷️",
                                                onClick = {
                                                    navigator.navigate(
                                                        ITagListScreen(
                                                            tid = tid,
                                                            initialTags = post.tags.value
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    CommentBanner(
                                        text = "點擊跳轉到評論區",
                                        onClick = {
                                            navigator.navigate(
                                                ICommentReaderScreen(
                                                    tid = tid,
                                                    postTitle = post.title.ifEmpty { "第${post.floor}樓" },
                                                    oPostId = post.pid,
                                                    authorId = authorId!!
                                                )
                                            )
                                        }
                                    )
                                }

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

                // Manga reader button visibility
                val isMangaForum = threadInfo?.forum?.fid?.let { YamiboForum.isMangaForum(it) } == true
                val isFirstPage = currentPage == 1
                val showMangaReader = isMangaForum && isFirstPage

                // Overlay menu
                ReaderOverlayMenu(
                    visible = showMenu,
                    title = title,
                    snackbarHostState = snackbarHostState,
                    onBack = { navigator.pop() },
                    onCatalog = { scope.launch { drawerState.open() } },
                    onFavorite = {
                        scope.launch {
                            snackbarHostState.showSnackbar("收藏功能開發中")
                        }
                    },
                    onShare = {
                        val url = YamiboRoute.Thread(tid).build()

                        clipboardManager.setText(AnnotatedString(url))
                        scope.launch {
                            snackbarHostState.showSnackbar("已複製連結")
                        }
                    },
                    onReply = {
                        val replyUrl = YamiboRoute.ThreadReply(tid, loadedPages.maxOrNull() ?: 1).build()
                        navigator.navigate(
                            IActionWebView(
                                title = "發表回復",
                                initialUrl = replyUrl,
                                successCondition = { url -> url.contains("mod=viewthread") && url.contains("tid=") },
                                onSuccess = {
                                    scope.launch { snackbarHostState.showSnackbar("回復成功") }
                                },
                            )
                        )
                    },
                    // Reload the current page
                    onRefresh = {
                        scope.launch {
                            state = ReaderState.Loading
                            loadPage(currentPage, forceRefresh = true)
                        }
                    },
                    onSettings = {
                        scope.launch {
                            snackbarHostState.showSnackbar("設定功能開發中")
                        }
                    },
                    showMangaReader = showMangaReader,
                    onMangaReader = {
                        val firstPost = posts.firstOrNull()
                        if (firstPost != null) {
                            val firstPostImages = firstPost.images.map { img ->
                                if (img.url.startsWith("http")) img.url else "${YamiboRoute.Domain.build()}${img.url}"
                            }
                            navigator.navigate(
                                IImageReaderScreen(
                                    tid = tid,
                                    postId = firstPost.pid,
                                    fid = threadInfo?.forum?.fid,
                                    threadTitle = title,
                                    imageList = firstPostImages,
                                    loadHistory = true,
                                )
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
