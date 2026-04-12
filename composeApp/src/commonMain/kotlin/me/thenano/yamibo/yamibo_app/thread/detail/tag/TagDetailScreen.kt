package me.thenano.yamibo.yamibo_app.thread.detail.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.TagPage
import io.github.littlesurvival.dto.value.TagId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.LocalTagRepository
import me.thenano.yamibo.yamibo_app.favorite.FavoriteCollectionPickerDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteLocationSelection
import me.thenano.yamibo.yamibo_app.favorite.FavoriteMultiPathRemoveDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteRemovalConfirmDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteTargetPayload
import me.thenano.yamibo.yamibo_app.favorite.IFavoriteCategoryManageScreen
import me.thenano.yamibo.yamibo_app.favorite.findFavoriteItem
import me.thenano.yamibo.yamibo_app.favorite.getFavoriteLocationSelection
import me.thenano.yamibo.yamibo_app.favorite.removeFavorite
import me.thenano.yamibo.yamibo_app.favorite.saveFavorite
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.novel.components.ThreadErrorContent
import me.thenano.yamibo.yamibo_app.thread.detail.tag.components.TagDetailContent
import me.thenano.yamibo.yamibo_app.thread.detail.tag.components.TagDetailTopBar
import me.thenano.yamibo.yamibo_app.thread.detail.tag.components.TagLoadingSkeleton
import me.thenano.yamibo.yamibo_app.thread.reader.IImageReaderScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.shareText

/** Tag page state */
internal sealed interface TagDetailState {
    data object Loading : TagDetailState
    data class Success(val page: TagPage) : TagDetailState
    data class Error(val message: String) : TagDetailState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagDetailScreen(
    tagId: TagId,
    tagName: String,
    initialPage: Int? = null
) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val tagRepository = LocalTagRepository.current
    val favoriteRepository = LocalFavoriteRepository.current
    val historyRepo = LocalReadHistoryRepository.current
    val platformContext = LocalPlatformContext.current

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<TagDetailState>(TagDetailState.Loading) }
    var currentPage by remember { mutableIntStateOf(initialPage ?: 1) }
    var currentTagName by remember { mutableStateOf(tagName) }
    var isRefreshing by remember { mutableStateOf(false) }
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
    var showFavoriteRemovalConfirm by remember { mutableStateOf(false) }
    var showFavoriteMultiPathDialog by remember { mutableStateOf(false) }

    val appSettingsRepo = LocalAppSettingsRepository.current
    val isMangaMode = appSettingsRepo.isMangaMode.state()

    // Reading history
    var mangaTagHistory by remember {
        mutableStateOf<ReadHistoryRepository.TagMangaReadingHistory?>(null)
    }
    val coverUrl: () -> String? = { mangaTagHistory?.coverUrl }

    suspend fun loadPage(page: Int) {
        val cached = tagRepository.getCachedTagPage(tagId, page)
        if (cached != null) {
            currentPage = cached.pageNav?.currentPage ?: page
            currentTagName = cached.tagName
            state = TagDetailState.Success(cached)
            return
        }

        val result = tagRepository.fetchTagPage(tagId, page)
        state =
            when (result) {
                is YamiboResult.Success -> {
                    // Force the currentPage to be the requested page
                    val correctedPageNav = result.value.pageNav?.copy(currentPage = page)
                    val correctedTagPage = result.value.copy(pageNav = correctedPageNav)
                    
                    currentPage = page
                    currentTagName = result.value.tagName
                    TagDetailState.Success(correctedTagPage)
                }

                else -> TagDetailState.Error(result.message())
            }
    }

    LaunchedEffect(tagId) {
        if (initialPage != null) {
            currentPage = initialPage
            loadPage(initialPage)
            return@LaunchedEffect
        }
        
        val cached = tagRepository.getCachedTagPage(tagId)
        if (cached != null) {
            currentPage = cached.pageNav?.currentPage ?: 1
            currentTagName = cached.tagName
            state = TagDetailState.Success(cached)
            return@LaunchedEffect
        }
        loadPage(1)
    }

    val stackSize = navigator.stack.size
    // Load reading history for manga tags (hot reload on resume/back navigation)
    LaunchedEffect(tagId, stackSize) {
        mangaTagHistory = historyRepo.getTagMangaReaderModeHistoryPosition(tagId)
    }

    fun favoriteTarget(): FavoriteTargetPayload.TagManga {
        return FavoriteTargetPayload.TagManga(
            tagId = tagId,
            tagName = currentTagName,
            coverUrl = coverUrl()
        )
    }

    suspend fun refreshFavoriteState() {
        val selection = favoriteRepository.getFavoriteLocationSelection(favoriteTarget())
        isFavorited = selection.item != null
        favoritePaths = selection.paths
    }

    suspend fun toggleFavorite() {
        val target = favoriteTarget()
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        if (selection.item != null) {
            pendingFavoriteRemovalSelection = selection
            if (appSettingsRepo.skipFavoriteRemovalConfirm.getValue()) {
                if (selection.paths.size > 1) {
                    showFavoriteMultiPathDialog = true
                } else {
                    withContext(Dispatchers.Default) { favoriteRepository.removeFavorite(target) }
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("已取消收藏")
                    pendingFavoriteRemovalSelection = null
                }
            } else {
                showFavoriteRemovalConfirm = true
            }
            return
        }

        favoriteRepository.saveFavorite(target)
        favoriteRefreshToken += 1
        snackbarHostState.showSnackbar("已加入收藏")
    }

    LaunchedEffect(tagId, currentTagName, favoriteRefreshToken) {
        refreshFavoriteState()
    }
    
    // Reading progress text
    val readingProgressText = remember(mangaTagHistory, isMangaMode) {
        val h = mangaTagHistory ?: return@remember null
        if (isMangaMode) {
            "第 ${h.tagPage} 頁 繚 ${h.threadTitle} 繚 ${h.threadImagePageIndex + 1}/${h.threadImageTotalPages}"
        } else {
            "第 ${h.tagPage} 頁 繚 ${h.threadTitle}"
        }
    }
    /** Handle thread click ??manga mode opens TagMangaReaderScreen */
    fun navigateToThread(thread: ThreadSummary, threads: List<ThreadSummary>, pageNav: PageNav?) {
        val fid = thread.fid
        val isManga = fid?.let { YamiboForum.isMangaForum(it) } == true
        val isNovel = fid?.let { YamiboForum.isNovelForum(it) } == true

        if (isMangaMode && isManga) {
            val isLastReadThread = mangaTagHistory?.threadId == thread.tid
            navigator.navigate(
                IImageReaderScreen(
                    tid = thread.tid,
                    postId = null,
                    fid = fid,
                    threadTitle = thread.title,
                    authorId = thread.author?.uid,
                    imageList = emptyList(),
                    initialPage = 1,
                    loadHistory = isLastReadThread,
                    tagId = tagId,
                    tagName = currentTagName,
                    tagThreads = threads,
                    tagPage = currentPage,
                    tagTotalPages = pageNav?.totalPages ?: 1
                )
            )
        } else if (isNovel) {
            navigator.navigate(
                INovelThreadDetailScreen(
                    tid = thread.tid,
                    title = thread.title
                    ,
                    authorId = thread.author?.uid
                )
            )
        } else {
            navigator.navigate(
                IThreadReaderScreen(
                    tid = thread.tid,
                    title = thread.title,
                    threadType = ReadHistoryRepository.ThreadEntryType.Normal
                )
            )
        }
    }

    /** Handle continue reading / start reading */
    fun handleContinueRead(threads: List<ThreadSummary>, pageNav: PageNav?) {
        val history = mangaTagHistory
        if (history != null && isMangaMode) {
            // ?嚙賣風?嚙踝蕭???+ 瞍怎璅∴蕭?嚗脣 TagMangaReaderScreen ?嚙賣嚙?嚙踝蕭嚙?ImagesReaderScreen
            val threadIndex = threads.indexOfFirst { it.tid == history.threadId }
            val authorId = if (threadIndex >= 0) threads[threadIndex].author?.uid else null
            navigator.navigate(
                IImageReaderScreen(
                    tid = history.threadId,
                    postId = null,
                    fid = null,
                    threadTitle = history.threadTitle,
                    authorId = authorId,
                    imageList = emptyList(),
                    initialPage = 1,
                    loadHistory = true,
                    tagId = tagId,
                    tagName = currentTagName,
                    tagThreads = threads,
                    tagPage = history.tagPage,
                    tagTotalPages = pageNav?.totalPages ?: 1
                )
            )
        } else if (history != null) {
            // ?嚙賣風?嚙踝蕭???+ ?嚙賣憤?嚙賣芋撘蕭??嚙賢?嚙賡霈
            navigator.navigate(
                IThreadReaderScreen(
                    tid = history.threadId,
                    title = history.threadTitle,
                    threadType = ReadHistoryRepository.ThreadEntryType.Normal
                )
            )
        } else {
            // ?嚙賣風?嚙踝蕭??嚙踝蕭??嚙踝蕭?蝚穿蕭???thread
            val first = threads.firstOrNull() ?: return
            navigateToThread(first, threads, pageNav)
        }
    }

    Scaffold(
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
            TagDetailTopBar(
                title = currentTagName,
                onBack = { navigator.pop() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(colors.creamBackground)
        ) {
            when (val currentState = state) {
                is TagDetailState.Loading -> TagLoadingSkeleton()

                is TagDetailState.Error -> {
                    ThreadErrorContent(
                        message = currentState.message,
                        onRetry = {
                            state = TagDetailState.Loading
                            scope.launch { loadPage(currentPage) }
                        }
                    )
                }

                is TagDetailState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            scope.launch {
                                when (val result =
                                    tagRepository.fetchTagPage(tagId, currentPage)
                                ) {
                                    is YamiboResult.Success -> {
                                        tagRepository.clearCachedTagPage(tagId)
                                        tagRepository.setCachedTagPage(
                                            tagId,
                                            currentPage,
                                            result.value
                                        )
                                        currentTagName = result.value.tagName
                                        state = TagDetailState.Success(result.value)
                                    }

                                    else -> {
                                        val msg = result.message()
                                        snackbarHostState.showSnackbar(
                                            message = "?嚙賣憭梧蕭?嚙?msg",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        TagDetailContent(
                            tagPage = currentState.page,
                            tagName = currentTagName,
                            coverUrl = coverUrl(),
                            isMangaMode = isMangaMode,
                            onMangaModeChange = { appSettingsRepo.isMangaMode.setValue(it) },
                            hasReadingHistory = mangaTagHistory != null,
                            readingProgressText = readingProgressText,
                            onContinueRead = {
                                handleContinueRead(currentState.page.threadSummaries, currentState.page.pageNav)
                            },
                            onFavorite = { scope.launch { toggleFavorite() } },
                            onFavoriteLongPress = {
                                scope.launch {
                                    val target = favoriteTarget()
                                    val selection = favoriteRepository.getFavoriteLocationSelection(target)
                                    favoriteDialogCategories = favoriteRepository.getCategories()
                                    favoriteDialogOptions = favoriteRepository.getCollectionOptions()
                                    favoriteDialogCategorySelection = selection.categoryIds
                                    favoriteDialogSelection = selection.collectionIds
                                    showFavoriteDialog = true
                                    return@launch
                                }
                            },
                            onShare = {
                                val url = YamiboRoute.TagPage(tagId).build()
                                shareText(platformContext, url, currentTagName)
                            },
                            onPageChange = { page ->
                                state = TagDetailState.Loading
                                scope.launch { loadPage(page) }
                            },
                            isFavorited = isFavorited,
                            onThreadClick = { thread ->
                                navigateToThread(
                                    thread,
                                    currentState.page.threadSummaries,
                                    currentState.page.pageNav
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFavoriteDialog) {
        val target = FavoriteTargetPayload.TagManga(
            tagId = tagId,
            tagName = currentTagName,
            coverUrl = coverUrl()
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
                    } else {
                        favoriteRepository.setItemLocations(existing.id, selectedCategories, selectedCollections)
                    }
                    showFavoriteDialog = false
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("已更新收藏路徑")
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
                        withContext(Dispatchers.Default) { favoriteRepository.removeFavorite(favoriteTarget()) }
                        favoriteRefreshToken += 1
                        snackbarHostState.showSnackbar("已取消收藏")
                        pendingFavoriteRemovalSelection = null
                    }
                }
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
                scope.launch {
                    withContext(Dispatchers.Default) { favoriteRepository.removeFavorite(favoriteTarget()) }
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("已取消全部收藏")
                    pendingFavoriteRemovalSelection = null
                }
            },
        )
    }
}
