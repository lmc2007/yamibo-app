package me.thenano.yamibo.yamibo_app.history

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.model.PageNav
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.IMainScreen
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteSyncRepository
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.favorite.FavoriteAddSyncConfirmDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteCollectionPickerDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteLocationSelection
import me.thenano.yamibo.yamibo_app.favorite.FavoriteMultiPathRemoveDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteRemovalConfirmDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteRemoveSyncConfirmDialog
import me.thenano.yamibo.yamibo_app.favorite.FavoriteTargetPayload
import me.thenano.yamibo.yamibo_app.favorite.IFavoriteCategoryManageScreen
import me.thenano.yamibo.yamibo_app.favorite.addFavoriteAndMaybeSync
import me.thenano.yamibo.yamibo_app.favorite.findFavoriteItem
import me.thenano.yamibo.yamibo_app.favorite.getFavoriteLocationSelection
import me.thenano.yamibo.yamibo_app.favorite.hasRemoteFavoriteForTarget
import me.thenano.yamibo.yamibo_app.favorite.removeFavoriteWithSync
import me.thenano.yamibo.yamibo_app.favorite.saveFavorite
import me.thenano.yamibo.yamibo_app.favorite.supportsRemoteWebsiteSync
import me.thenano.yamibo.yamibo_app.favorite.syncExistingFavoriteIfRequested
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IImageReaderScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.math.ceil

private const val PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadHistoryPage(reTapToken: Int = 0) {
    val colors = YamiboTheme.colors
    val appSettingsRepository = LocalAppSettingsRepository.current
    val readHistoryRepo = LocalReadHistoryRepository.current
    val favoriteRepository = LocalFavoriteRepository.current
    val favoriteSyncRepository = LocalFavoriteSyncRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<HistoryState>(HistoryState.Loading) }
    var currentPage by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf(PageMode.Normal) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedItems by remember { mutableStateOf(setOf<ReadHistoryRepository.AnyReadingHistory>()) }
    val focusRequester = remember { FocusRequester() }

    var favoriteDialogTarget by remember { mutableStateOf<FavoriteTargetPayload?>(null) }
    var favoriteDialogCategorySelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favoriteDialogCategories by remember {
        mutableStateOf<List<LocalFavoriteRepository.FavoriteCategory>>(emptyList())
    }
    var favoriteCollectionOptions by remember {
        mutableStateOf<List<LocalFavoriteRepository.FavoriteCollectionOption>>(emptyList())
    }
    var favoriteRefreshToken by remember { mutableIntStateOf(0) }
    var pendingFavoriteRemovalTarget by remember { mutableStateOf<FavoriteTargetPayload?>(null) }
    var pendingFavoriteRemovalSelection by remember { mutableStateOf<FavoriteLocationSelection?>(null) }
    var pendingFavoriteRemovalSuccessMessage by remember { mutableStateOf("已移除收藏") }
    var showFavoriteRemovalConfirm by remember { mutableStateOf(false) }
    var showFavoriteMultiPathDialog by remember { mutableStateOf(false) }
    var showFavoriteAddSyncConfirm by remember { mutableStateOf(false) }
    var showFavoriteRemoveSyncConfirm by remember { mutableStateOf(false) }

    /** Re-tap History tab → open the topmost item's reader */
    LaunchedEffect(reTapToken) {
        if (reTapToken == 0) return@LaunchedEffect
        val topItem = (state as? HistoryState.Success)?.items?.firstOrNull() ?: return@LaunchedEffect
        when (topItem) {
            is ThreadReadingHistory -> navigator.navigate(
                IThreadReaderScreen(
                    tid = topItem.threadId,
                    title = topItem.threadName,
                    threadType = topItem.threadType,
                    authorId = topItem.authorId,
                    initialPage = topItem.page
                )
            )
            is ReadHistoryRepository.TagMangaReadingHistory -> navigator.navigate(
                IImageReaderScreen(
                    tid = topItem.threadId,
                    postId = null,
                    fid = null,
                    threadTitle = topItem.threadTitle,
                    authorId = null,
                    imageList = emptyList(),
                    tagId = topItem.tagId,
                    tagName = topItem.tagName,
                    tagPage = topItem.tagPage,
                    tagThreads = emptyList(),
                    initialPage = topItem.threadImagePageIndex + 1
                )
            )
            else -> {}
        }
    }

    suspend fun loadPage(page: Int) {
        state = HistoryState.Loading
        try {
            val count = readHistoryRepo.getCombinedHistoryCount()
            if (count == 0L) {
                state = HistoryState.Empty
                return
            }
            state = HistoryState.Success(
                items = readHistoryRepo.getCombinedHistoryPage(page, PAGE_SIZE),
                totalCount = count,
                currentPage = page
            )
            currentPage = page
        } catch (e: Exception) {
            state = HistoryState.Error(e.message ?: "載入失敗")
        }
    }

    suspend fun doSearch(query: String, page: Int = 1) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        state = HistoryState.Loading
        try {
            val count = readHistoryRepo.searchCombinedHistoryCount(trimmed)
            if (count == 0L) {
                state = HistoryState.Empty
                return
            }
            state = HistoryState.Success(
                items = readHistoryRepo.searchCombinedHistory(trimmed, page, PAGE_SIZE),
                totalCount = count,
                currentPage = page
            )
            currentPage = page
        } catch (e: Exception) {
            state = HistoryState.Error(e.message ?: "搜尋失敗")
        }
    }

    fun threadPayload(history: ThreadReadingHistory): FavoriteTargetPayload.Thread {
        return FavoriteTargetPayload.Thread(
            tid = history.threadId,
            title = history.threadName,
            threadType = history.threadType,
            authorId = history.authorId,
            coverUrl = history.threadCover,
            lastUpdatedTime = history.lastUpdatedTime,
            forumId = history.forumId,
            forumName = history.forumName
        )
    }

    suspend fun openFavoriteDialogWithSelection(target: FavoriteTargetPayload) {
        favoriteDialogCategories = favoriteRepository.getCategories()
        favoriteCollectionOptions = favoriteRepository.getCollectionOptions()
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        favoriteDialogCategorySelection = selection.categoryIds
        favoriteDialogSelection = selection.collectionIds
        favoriteDialogTarget = target
    }

    suspend fun completeFavoriteAdd(target: FavoriteTargetPayload, syncToRemote: Boolean) {
        val syncResult = withContext(Dispatchers.Default) {
            addFavoriteAndMaybeSync(favoriteRepository, favoriteSyncRepository, target, syncToRemote)
        }
        favoriteRefreshToken += 1
        val message = when {
            syncResult == null -> "已加入收藏，預設存入未分類"
            syncResult.success -> "已加入收藏，${syncResult.message ?: "已同步到百合會。"}"
            else -> "已加入收藏，但同步失敗：${syncResult.message ?: "請稍後再試"}"
        }
        snackbarHostState.showSnackbar(message)
    }

    suspend fun completeSavedFavoriteSync(target: FavoriteTargetPayload, syncToRemote: Boolean) {
        val syncingSnackbarJob = if (syncToRemote) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "正在同步到百合會...",
                    duration = SnackbarDuration.Indefinite,
                )
            }
        } else {
            null
        }
        val syncResult = withContext(Dispatchers.Default) {
            syncExistingFavoriteIfRequested(favoriteRepository, favoriteSyncRepository, target, syncToRemote)
        }
        syncingSnackbarJob?.cancel()
        snackbarHostState.currentSnackbarData?.dismiss()
        favoriteRefreshToken += 1
        val message = when {
            syncResult == null -> "已加入本地收藏，預設存入未分類"
            syncResult.success -> "已加入本地收藏，${syncResult.message ?: "已同步到百合會。"}"
            else -> "已加入本地收藏，但同步到百合會失敗：${syncResult.message ?: "請稍後再試"}"
        }
        snackbarHostState.showSnackbar(message)
    }

    suspend fun completeFavoriteRemoval(target: FavoriteTargetPayload, removeRemote: Boolean) {
        val syncingSnackbarJob = if (removeRemote) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "正在從百合會移除收藏...",
                    duration = SnackbarDuration.Indefinite,
                )
            }
        } else {
            null
        }
        val removeResult = withContext(Dispatchers.Default) {
            removeFavoriteWithSync(
                favoriteRepository = favoriteRepository,
                favoriteSyncRepository = favoriteSyncRepository,
                target = target,
                removeRemote = removeRemote,
            )
        }
        syncingSnackbarJob?.cancel()
        snackbarHostState.currentSnackbarData?.dismiss()
        favoriteRefreshToken += 1
        snackbarHostState.showSnackbar(
            if (removeResult.success) pendingFavoriteRemovalSuccessMessage else removeResult.message ?: "移除收藏失敗",
        )
        pendingFavoriteRemovalTarget = null
        pendingFavoriteRemovalSelection = null
    }

    suspend fun maybePromptRemoteRemoval(target: FavoriteTargetPayload) {
        val shouldPromptRemote = target.supportsRemoteWebsiteSync() &&
            hasRemoteFavoriteForTarget(favoriteRepository, favoriteSyncRepository, target)
        when {
            shouldPromptRemote && appSettingsRepository.favoriteRemoveSyncPromptEnabled.getValue() -> {
                showFavoriteRemoveSyncConfirm = true
            }
            else -> {
                completeFavoriteRemoval(
                    target = target,
                    removeRemote = shouldPromptRemote && appSettingsRepository.favoriteRemoveSyncDefault.getValue(),
                )
            }
        }
    }

    suspend fun toggleFavoriteQuickWithFeedback(target: FavoriteTargetPayload) {
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        if (selection.item != null) {
            pendingFavoriteRemovalTarget = target
            pendingFavoriteRemovalSelection = selection
            pendingFavoriteRemovalSuccessMessage = "已移除收藏"
            if (appSettingsRepository.skipFavoriteRemovalConfirm.getValue()) {
                if (selection.paths.size > 1) {
                    showFavoriteMultiPathDialog = true
                } else {
                    maybePromptRemoteRemoval(target)
                }
            } else {
                showFavoriteRemovalConfirm = true
            }
            return
        }

        if (target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncPromptEnabled.getValue()) {
            favoriteRepository.saveFavorite(target)
            favoriteRefreshToken += 1
            pendingFavoriteRemovalTarget = target
            showFavoriteAddSyncConfirm = true
        } else {
            completeFavoriteAdd(
                target = target,
                syncToRemote = target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncDefault.getValue(),
            )
        }
    }

    LaunchedEffect(navigator.stack.size) {
        if (navigator.currentScreen is IMainScreen) {
            if (mode == PageMode.Search && searchQuery.isNotBlank()) {
                doSearch(searchQuery, currentPage)
            } else if (mode != PageMode.Select) {
                loadPage(currentPage)
            }
        }
    }

    DisposableEffect(mode, navigator) {
        if (mode == PageMode.Normal) {
            onDispose { }
        } else {
            val handler = {
                when (mode) {
                    PageMode.Search -> {
                        searchQuery = ""
                        mode = PageMode.Normal
                        scope.launch { loadPage(1) }
                        true
                    }

                    PageMode.Select -> {
                        selectedItems = emptySet()
                        mode = PageMode.Normal
                        true
                    }

                    PageMode.Normal -> false
                }
            }
            navigator.backHandlers.add(handler)
            onDispose { navigator.backHandlers.remove(handler) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().statusBarsPadding(),
            color = colors.creamBackground,
            shadowElevation = 2.dp
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "history_top_bar"
            ) { currentMode ->
                when (currentMode) {
                    PageMode.Normal -> NormalTopBar(
                        onSearch = { mode = PageMode.Search },
                        onMultiSelect = {
                            selectedItems = emptySet()
                            mode = PageMode.Select
                        }
                    )

                    PageMode.Search -> SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { scope.launch { doSearch(searchQuery) } },
                        onBack = {
                            searchQuery = ""
                            mode = PageMode.Normal
                            scope.launch { loadPage(1) }
                        },
                        focusRequester = focusRequester
                    )

                    PageMode.Select -> SelectTopBar(
                        onSelectAll = {
                            val current = state as? HistoryState.Success ?: return@SelectTopBar
                            selectedItems = current.items.toSet()
                        },
                        onClearAll = {
                            scope.launch {
                                readHistoryRepo.deleteAllCombinedHistory()
                                selectedItems = emptySet()
                                mode = PageMode.Normal
                                state = HistoryState.Empty
                                snackbarHostState.showSnackbar("已刷新閱讀歷史紀錄")
                            }
                        },
                        onCancel = {
                            selectedItems = emptySet()
                            mode = PageMode.Normal
                        },
                        onDeleteSelected = {
                            if (selectedItems.isNotEmpty()) {
                                scope.launch {
                                    val deletedAmount = selectedItems.size
                                    readHistoryRepo.deleteCombinedHistoryBatch(selectedItems.toList())
                                    selectedItems = emptySet()
                                    mode = PageMode.Normal
                                    loadPage(1)
                                    snackbarHostState.showSnackbar("已刪除 $deletedAmount 項紀錄")
                                }
                            }
                        },
                        selectedCount = selectedItems.size
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val current = state) {
                is HistoryState.Loading -> LoadingContent()
                is HistoryState.Empty -> EmptyContent(mode)
                is HistoryState.Error -> ErrorContent(current.message) { scope.launch { loadPage(currentPage) } }
                is HistoryState.Success -> {
                    val totalPages = ceil(current.totalCount.toDouble() / PAGE_SIZE).toInt()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        groupByDate(current.items).forEach { (dateLabel, entries) ->
                            item(key = "header_$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textDark.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(entries, key = { itemKey(it) }) { history ->
                                when (history) {
                                    is ThreadReadingHistory -> ThreadHistoryItem(
                                        history = history,
                                        pageMode = mode,
                                        selectedItems = selectedItems,
                                        onToggleSelection = {
                                            selectedItems = if (history in selectedItems) selectedItems - history else selectedItems + history
                                        },
                                        onDelete = {
                                            scope.launch {
                                                readHistoryRepo.deleteHistoryBatch(listOf(history))
                                                loadPage(currentPage)
                                                snackbarHostState.showSnackbar("已刪除這筆紀錄")
                                            }
                                        },
                                        onFavorite = { scope.launch { toggleFavoriteQuickWithFeedback(threadPayload(history)) } },
                                        onFavoriteLongPress = { scope.launch { openFavoriteDialogWithSelection(threadPayload(history)) } },
                                        favoriteRefreshToken = favoriteRefreshToken,
                                        navigator = navigator
                                    )
                                    is ReadHistoryRepository.TagMangaReadingHistory -> TagHistoryItem(
                                        history = history,
                                        pageMode = mode,
                                        selectedItems = selectedItems,
                                        onToggleSelection = {
                                            selectedItems = if (history in selectedItems) selectedItems - history else selectedItems + history
                                        },
                                        onDelete = {
                                            scope.launch {
                                                readHistoryRepo.deleteMangaTagHistory(history.tagId)
                                                loadPage(currentPage)
                                                snackbarHostState.showSnackbar("已刪除這筆紀錄")
                                            }
                                        },
                                        onFavorite = {
                                            scope.launch {
                                            toggleFavoriteQuickWithFeedback(
                                                    FavoriteTargetPayload.TagManga(
                                                        tagId = history.tagId,
                                                        tagName = history.tagName,
                                                        coverUrl = history.coverUrl
                                                    )
                                                )
                                            }
                                        },
                                        onFavoriteLongPress = {
                                            scope.launch {
                                                openFavoriteDialogWithSelection(
                                                    FavoriteTargetPayload.TagManga(
                                                        tagId = history.tagId,
                                                        tagName = history.tagName,
                                                        coverUrl = history.coverUrl
                                                    )
                                                )
                                            }
                                        },
                                        favoriteRefreshToken = favoriteRefreshToken,
                                        navigator = navigator
                                    )
                                    else -> {}
                                }
                            }
                        }
                        if (totalPages > 1) {
                            item(key = "pagination") {
                                PageNavigation(
                                    pageNav = PageNav(
                                        currentPage = current.currentPage,
                                        totalPages = totalPages,
                                        prevUrl = if (current.currentPage > 1) "prev" else null,
                                        nextUrl = if (current.currentPage < totalPages) "next" else null
                                    ),
                                    onPageChange = { page ->
                                        scope.launch {
                                            if (mode == PageMode.Search && searchQuery.isNotBlank()) doSearch(searchQuery, page)
                                            else loadPage(page)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            YamiboSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (favoriteDialogTarget != null) {
        FavoriteCollectionPickerDialog(
            categories = favoriteDialogCategories,
            options = favoriteCollectionOptions,
            initialCategorySelection = favoriteDialogCategorySelection,
            initialCollectionSelection = favoriteDialogSelection,
            onDismiss = { favoriteDialogTarget = null },
            onEdit = {
                favoriteDialogTarget = null
                navigator.navigate(IFavoriteCategoryManageScreen())
            },
            onConfirm = { selectedCategories, selectedCollections ->
                scope.launch {
                    val target = favoriteDialogTarget ?: return@launch
                    val existing = favoriteRepository.findFavoriteItem(target)
                    if (existing == null) {
                        favoriteRepository.saveFavorite(
                            target,
                            categoryIds = selectedCategories.toList(),
                            collectionIds = selectedCollections.toList()
                        )
                        favoriteDialogTarget = null
                        favoriteRefreshToken += 1
                        if (target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncPromptEnabled.getValue()) {
                            pendingFavoriteRemovalTarget = target
                            showFavoriteAddSyncConfirm = true
                        } else {
                            completeSavedFavoriteSync(
                                target = target,
                                syncToRemote = target.supportsRemoteWebsiteSync() && appSettingsRepository.favoriteAddSyncDefault.getValue(),
                            )
                        }
                    } else if (selectedCategories.isEmpty() && selectedCollections.isEmpty()) {
                        favoriteDialogTarget = null
                        pendingFavoriteRemovalTarget = target
                        pendingFavoriteRemovalSelection = favoriteRepository.getFavoriteLocationSelection(target)
                        pendingFavoriteRemovalSuccessMessage = "已從所有位置移除收藏"
                        if (appSettingsRepository.skipFavoriteRemovalConfirm.getValue()) {
                            if ((pendingFavoriteRemovalSelection?.paths?.size ?: 0) > 1) {
                                showFavoriteMultiPathDialog = true
                            } else {
                                maybePromptRemoteRemoval(target)
                            }
                        } else {
                            showFavoriteRemovalConfirm = true
                        }
                    } else {
                        favoriteRepository.setItemLocations(existing.id, selectedCategories, selectedCollections)
                        favoriteDialogTarget = null
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
                pendingFavoriteRemovalTarget = null
                pendingFavoriteRemovalSelection = null
            },
            onConfirm = { skipNextTime ->
                appSettingsRepository.skipFavoriteRemovalConfirm.setValue(skipNextTime)
                showFavoriteRemovalConfirm = false
                val target = pendingFavoriteRemovalTarget ?: return@FavoriteRemovalConfirmDialog
                val selection = pendingFavoriteRemovalSelection
                scope.launch {
                    if ((selection?.paths?.size ?: 0) > 1) {
                        showFavoriteMultiPathDialog = true
                    } else {
                        pendingFavoriteRemovalSuccessMessage = "已移除收藏"
                        maybePromptRemoteRemoval(target)
                    }
                }
            },
        )
    }

    if (showFavoriteAddSyncConfirm) {
        FavoriteAddSyncConfirmDialog(
            onDismiss = {
                val target = pendingFavoriteRemovalTarget
                showFavoriteAddSyncConfirm = false
                pendingFavoriteRemovalTarget = null
                if (target != null) {
                    scope.launch { completeSavedFavoriteSync(target, syncToRemote = false) }
                }
            },
            onConfirm = { rememberChoice, syncRemote ->
                val target = pendingFavoriteRemovalTarget ?: return@FavoriteAddSyncConfirmDialog
                showFavoriteAddSyncConfirm = false
                if (rememberChoice) {
                    appSettingsRepository.favoriteAddSyncPromptEnabled.setValue(false)
                    appSettingsRepository.favoriteAddSyncDefault.setValue(syncRemote)
                }
                pendingFavoriteRemovalTarget = null
                scope.launch { completeSavedFavoriteSync(target, syncRemote) }
            },
        )
    }

    if (showFavoriteRemoveSyncConfirm) {
        FavoriteRemoveSyncConfirmDialog(
            onDismiss = {
                showFavoriteRemoveSyncConfirm = false
                pendingFavoriteRemovalTarget = null
                pendingFavoriteRemovalSelection = null
            },
            onConfirm = { rememberChoice, syncRemote ->
                val target = pendingFavoriteRemovalTarget ?: return@FavoriteRemoveSyncConfirmDialog
                showFavoriteRemoveSyncConfirm = false
                if (rememberChoice) {
                    appSettingsRepository.favoriteRemoveSyncPromptEnabled.setValue(false)
                    appSettingsRepository.favoriteRemoveSyncDefault.setValue(syncRemote)
                }
                scope.launch { completeFavoriteRemoval(target, syncRemote) }
            },
        )
    }

    if (showFavoriteMultiPathDialog) {
        FavoriteMultiPathRemoveDialog(
            paths = pendingFavoriteRemovalSelection?.paths.orEmpty(),
            tip = "tip：長按可詳細編輯收藏路徑",
            onDismiss = {
                showFavoriteMultiPathDialog = false
                pendingFavoriteRemovalTarget = null
                pendingFavoriteRemovalSelection = null
            },
            onRemoveAll = {
                val target = pendingFavoriteRemovalTarget ?: return@FavoriteMultiPathRemoveDialog
                showFavoriteMultiPathDialog = false
                pendingFavoriteRemovalSuccessMessage = "已從所有位置移除收藏"
                scope.launch {
                    maybePromptRemoteRemoval(target)
                }
            },
        )
    }
}

