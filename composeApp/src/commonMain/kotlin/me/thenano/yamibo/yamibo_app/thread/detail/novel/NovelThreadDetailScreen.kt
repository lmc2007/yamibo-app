package me.thenano.yamibo.yamibo_app.thread.detail.novel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.*
import me.thenano.yamibo.yamibo_app.favorite.*
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.*
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.shareText
import me.thenano.yamibo.yamibo_app.util.time.epochMillisOrNull

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
    val appSettingsRepo = LocalAppSettingsRepository.current
    val threadRepository = LocalThreadRepository.current
    val favoriteRepository = LocalFavoriteRepository.current
    val favoriteSyncRepository = LocalFavoriteSyncRepository.current
    val readHistoryRepo = LocalReadHistoryRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val platformContext = LocalPlatformContext.current

    var state by remember { mutableStateOf<ThreadState>(ThreadState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pagePostsCache = remember { mutableStateMapOf<Int, List<Post>>() }
    var expandedPages by remember { mutableStateOf(setOf(1)) }
    var readHistory by remember { mutableStateOf<ThreadReadingHistory?>(null) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var favoriteDialogCategorySelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogCategories by remember {
        mutableStateOf<List<me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCategory>>(emptyList())
    }
    var favoriteDialogOptions by remember {
        mutableStateOf<List<me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCollectionOption>>(emptyList())
    }
    var isFavorited by remember { mutableStateOf(false) }
    var favoritePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var favoriteRefreshToken by remember { mutableStateOf(0) }
    var pendingFavoriteRemovalSelection by remember { mutableStateOf<FavoriteLocationSelection?>(null) }
    var pendingFavoriteRemovalSuccessMessage by remember { mutableStateOf("已移除收藏") }
    var showFavoriteRemovalConfirm by remember { mutableStateOf(false) }
    var showFavoriteMultiPathDialog by remember { mutableStateOf(false) }
    var showFavoriteAddSyncConfirm by remember { mutableStateOf(false) }
    var showFavoriteRemoveSyncConfirm by remember { mutableStateOf(false) }

    /** Load reading history for this thread */
    LaunchedEffect(tid) {
        try {
            readHistory = readHistoryRepo.getPosition(tid, ReadHistoryRepository.ThreadEntryType.Novel, authorId)
        } catch (_: Exception) { }
    }

    fun favoriteTarget(): FavoriteTargetPayload.Thread {
        val currentTitle = (state as? ThreadState.Success)?.page?.thread?.title ?: title
        val currentForumId = (state as? ThreadState.Success)?.page?.thread?.forum?.fid
        val currentForumName = (state as? ThreadState.Success)?.page?.thread?.forum?.name
        val firstPost = (state as? ThreadState.Success)?.page?.posts?.firstOrNull { it.floor == 1 }
        return FavoriteTargetPayload.Thread(
            tid = tid,
            title = currentTitle,
            threadType = ReadHistoryRepository.ThreadEntryType.Novel,
            authorId = authorId,
            coverUrl = readHistory?.threadCover,
            lastUpdatedTime = firstPost?.lastEditedTime?.epochMillisOrNull() ?: firstPost?.timeCreate?.epochMillisOrNull(),
            forumId = currentForumId,
            forumName = currentForumName
        )
    }

    suspend fun refreshFavoriteState() {
        val target = favoriteTarget()
        favoriteRepository.syncFavoriteMetadata(target)
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        isFavorited = selection.item != null
        favoritePaths = selection.paths
    }

    suspend fun completeFavoriteAdd(syncToRemote: Boolean) {
        completeFavoriteAddWithFeedback(
            favoriteRepository = favoriteRepository,
            favoriteSyncRepository = favoriteSyncRepository,
            target = favoriteTarget(),
            syncToRemote = syncToRemote,
            snackbarHostState = snackbarHostState,
            onRefreshRequested = { favoriteRefreshToken += 1 },
        )
    }

    suspend fun completeSavedFavoriteSync(syncToRemote: Boolean) {
        completeSavedFavoriteSyncWithFeedback(
            favoriteRepository = favoriteRepository,
            favoriteSyncRepository = favoriteSyncRepository,
            target = favoriteTarget(),
            syncToRemote = syncToRemote,
            scope = scope,
            snackbarHostState = snackbarHostState,
            onRefreshRequested = { favoriteRefreshToken += 1 },
        )
    }

    suspend fun completeFavoriteRemoval(removeRemote: Boolean) {
        completeFavoriteRemovalWithFeedback(
            favoriteRepository = favoriteRepository,
            favoriteSyncRepository = favoriteSyncRepository,
            target = favoriteTarget(),
            removeRemote = removeRemote,
            scope = scope,
            snackbarHostState = snackbarHostState,
            successMessage = pendingFavoriteRemovalSuccessMessage,
            failureMessage = "移除收藏失敗",
            onRefreshRequested = { favoriteRefreshToken += 1 },
        )
        pendingFavoriteRemovalSelection = null
    }

    suspend fun maybePromptRemoteRemoval() {
        val target = favoriteTarget()
        val shouldPromptRemote = target.supportsRemoteWebsiteSync() &&
            hasRemoteFavoriteForTarget(favoriteRepository, favoriteSyncRepository, target)
        when {
            shouldPromptRemote && appSettingsRepo.favoriteRemoveSyncPromptEnabled.getValue() -> {
                showFavoriteRemoveSyncConfirm = true
            }
            else -> {
                completeFavoriteRemoval(
                    removeRemote = shouldPromptRemote && appSettingsRepo.favoriteRemoveSyncDefault.getValue(),
                )
            }
        }
    }

    suspend fun toggleFavorite() {
        val target = favoriteTarget()
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        if (selection.item != null) {
            pendingFavoriteRemovalSelection = selection
            pendingFavoriteRemovalSuccessMessage = "已移除收藏"
            if (appSettingsRepo.skipFavoriteRemovalConfirm.getValue()) {
                if (selection.paths.size > 1) {
                    showFavoriteMultiPathDialog = true
                } else {
                    maybePromptRemoteRemoval()
                }
            } else {
                showFavoriteRemovalConfirm = true
            }
            return
        }

        if (target.supportsRemoteWebsiteSync() && appSettingsRepo.favoriteAddSyncPromptEnabled.getValue()) {
            favoriteRepository.saveFavorite(target)
            favoriteRefreshToken += 1
            showFavoriteAddSyncConfirm = true
        } else {
            completeFavoriteAdd(
                syncToRemote = target.supportsRemoteWebsiteSync() && appSettingsRepo.favoriteAddSyncDefault.getValue(),
            )
        }
    }

    LaunchedEffect(tid, authorId, favoriteRefreshToken, (state as? ThreadState.Success)?.page?.thread?.title) {
        refreshFavoriteState()
    }

    suspend fun loadThread(page: Int = 1) {
        val cached = threadRepository.getCachedThread(tid, authorId, page)
        if (cached != null) {
            pagePostsCache[page] = cached.posts
            state = ThreadState.Success(cached)
            return
        }

        val result = threadRepository.fetchThread(tid, authorId, page)
        state =
            when (result) {
                is YamiboResult.Success -> {
                    pagePostsCache[page] = result.value.posts
                    ThreadState.Success(result.value)
                }
                else -> ThreadState.Error(result.message())
            }
    }

    /** initial load ??use cache if available, only fetch on cold start */
    LaunchedEffect(tid) {
        val cached = threadRepository.getCachedThread(tid, authorId)
        if (cached != null) {
            pagePostsCache[1] = cached.posts
            state = ThreadState.Success(cached)
            return@LaunchedEffect
        }
        loadThread()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = {
            YamiboSnackbarHost(hostState = snackbarHostState)
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
                            scope.launch {
                                when (val result =
                                    threadRepository.fetchThread(tid, authorId)
                                ) {
                                    is YamiboResult.Success -> {
                                        threadRepository.clearCachedThread(tid)
                                        threadRepository.setCachedThread(
                                            tid,
                                            authorId,
                                            1,
                                            result.value
                                        )
                                        pagePostsCache.clear() // Clear local cache as well
                                        pagePostsCache[1] = result.value.posts
                                        expandedPages = setOf(1)
                                        state = ThreadState.Success(result.value)
                                    }

                                    else -> {
                                        snackbarHostState.showSnackbar(
                                            message = "重新整理帖子失敗：${result.message()}",
                                            duration = SnackbarDuration.Short,
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
                                    val cached = threadRepository.getCachedThread(tid, authorId, page)
                                    if (cached != null) {
                                        pagePostsCache[page] = cached.posts
                                    } else {
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
                                                message = "載入第 $page 頁失敗: ${result.message()}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                }
                            },
                            onPostClick = { page, post ->
                                navigator.navigate(
                                    IThreadReaderScreen(
                                        tid = tid,
                                        title = current.page.thread.title,
                                        threadType = ReadHistoryRepository.ThreadEntryType.Novel,
                                        authorId = authorId,
                                        initialPage = page,
                                        targetPid = post.pid
                                    )
                                )
                            },
                            onFavorite = { scope.launch { toggleFavorite() } },
                            onFavoriteLongPress = {
                                scope.launch {
                                    val selection = favoriteRepository.getFavoriteLocationSelection(favoriteTarget())
                                    favoriteDialogCategories = favoriteRepository.getCategories()
                                    favoriteDialogOptions = favoriteRepository.getCollectionOptions()
                                    favoriteDialogCategorySelection = selection.categoryIds
                                    favoriteDialogSelection = selection.collectionIds
                                    showFavoriteDialog = true
                                }
                            },
                            onContinueRead = {
                                val history = readHistory
                                if (history != null) {
                                    navigator.navigate(
                                        IThreadReaderScreen(
                                            tid = tid,
                                            title = current.page.thread.title,
                                            threadType = ReadHistoryRepository.ThreadEntryType.Novel,
                                            authorId = authorId,
                                            initialPage = history.page
                                        )
                                    )
                                } else {
                                    navigator.navigate(
                                        IThreadReaderScreen(
                                            tid = tid,
                                            title = current.page.thread.title,
                                            threadType = ReadHistoryRepository.ThreadEntryType.Novel,
                                            authorId = authorId,
                                            initialPage = 1
                                        )
                                    )
                                }
                            },
                            readingProgressText = readHistory?.let { h ->
                                buildString {
                                    append("第 ${h.page} 頁")
                                    if (h.postTitle.isNotEmpty()) {
                                        append(" ‧ ${h.postTitle}")
                                    }
                                }
                            },
                            isFavorited = isFavorited,
                            snackbarHostState = snackbarHostState,
                            scope = scope,
                            platformContext = platformContext
                        )
                    }
            }
        }
    }

if (showFavoriteDialog) {
    val target = FavoriteTargetPayload.Thread(
        tid = tid,
        title = (state as? ThreadState.Success)?.page?.thread?.title ?: title,
        threadType = ReadHistoryRepository.ThreadEntryType.Novel,
        authorId = authorId,
        coverUrl = readHistory?.threadCover,
        lastUpdatedTime = (state as? ThreadState.Success)?.page?.posts?.firstOrNull { it.floor == 1 }?.let {
            it.lastEditedTime?.epochMillisOrNull() ?: it.timeCreate.epochMillisOrNull()
        },
        forumId = (state as? ThreadState.Success)?.page?.thread?.forum?.fid,
        forumName = (state as? ThreadState.Success)?.page?.thread?.forum?.name
    )
    FavoriteCollectionPickerDialog(
        categories = favoriteDialogCategories,
        options = favoriteDialogOptions,
        initialCategorySelection = favoriteDialogCategorySelection,
        initialCollectionSelection = favoriteDialogSelection,
        onDismiss = { showFavoriteDialog = false },
        onEdit = {
            showFavoriteDialog = false
            navigator.navigate(IFavoriteCategoryManageScreen())
        },
        onConfirm = { selectedCategories, selectedCollections ->
            scope.launch {
                val existing = favoriteRepository.findFavoriteItem(target)
                if (existing == null) {
                    favoriteRepository.saveFavorite(
                        target,
                        categoryIds = selectedCategories.toList(),
                        collectionIds = selectedCollections.toList()
                    )
                    showFavoriteDialog = false
                    favoriteRefreshToken += 1
                    if (target.supportsRemoteWebsiteSync() && appSettingsRepo.favoriteAddSyncPromptEnabled.getValue()) {
                        showFavoriteAddSyncConfirm = true
                    } else {
                        completeSavedFavoriteSync(
                            syncToRemote = target.supportsRemoteWebsiteSync() && appSettingsRepo.favoriteAddSyncDefault.getValue(),
                        )
                    }
                } else if (selectedCategories.isEmpty() && selectedCollections.isEmpty()) {
                    showFavoriteDialog = false
                    pendingFavoriteRemovalSelection = favoriteRepository.getFavoriteLocationSelection(target)
                    pendingFavoriteRemovalSuccessMessage = "已從所有位置移除收藏"
                    if (appSettingsRepo.skipFavoriteRemovalConfirm.getValue()) {
                        if ((pendingFavoriteRemovalSelection?.paths?.size ?: 0) > 1) {
                            showFavoriteMultiPathDialog = true
                        } else {
                            maybePromptRemoteRemoval()
                        }
                    } else {
                        showFavoriteRemovalConfirm = true
                    }
                } else {
                    favoriteRepository.setItemLocations(existing.id, selectedCategories, selectedCollections)
                    showFavoriteDialog = false
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("收藏位置已更新")
                }
            }
        }
    )
}

if (showFavoriteRemovalConfirm) {
    FavoriteRemovalConfirmDialog(
        onDismiss = {
            showFavoriteRemovalConfirm = false
            pendingFavoriteRemovalSelection = null
        },
        onConfirm = { skipNextTime ->
            appSettingsRepo.skipFavoriteRemovalConfirm.setValue(skipNextTime)
            showFavoriteRemovalConfirm = false
            scope.launch {
                val selection = pendingFavoriteRemovalSelection
                if ((selection?.paths?.size ?: 0) > 1) {
                    showFavoriteMultiPathDialog = true
                } else {
                    pendingFavoriteRemovalSuccessMessage = "已移除收藏"
                    maybePromptRemoteRemoval()
                }
            }
        },
    )
}

if (showFavoriteAddSyncConfirm) {
    FavoriteAddSyncConfirmDialog(
        onDismiss = {
            showFavoriteAddSyncConfirm = false
            scope.launch { completeSavedFavoriteSync(syncToRemote = false) }
        },
        onConfirm = { rememberChoice, syncRemote ->
            showFavoriteAddSyncConfirm = false
            if (rememberChoice) {
                appSettingsRepo.favoriteAddSyncPromptEnabled.setValue(false)
                appSettingsRepo.favoriteAddSyncDefault.setValue(syncRemote)
            }
            scope.launch { completeSavedFavoriteSync(syncRemote) }
        },
    )
}

if (showFavoriteRemoveSyncConfirm) {
    FavoriteRemoveSyncConfirmDialog(
        onDismiss = {
            showFavoriteRemoveSyncConfirm = false
            pendingFavoriteRemovalSelection = null
        },
        onConfirm = { rememberChoice, syncRemote ->
            showFavoriteRemoveSyncConfirm = false
            if (rememberChoice) {
                appSettingsRepo.favoriteRemoveSyncPromptEnabled.setValue(false)
                appSettingsRepo.favoriteRemoveSyncDefault.setValue(syncRemote)
            }
            scope.launch { completeFavoriteRemoval(syncRemote) }
        },
    )
}

if (showFavoriteMultiPathDialog) {
    FavoriteMultiPathRemoveDialog(
        paths = pendingFavoriteRemovalSelection?.paths.orEmpty(),
        tip = "tip：長按可詳細編輯收藏路徑",
        onDismiss = {
            showFavoriteMultiPathDialog = false
            pendingFavoriteRemovalSelection = null
        },
        onRemoveAll = {
            showFavoriteMultiPathDialog = false
            pendingFavoriteRemovalSuccessMessage = "已從所有位置移除收藏"
            scope.launch {
                maybePromptRemoteRemoval()
            }
        },
    )
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
    onPostClick: (Int, Post) -> Unit,
    onFavorite: () -> Unit,
    onFavoriteLongPress: () -> Unit,
    isFavorited: Boolean,
    onContinueRead: () -> Unit,
    readingProgressText: String?,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    platformContext: PlatformContext
) {
    val colors = YamiboTheme.colors
    val thread = threadPage.thread
    val firstPost = threadPage.posts.firstOrNull()
    val totalPages = threadPage.pageNav?.totalPages ?: 1

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        /** Thread Header */
        item {
            ThreadHeader(
                threadPage = threadPage,
                isFavorited = isFavorited,
                onFavorite = onFavorite,
                onFavoriteLongPress = onFavoriteLongPress,
                onShare = {
                    val url = YamiboRoute.Thread(thread.tid).build()
                    shareText(platformContext, url, thread.title)
                },
                onContinueRead = onContinueRead,
                readingProgressText = readingProgressText,
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
                },
                onPostClick = { post -> onPostClick(page, post) }
            )
        }
    }
}
