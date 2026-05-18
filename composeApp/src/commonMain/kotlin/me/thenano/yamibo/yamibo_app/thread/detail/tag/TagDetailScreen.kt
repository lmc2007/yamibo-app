package me.thenano.yamibo.yamibo_app.thread.detail.tag

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import me.thenano.yamibo.yamibo_app.LocalBookMarkRepository
import me.thenano.yamibo.yamibo_app.LocalDetailNoteRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteSyncRepository
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
import me.thenano.yamibo.yamibo_app.favorite.removeFavoriteWithSync
import me.thenano.yamibo.yamibo_app.favorite.saveFavorite
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.LocalBookMarkRepository as BookMarkRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.DetailNoteRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.components.DetailNoteEditorDialog
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
    val favoriteSyncRepository = LocalFavoriteSyncRepository.current
    val detailNoteRepository = LocalDetailNoteRepository.current
    val bookMarkRepository = LocalBookMarkRepository.current
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
    var detailNote by remember { mutableStateOf<DetailNoteRepository.DetailNote?>(null) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var threadBookMarkEntries by remember { mutableStateOf<Map<Long, BookMarkRepository.Entry>>(emptyMap()) }
    var actionThread by remember { mutableStateOf<ThreadSummary?>(null) }

    val appSettingsRepo = LocalAppSettingsRepository.current
    val isMangaMode = appSettingsRepo.isMangaMode.state()

    suspend fun reloadThreadBookMarks() {
        threadBookMarkEntries = bookMarkRepository
            .getEntriesByParent(BookMarkRepository.TargetType.TagMangaThread, tagId.value.toLong())
            .associateBy { it.targetId }
    }

    suspend fun reloadNote() {
        detailNote = detailNoteRepository.getNote(
            targetType = DetailNoteRepository.TargetType.TagManga,
            targetId = tagId.value.toLong(),
        )
    }

    LaunchedEffect(tagId) {
        reloadNote()
    }

    LaunchedEffect(tagId) {
        reloadThreadBookMarks()
    }

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

                else -> TagDetailState.Error(result.localizedMessage())
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
                    withContext(Dispatchers.Default) { removeFavoriteWithSync(favoriteRepository, favoriteSyncRepository, target) }
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_removed))
                    pendingFavoriteRemovalSelection = null
                }
            } else {
                showFavoriteRemovalConfirm = true
            }
            return
        }

        favoriteRepository.saveFavorite(target)
        favoriteRefreshToken += 1
        snackbarHostState.showSnackbar(appString(Res.string.ui_added_favorites))
    }

    LaunchedEffect(tagId, currentTagName, favoriteRefreshToken) {
        refreshFavoriteState()
    }
    
    // Reading progress text
    val readingProgressText = remember(mangaTagHistory, isMangaMode) {
        val h = mangaTagHistory ?: return@remember null
        if (isMangaMode) {
            appString(Res.string.tag_read_progress_with_image, h.tagPage, h.threadTitle, h.threadImagePageIndex + 1, h.threadImageTotalPages)
        } else {
            appString(Res.string.tag_read_progress, h.tagPage, h.threadTitle)
        }
    }

    /** Handle thread click: manga mode opens TagMangaReaderScreen */
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
                    title = thread.title,
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
            navigator.navigate(
                IThreadReaderScreen(
                    tid = history.threadId,
                    title = history.threadTitle,
                    threadType = ReadHistoryRepository.ThreadEntryType.Normal
                )
            )
        } else {
            val first = threads.firstOrNull() ?: return
            navigateToThread(first, threads, pageNav)
        }
    }

    Scaffold(
        containerColor = colors.creamBackground,
        snackbarHost = {
            YamiboSnackbarHost(hostState = snackbarHostState)
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
                                        snackbarHostState.showSnackbar(
                                            message = appString(Res.string.tag_refresh_failed, result.localizedMessage()),
                                            duration = SnackbarDuration.Short,
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
                            noteContent = detailNote?.content.orEmpty(),
                            onNoteClick = { showNoteDialog = true },
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
                            },
                            bookmarkedThreadIds = threadBookMarkEntries.values
                                .filter { it.bookmarked }
                                .mapTo(mutableSetOf()) { it.targetId },
                            readThreadIds = threadBookMarkEntries.values
                                .filter { it.read }
                                .mapTo(mutableSetOf()) { it.targetId },
                            onThreadLongPress = { actionThread = it },
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
                        showFavoriteDialog = false
                        favoriteRefreshToken += 1
                        snackbarHostState.showSnackbar(appString(Res.string.ui_added_favorites))
                    } else if (selectedCategories.isEmpty() && selectedCollections.isEmpty()) {
                        showFavoriteDialog = false
                        pendingFavoriteRemovalSelection = favoriteRepository.getFavoriteLocationSelection(target)
                        if (appSettingsRepo.skipFavoriteRemovalConfirm.getValue()) {
                            if ((pendingFavoriteRemovalSelection?.paths?.size ?: 0) > 1) {
                                showFavoriteMultiPathDialog = true
                            } else {
                                withContext(Dispatchers.Default) { removeFavoriteWithSync(favoriteRepository, favoriteSyncRepository, target) }
                                favoriteRefreshToken += 1
                                snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_removed_from_all_locations))
                                pendingFavoriteRemovalSelection = null
                            }
                        } else {
                            showFavoriteRemovalConfirm = true
                        }
                    } else {
                        favoriteRepository.setItemLocations(existing.id, selectedCategories, selectedCollections)
                        showFavoriteDialog = false
                        favoriteRefreshToken += 1
                        snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_location_updated))
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
                        withContext(Dispatchers.Default) { removeFavoriteWithSync(favoriteRepository, favoriteSyncRepository, favoriteTarget()) }
                        favoriteRefreshToken += 1
                        snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_removed))
                        pendingFavoriteRemovalSelection = null
                    }
                }
            },
        )
    }

    if (showFavoriteMultiPathDialog) {
        FavoriteMultiPathRemoveDialog(
            paths = pendingFavoriteRemovalSelection?.paths.orEmpty(),
            tip = appString(Res.string.ui_tip_long_press_edit_favorite_path_in_detail),
            onDismiss = {
                showFavoriteMultiPathDialog = false
                pendingFavoriteRemovalSelection = null
            },
            onRemoveAll = {
                showFavoriteMultiPathDialog = false
                scope.launch {
                        withContext(Dispatchers.Default) { removeFavoriteWithSync(favoriteRepository, favoriteSyncRepository, favoriteTarget()) }
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar(appString(Res.string.ui_favorite_removed_from_all_locations))
                    pendingFavoriteRemovalSelection = null
                }
            },
        )
    }

    if (showNoteDialog) {
        DetailNoteEditorDialog(
            initialContent = detailNote?.content.orEmpty(),
            onDismiss = { showNoteDialog = false },
            onSave = { content ->
                showNoteDialog = false
                scope.launch {
                    detailNoteRepository.saveNote(
                        targetType = DetailNoteRepository.TargetType.TagManga,
                        targetId = tagId.value.toLong(),
                        content = content,
                    )
                    reloadNote()
                    snackbarHostState.showSnackbar(
                        if (content.isBlank()) appString(Res.string.ui_note_deleted) else appString(Res.string.ui_note_saved),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            onDelete = {
                showNoteDialog = false
                scope.launch {
                    detailNoteRepository.deleteNote(
                        targetType = DetailNoteRepository.TargetType.TagManga,
                        targetId = tagId.value.toLong(),
                    )
                    reloadNote()
                    snackbarHostState.showSnackbar(appString(Res.string.ui_note_deleted), duration = SnackbarDuration.Short)
                }
            },
        )
    }

    actionThread?.let { thread ->
        val entry = threadBookMarkEntries[thread.tid.value.toLong()]
        BookMarkActionDialog(
            bookmarked = entry?.bookmarked == true,
            read = entry?.read == true,
            onDismiss = { actionThread = null },
            onToggleBookMark = {
                actionThread = null
                scope.launch {
                    val next = entry?.bookmarked != true
                    bookMarkRepository.setBookmarked(
                        targetType = BookMarkRepository.TargetType.TagMangaThread,
                        parentId = tagId.value.toLong(),
                        targetId = thread.tid.value.toLong(),
                        title = thread.title,
                        bookmarked = next,
                    )
                    reloadThreadBookMarks()
                    snackbarHostState.showSnackbar(
                        if (next) appString(Res.string.ui_bookmark_added) else appString(Res.string.ui_bookmark_removed),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            onToggleRead = {
                actionThread = null
                scope.launch {
                    val next = entry?.read != true
                    bookMarkRepository.setRead(
                        targetType = BookMarkRepository.TargetType.TagMangaThread,
                        parentId = tagId.value.toLong(),
                        targetId = thread.tid.value.toLong(),
                        title = thread.title,
                        read = next,
                    )
                    reloadThreadBookMarks()
                    snackbarHostState.showSnackbar(
                        if (next) appString(Res.string.ui_marked_as_read) else appString(Res.string.ui_marked_as_unread),
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            onClearHistory = {
                actionThread = null
                scope.launch {
                    bookMarkRepository.setRead(
                        targetType = BookMarkRepository.TargetType.TagMangaThread,
                        parentId = tagId.value.toLong(),
                        targetId = thread.tid.value.toLong(),
                        title = thread.title,
                        read = false,
                    )
                    if (mangaTagHistory?.threadId == thread.tid) {
                        mangaTagHistory = null
                    }
                    reloadThreadBookMarks()
                    snackbarHostState.showSnackbar(appString(Res.string.ui_reading_history_cleared), duration = SnackbarDuration.Short)
                }
            },
        )
    }
}

@Composable
private fun BookMarkActionDialog(
    bookmarked: Boolean,
    read: Boolean,
    onDismiss: () -> Unit,
    onToggleBookMark: () -> Unit,
    onToggleRead: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appString(Res.string.ui_read_mark), color = colors.brownDeep) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                YamiboActionRow(if (bookmarked) appString(Res.string.ui_remove_bookmark) else appString(Res.string.ui_add_bookmark), onToggleBookMark)
                YamiboActionRow(if (read) appString(Res.string.ui_mark_as_unread) else appString(Res.string.ui_mark_as_read), onToggleRead)
                YamiboActionRow(appString(Res.string.ui_clear_reading_history), onClearHistory)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(appString(Res.string.common_cancel), color = colors.brownDeep) }
        },
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun YamiboActionRow(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.creamBackground,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            color = colors.textDark,
        )
    }
}




