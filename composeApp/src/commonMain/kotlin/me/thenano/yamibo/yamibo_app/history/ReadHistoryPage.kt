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
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
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
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IImageReaderScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.math.ceil

private sealed interface HistoryState {
    data object Loading : HistoryState
    data class Success(
        val items: List<ReadHistoryRepository.AnyReadingHistory>,
        val totalCount: Long,
        val currentPage: Int
    ) : HistoryState

    data object Empty : HistoryState
    data class Error(val message: String) : HistoryState
}

private enum class PageMode { Normal, Search, Select }

private const val PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadHistoryPage() {
    val colors = YamiboTheme.colors
    val appSettingsRepository = LocalAppSettingsRepository.current
    val readHistoryRepo = LocalReadHistoryRepository.current
    val favoriteRepository = LocalFavoriteRepository.current
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
    var showFavoriteRemovalConfirm by remember { mutableStateOf(false) }
    var showFavoriteMultiPathDialog by remember { mutableStateOf(false) }

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
            forumId = history.forumId,
            forumName = history.forumName
        )
    }

    suspend fun saveFavoriteQuick(target: FavoriteTargetPayload) {
        favoriteRepository.saveFavorite(target)
        snackbarHostState.showSnackbar("已加入全部收藏")
    }

    suspend fun openFavoriteDialog(target: FavoriteTargetPayload) {
        favoriteDialogCategories = favoriteRepository.getCategories()
        favoriteCollectionOptions = favoriteRepository.getCollectionOptions()
        favoriteDialogSelection = favoriteRepository.findFavoriteItem(target)
            ?.let { favoriteRepository.getCollectionIdsForItem(it.id) }
            ?: emptySet()
        favoriteDialogTarget = target
    }

    suspend fun saveFavoriteQuickWithFeedback(target: FavoriteTargetPayload) {
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        if (selection.item != null) {
            val pathText = selection.paths.joinToString("、").ifBlank { "既有收藏位置" }
            snackbarHostState.showSnackbar("您已經收藏在 $pathText")
            return
        }

        favoriteRepository.saveFavorite(target)
        favoriteRefreshToken += 1
        snackbarHostState.showSnackbar("已收藏到預設類別")
    }

    suspend fun openFavoriteDialogWithSelection(target: FavoriteTargetPayload) {
        favoriteDialogCategories = favoriteRepository.getCategories()
        favoriteCollectionOptions = favoriteRepository.getCollectionOptions()
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        favoriteDialogCategorySelection = selection.categoryIds
        favoriteDialogSelection = selection.collectionIds
        favoriteDialogTarget = target
    }

    suspend fun toggleFavoriteQuickWithFeedback(target: FavoriteTargetPayload) {
        val selection = favoriteRepository.getFavoriteLocationSelection(target)
        if (selection.item != null) {
            pendingFavoriteRemovalTarget = target
            pendingFavoriteRemovalSelection = selection
            if (appSettingsRepository.skipFavoriteRemovalConfirm.getValue()) {
                if (selection.paths.size > 1) {
                    showFavoriteMultiPathDialog = true
                } else {
                    withContext(Dispatchers.Default) {
                        favoriteRepository.removeFavorite(target)
                    }
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("已取消收藏")
                    pendingFavoriteRemovalTarget = null
                    pendingFavoriteRemovalSelection = null
                }
            } else {
                showFavoriteRemovalConfirm = true
            }
            return
        }

        favoriteRepository.saveFavorite(target)
        favoriteRefreshToken += 1
        snackbarHostState.showSnackbar("已收藏到預設類別")
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
            modifier = Modifier.fillMaxWidth(),
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
                                snackbarHostState.showSnackbar("已清空閱讀紀錄")
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
                                    snackbarHostState.showSnackbar("已刪除 $deletedAmount 筆紀錄")
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
                                                snackbarHostState.showSnackbar("已刪除此筆紀錄")
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
                                                snackbarHostState.showSnackbar("已刪除此筆紀錄")
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

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colors.brownDeep,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
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
                    } else {
                        favoriteRepository.setItemLocations(existing.id, selectedCategories, selectedCollections)
                    }
                    favoriteDialogTarget = null
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("收藏類別已更新")
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
                        withContext(Dispatchers.Default) {
                            favoriteRepository.removeFavorite(target)
                        }
                        favoriteRefreshToken += 1
                        snackbarHostState.showSnackbar("已取消收藏")
                        pendingFavoriteRemovalTarget = null
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
                pendingFavoriteRemovalTarget = null
                pendingFavoriteRemovalSelection = null
            },
            onRemoveAll = {
                val target = pendingFavoriteRemovalTarget ?: return@FavoriteMultiPathRemoveDialog
                showFavoriteMultiPathDialog = false
                scope.launch {
                    withContext(Dispatchers.Default) {
                        favoriteRepository.removeFavorite(target)
                    }
                    favoriteRefreshToken += 1
                    snackbarHostState.showSnackbar("已取消全部收藏")
                    pendingFavoriteRemovalTarget = null
                    pendingFavoriteRemovalSelection = null
                }
            },
        )
    }
}

@Composable
private fun LoadingContent() {
    val colors = YamiboTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.brownPrimary, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun EmptyContent(mode: PageMode) {
    val colors = YamiboTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = YamiboIcons.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = colors.brownPrimary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (mode == PageMode.Search) "沒有找到結果" else "還沒有閱讀紀錄",
            fontSize = 16.sp,
            color = colors.textDark.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (mode == PageMode.Search) "可以試試其他關鍵字" else "開始閱讀後，紀錄會顯示在這裡",
            fontSize = 13.sp,
            color = colors.textDark.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    val colors = YamiboTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "讀取失敗：$message",
            fontSize = 14.sp,
            color = colors.textDark.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = colors.brownPrimary)
        ) {
            Text("重新整理", color = Color.White)
        }
    }
}

@Composable
private fun ThreadHistoryItem(
    history: ThreadReadingHistory,
    pageMode: PageMode,
    selectedItems: Set<ReadHistoryRepository.AnyReadingHistory>,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onFavoriteLongPress: () -> Unit,
    favoriteRefreshToken: Int,
    navigator: me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator,
) {
    val favoriteRepository = LocalFavoriteRepository.current
    val target = remember(history) {
        FavoriteTargetPayload.Thread(
            tid = history.threadId,
            title = history.threadName,
            threadType = history.threadType,
            authorId = history.authorId,
            coverUrl = history.threadCover,
            forumId = history.forumId,
            forumName = history.forumName
        )
    }
    val favoriteSelection by produceState<FavoriteLocationSelection?>(
        initialValue = null,
        target,
        favoriteRefreshToken
    ) {
        value = favoriteRepository.getFavoriteLocationSelection(target)
    }

    ReadHistoryCard(
        history = history,
        timeLabel = formatTime(history.lastVisitTime),
        isSelectMode = pageMode == PageMode.Select,
        isSelected = history in selectedItems,
        isFavorited = favoriteSelection?.item != null,
        onClick = {
            if (pageMode == PageMode.Select) onToggleSelection()
            else {
                navigator.navigate(
                    IThreadReaderScreen(
                        tid = history.threadId,
                        title = history.threadName,
                        threadType = history.threadType,
                        authorId = history.authorId,
                        initialPage = history.page
                    )
                )
            }
        },
        onCoverClick = {
            if (pageMode == PageMode.Select) onToggleSelection()
            else if (history.threadType == ReadHistoryRepository.ThreadEntryType.Novel) {
                navigator.navigate(
                    INovelThreadDetailScreen(
                        tid = history.threadId,
                        title = history.threadName,
                        authorId = history.authorId
                    )
                )
            } else {
                navigator.navigate(
                    IThreadReaderScreen(
                        tid = history.threadId,
                        title = history.threadName,
                        threadType = history.threadType,
                        authorId = history.authorId,
                        initialPage = history.page
                    )
                )
            }
        },
        onDelete = onDelete,
        onFavorite = onFavorite,
        onFavoriteLongPress = onFavoriteLongPress
    )
}

@Composable
private fun TagHistoryItem(
    history: ReadHistoryRepository.TagMangaReadingHistory,
    pageMode: PageMode,
    selectedItems: Set<ReadHistoryRepository.AnyReadingHistory>,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onFavoriteLongPress: () -> Unit,
    favoriteRefreshToken: Int,
    navigator: me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator,
) {
    val favoriteRepository = LocalFavoriteRepository.current
    val target = remember(history) {
        FavoriteTargetPayload.TagManga(
            tagId = history.tagId,
            tagName = history.tagName,
            coverUrl = history.coverUrl
        )
    }
    val favoriteSelection by produceState<FavoriteLocationSelection?>(
        initialValue = null,
        target,
        favoriteRefreshToken
    ) {
        value = favoriteRepository.getFavoriteLocationSelection(target)
    }

    TagMangaHistoryCard(
        history = history,
        timeLabel = formatTime(history.lastVisitTime),
        isSelectMode = pageMode == PageMode.Select,
        isSelected = history in selectedItems,
        isFavorited = favoriteSelection?.item != null,
        onClick = {
            if (pageMode == PageMode.Select) onToggleSelection()
            else {
                navigator.navigate(
                    IImageReaderScreen(
                        tid = history.threadId,
                        postId = null,
                        fid = null,
                        threadTitle = history.threadTitle,
                        authorId = null,
                        imageList = emptyList(),
                        tagId = history.tagId,
                        tagName = history.tagName,
                        tagPage = history.tagPage,
                        tagThreads = emptyList(),
                        initialPage = history.threadImagePageIndex + 1
                    )
                )
            }
        },
        onCoverClick = {
            if (pageMode == PageMode.Select) onToggleSelection()
            else {
                navigator.navigate(
                    ITagDetailScreen(
                        tagId = history.tagId,
                        title = history.tagName,
                        page = history.tagPage
                    )
                )
            }
        },
        onDelete = onDelete,
        onFavorite = onFavorite,
        onFavoriteLongPress = onFavoriteLongPress
    )
}

private fun itemKey(history: ReadHistoryRepository.AnyReadingHistory): String {
    return when (history) {
        is ThreadReadingHistory -> "thread_${history.threadType}_${history.threadId.value}_${history.authorId?.value ?: 0}"
        is ReadHistoryRepository.TagMangaReadingHistory -> "tag_${history.tagId.value}_${history.threadId.value}"
        else -> "history_${history.lastVisitTime}"
    }
}

@Composable
private fun NormalTopBar(onSearch: () -> Unit, onMultiSelect: () -> Unit) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "閱讀紀錄",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.brownDeep,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = YamiboIcons.Search,
                contentDescription = "搜尋",
                modifier = Modifier.size(30.dp).offset(y = 5.dp),
                tint = colors.brownDeep
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onMultiSelect, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = YamiboIcons.Trashcan,
                contentDescription = "多選刪除",
                modifier = Modifier.size(30.dp),
                tint = colors.brownDeep
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Text(YamiboIcons.Back, color = colors.brownDeep, fontSize = 18.sp) }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = { Text("搜尋標題...", color = colors.textDark.copy(alpha = 0.4f), fontSize = 15.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textDark,
                unfocusedTextColor = colors.textDark,
                cursorColor = colors.brownDeep,
                focusedBorderColor = colors.brownDeep,
                unfocusedBorderColor = colors.brownPrimary.copy(alpha = 0.3f),
                focusedContainerColor = colors.brownPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = colors.brownPrimary.copy(alpha = 0.03f)
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )
        Spacer(Modifier.width(6.dp))
        Surface(onClick = onSearch, shape = RoundedCornerShape(12.dp), color = colors.brownDeep) {
            Text(
                text = "搜尋",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun SelectTopBar(
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onCancel: () -> Unit,
    onDeleteSelected: () -> Unit,
    selectedCount: Int
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "已選取 $selectedCount 項",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.brownDeep,
            modifier = Modifier.weight(1f)
        )
        Surface(onClick = onSelectAll, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text("全選", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.brownDeep)
        }
        if (selectedCount > 0) {
            Surface(onClick = onDeleteSelected, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.15f)) {
                Text("刪除", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
            }
        }
        Surface(onClick = onClearAll, shape = RoundedCornerShape(10.dp), color = Color(0xFFE53935).copy(alpha = 0.1f)) {
            Text("清空全部", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
        }
        Surface(onClick = onCancel, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f)) {
            Text("取消", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.brownDeep)
        }
    }
}

private fun groupByDate(
    items: List<ReadHistoryRepository.AnyReadingHistory>
): List<Pair<String, List<ReadHistoryRepository.AnyReadingHistory>>> {
    val now = currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val grouped = mutableMapOf<String, MutableList<ReadHistoryRepository.AnyReadingHistory>>()
    for (item in items) {
        val diffMs = now - item.lastVisitTime
        val label = when {
            diffMs < oneDayMs -> "今天"
            diffMs < 2 * oneDayMs -> "昨天"
            diffMs < 3 * oneDayMs -> "前天"
            diffMs < 7 * oneDayMs -> "${(diffMs / oneDayMs).toInt()} 天前"
            else -> formatDate(item.lastVisitTime)
        }
        grouped.getOrPut(label) { mutableListOf() }.add(item)
    }
    return grouped.toList()
}

private fun formatDate(timestamp: Long): String {
    val totalDays = timestamp / (24 * 60 * 60 * 1000L)
    var year = 1970
    var remainingDays = totalDays + (8 * 60 * 60 * 1000L / (24 * 60 * 60 * 1000L))
    while (true) {
        val daysInYear = if (isLeapYear(year)) 366L else 365L
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }
    val monthDays = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (days in monthDays) {
        if (remainingDays < days) break
        remainingDays -= days
        month++
    }
    val day = remainingDays.toInt() + 1
    return "$year/$month/$day"
}

private fun formatTime(timestamp: Long): String {
    val adjustedMs = timestamp + 8 * 60 * 60 * 1000L
    val totalMinutes = (adjustedMs / (60 * 1000L)) % (24 * 60)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private fun isLeapYear(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}
