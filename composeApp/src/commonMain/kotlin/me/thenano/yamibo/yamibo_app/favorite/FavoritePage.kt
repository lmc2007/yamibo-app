package me.thenano.yamibo.yamibo_app.favorite

import YamiboIcons
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.*
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteGridMode
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.detail.tag.ITagDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

private sealed interface FavoritePageState {
    data object Loading : FavoritePageState
    data class Ready(
        val categories: List<FavoriteCategory>,
        val selectedCategoryId: Long,
        val content: FavoriteCategoryContent,
    ) : FavoritePageState
}

private enum class FavoritePageMode { Normal, Select }
private enum class FavoriteSortMode(val label: String) { Default("默認"), UpdatedAt("更新時間"), Name("名稱"), LastRead("最後一次閱讀") }

private data class FavoriteCollectionDraft(
    val collectionId: Long? = null,
    val parentCategoryId: Long? = null,
    val title: String = "合成集合",
    val initialName: String = "",
    val initialColorKey: String = "brown",
    val removeOriginalItems: Boolean = true,
    val showRemoveOriginalOption: Boolean = true,
)

private data class FavoritePickerReturnState(
    val openedCategoryId: Long?,
    val categorySelection: Set<Long>,
    val collectionSelection: Set<Long>,
)

private data class FavoriteDeleteRequest(
    val itemIds: Set<Long>,
    val categoryId: Long,
    val collectionId: Long?,
    val hasMultiPathItems: Boolean,
)

private sealed interface FavoriteGridEntry {
    val key: String
    data class Collection(val value: FavoriteCollectionWithItems) : FavoriteGridEntry { override val key = "collection_${value.collection.id}" }
    data class Item(val value: FavoriteItem) : FavoriteGridEntry { override val key = "item_${value.id}" }
}

class IFavoriteCategoryManageScreen : Navigatable {
    override val id = buildId()
    @Composable override fun Content() { FavoriteCategoryManageScreen() }
}

class IFavoriteCategoryEditorScreen(private val categoryId: Long? = null) : Navigatable {
    override val id = buildId(categoryId ?: "new")
    @Composable override fun Content() { FavoriteCategoryEditorScreen(categoryId) }
}

@Composable
fun FavoritePage() {
    val colors = YamiboTheme.colors
    val appSettingsRepository = LocalAppSettingsRepository.current
    val favoriteRepository = LocalFavoriteRepository.current
    val readHistoryRepository = LocalReadHistoryRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteGridMode = appSettingsRepository.favoriteGridMode.state()

    var state by remember { mutableStateOf<FavoritePageState>(FavoritePageState.Loading) }
    var mode by rememberSaveable { mutableStateOf(FavoritePageMode.Normal) }
    var selectedItemIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var selectedCollectionIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var openedCollectionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var sortMode by rememberSaveable { mutableStateOf(FavoriteSortMode.Default) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    var pickerCategories by remember { mutableStateOf<List<FavoriteCategory>>(emptyList()) }
    var pickerOptions by remember { mutableStateOf<List<FavoriteCollectionOption>>(emptyList()) }
    var pickerCategorySelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pickerCollectionSelection by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pickerOpenedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var pickerReturnState by remember { mutableStateOf<FavoritePickerReturnState?>(null) }
    var collectionDraft by remember { mutableStateOf<FavoriteCollectionDraft?>(null) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showGridModeDialog by remember { mutableStateOf(false) }
    var deleteRequest by remember { mutableStateOf<FavoriteDeleteRequest?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteScopeDialog by remember { mutableStateOf(false) }

    fun showSnackbarMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    suspend fun reload(preferredCategoryId: Long? = null) {
        val currentSelectedCategoryId = (state as? FavoritePageState.Ready)?.selectedCategoryId
        val snapshot = withContext(Dispatchers.Default) {
            favoriteRepository.ensureDefaults()
            val categories = favoriteRepository.getCategories()
            val selectedCategoryId = preferredCategoryId ?: currentSelectedCategoryId ?: categories.firstOrNull()?.id ?: 0L
            val content = if (selectedCategoryId == 0L) {
                FavoriteCategoryContent(emptyList(), emptyList())
            } else {
                favoriteRepository.getCategoryContent(selectedCategoryId)
            }
            Triple(categories, selectedCategoryId, content)
        }
        val (categories, selectedCategoryId, content) = snapshot
        if (openedCollectionId != null && content.collections.none { it.collection.id == openedCollectionId }) {
            openedCollectionId = null
        }
        state = FavoritePageState.Ready(categories, selectedCategoryId, content)
    }

    suspend fun loadSelectionIntersection(itemIds: Set<Long>) {
        if (itemIds.isEmpty()) {
            pickerCategorySelection = emptySet()
            pickerCollectionSelection = emptySet()
            return
        }
        val (categories, collections) = withContext(Dispatchers.Default) {
            val intersectedCategories = itemIds.map { favoriteRepository.getCategoryIdsForItem(it) }
                .reduce { acc, set -> acc intersect set }
            val intersectedCollections = itemIds.map { favoriteRepository.getCollectionIdsForItem(it) }
                .reduce { acc, set -> acc intersect set }
            intersectedCategories to intersectedCollections
        }
        pickerCategorySelection = categories
        pickerCollectionSelection = collections
    }

    LaunchedEffect(navigator.stack.size) { reload() }

    DisposableEffect(mode, openedCollectionId, navigator) {
        if (mode == FavoritePageMode.Normal && openedCollectionId == null) {
            onDispose { }
        } else {
            val handler = {
                when {
                    mode == FavoritePageMode.Select -> {
                        selectedItemIds = emptySet()
                        selectedCollectionIds = emptySet()
                        mode = FavoritePageMode.Normal
                        true
                    }
                    openedCollectionId != null -> {
                        openedCollectionId = null
                        true
                    }
                    else -> false
                }
            }
            navigator.backHandlers.add(handler)
            onDispose { navigator.backHandlers.remove(handler) }
        }
    }

    fun requestDeleteFavorites(itemIds: Set<Long>, categoryId: Long, collectionId: Long?) {
        scope.launch {
            val hasMultiPathItems = withContext(Dispatchers.Default) {
                itemIds.any { favoriteRepository.getFavoritePaths(it).size > 1 }
            }
            deleteRequest = FavoriteDeleteRequest(
                itemIds = itemIds,
                categoryId = categoryId,
                collectionId = collectionId,
                hasMultiPathItems = hasMultiPathItems,
            )
            if (appSettingsRepository.skipFavoriteRemovalConfirm.getValue()) {
                if (hasMultiPathItems) {
                    showDeleteScopeDialog = true
                } else {
                    withContext(Dispatchers.Default) {
                        favoriteRepository.deleteFavoriteItems(itemIds)
                    }
                    selectedItemIds = emptySet()
                    selectedCollectionIds = emptySet()
                    mode = FavoritePageMode.Normal
                    reload(categoryId)
                    showSnackbarMessage("已刪除收藏")
                    deleteRequest = null
                }
            } else {
                showDeleteConfirmDialog = true
            }
        }
    }

    suspend fun performDeleteAllFavorites(request: FavoriteDeleteRequest) {
        withContext(Dispatchers.Default) {
            favoriteRepository.deleteFavoriteItems(request.itemIds)
        }
        selectedItemIds = emptySet()
        selectedCollectionIds = emptySet()
        mode = FavoritePageMode.Normal
        reload(request.categoryId)
        showSnackbarMessage("已刪除收藏")
        deleteRequest = null
    }

    suspend fun performDeleteCurrentDirectory(request: FavoriteDeleteRequest) {
        withContext(Dispatchers.Default) {
            if (request.collectionId != null) {
                favoriteRepository.removeItemsFromCollections(request.itemIds, setOf(request.collectionId))
            } else {
                favoriteRepository.removeItemsFromCategory(request.itemIds, request.categoryId)
            }
        }
        selectedItemIds = emptySet()
        selectedCollectionIds = emptySet()
        mode = FavoritePageMode.Normal
        reload(request.categoryId)
        showSnackbarMessage("已移除當下目錄收藏")
        deleteRequest = null
    }

    val ready = state as? FavoritePageState.Ready
    val openedCollection = ready?.content?.collections?.firstOrNull { it.collection.id == openedCollectionId }
    val visibleItems = openedCollection?.items ?: ready?.content?.directItems.orEmpty()
    var lastReadMap by remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    LaunchedEffect(ready?.selectedCategoryId, openedCollectionId, visibleItems.map { it.id }) {
        lastReadMap = withContext(Dispatchers.Default) {
            buildMap {
                visibleItems.forEach { item ->
                    put(item.id, favoriteItemLastReadAt(item, readHistoryRepository))
                }
            }
        }
    }

    val collections = sortCollections(ready?.content?.collections.orEmpty(), sortMode, sortDescending, lastReadMap)
    val items = sortItems(visibleItems, sortMode, sortDescending, lastReadMap)
    val gridEntries = buildList {
        if (openedCollection == null) addAll(collections.map(FavoriteGridEntry::Collection))
        addAll(items.map(FavoriteGridEntry::Item))
    }

    Box(Modifier.fillMaxSize().background(colors.creamBackground)) {
        if (ready == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.brownPrimary, modifier = Modifier.size(32.dp))
            }
        } else {
            FavoritePageContent(
                ready = ready,
                mode = mode,
                sortMode = sortMode,
                sortDescending = sortDescending,
                favoriteGridMode = favoriteGridMode,
                gridEntries = gridEntries,
                openedCollection = openedCollection,
                selectedItemIds = selectedItemIds,
                selectedCollectionIds = selectedCollectionIds,
                lastReadMap = lastReadMap,
                onCreateCategory = { navigator.navigate(IFavoriteCategoryEditorScreen()) },
                onManageCategory = { navigator.navigate(IFavoriteCategoryManageScreen()) },
                onSelectCategory = { categoryId ->
                    openedCollectionId = null
                    selectedItemIds = emptySet()
                    selectedCollectionIds = emptySet()
                    mode = FavoritePageMode.Normal
                    scope.launch { reload(categoryId) }
                },
                onShowGridMode = { showGridModeDialog = true },
                onShowSort = { showSortDialog = true },
                onBackToTab = { openedCollectionId = null },
                onSelectAll = {
                    if (openedCollection == null) selectedCollectionIds = ready.content.collections.map { it.collection.id }.toSet()
                    selectedItemIds = visibleItems.map { it.id }.toSet()
                },
                onClearSelection = {
                    selectedItemIds = emptySet()
                    selectedCollectionIds = emptySet()
                },
                onCancelSelection = {
                    selectedItemIds = emptySet()
                    selectedCollectionIds = emptySet()
                    mode = FavoritePageMode.Normal
                },
                onOpenMoveDialog = {
                    scope.launch {
                        val (categories, options) = withContext(Dispatchers.Default) {
                            favoriteRepository.getCategories() to favoriteRepository.getCollectionOptions()
                        }
                        pickerCategories = categories
                        pickerOptions = options
                        loadSelectionIntersection(selectedItemIds)
                        pickerOpenedCategoryId = null
                        showMoveDialog = true
                    }
                },
                onOpenMergeDialog = {
                    pickerReturnState = null
                    collectionDraft = FavoriteCollectionDraft(
                        parentCategoryId = ready.selectedCategoryId,
                        title = "合成集合",
                        showRemoveOriginalOption = true,
                    )
                },
                onEditSelectedCollection = {
                    val collection = ready.content.collections.firstOrNull { it.collection.id in selectedCollectionIds }?.collection ?: return@FavoritePageContent
                    collectionDraft = FavoriteCollectionDraft(
                        collectionId = collection.id,
                        parentCategoryId = collection.categoryId,
                        title = "編輯集合",
                        initialName = collection.name,
                        initialColorKey = collection.colorKey,
                        removeOriginalItems = false,
                        showRemoveOriginalOption = false,
                    )
                },
                onDissolveSelectedCollections = {
                    scope.launch {
                        ready.content.collections.filter { it.collection.id in selectedCollectionIds }.forEach { collection ->
                            val itemIds = collection.items.map { it.id }.toSet()
                            favoriteRepository.addItemsToLocations(itemIds = itemIds, categoryIds = setOf(ready.selectedCategoryId))
                            favoriteRepository.removeItemsFromCollections(itemIds = itemIds, collectionIds = setOf(collection.collection.id))
                            favoriteRepository.deleteCollection(collection.collection.id)
                        }
                        selectedCollectionIds = emptySet()
                        mode = FavoritePageMode.Normal
                        reload(ready.selectedCategoryId)
                        showSnackbarMessage("已解散集合")
                    }
                },
                onDeleteSelectedItems = {
                    requestDeleteFavorites(selectedItemIds, ready.selectedCategoryId, openedCollection?.collection?.id)
                },
                onOpenCollection = { openedCollectionId = it },
                onToggleItem = { id -> selectedItemIds = selectedItemIds.toggle(id) },
                onToggleCollection = { id -> selectedCollectionIds = selectedCollectionIds.toggle(id) },
                onEnterSelectItem = { id -> mode = FavoritePageMode.Select; selectedItemIds = selectedItemIds + id },
                onEnterSelectCollection = { id -> mode = FavoritePageMode.Select; selectedCollectionIds = selectedCollectionIds + id },
                onOpenItem = { openFavoriteItem(navigator, it) },
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)) { data ->
            Snackbar(snackbarData = data, containerColor = colors.brownDeep, contentColor = Color.White, shape = RoundedCornerShape(12.dp))
        }
    }

    if (showMoveDialog) {
        FavoriteCollectionPickerDialog(
            categories = pickerCategories,
            options = pickerOptions,
            initialCategorySelection = pickerCategorySelection,
            initialCollectionSelection = pickerCollectionSelection,
            initialOpenedCategoryId = pickerOpenedCategoryId,
            onDismiss = {
                showMoveDialog = false
                pickerOpenedCategoryId = null
            },
            onEdit = {
                showMoveDialog = false
                navigator.navigate(IFavoriteCategoryManageScreen())
            },
            onCreateCollection = { categoryId ->
                pickerReturnState = FavoritePickerReturnState(
                    openedCategoryId = categoryId,
                    categorySelection = pickerCategorySelection,
                    collectionSelection = pickerCollectionSelection,
                )
                showMoveDialog = false
                collectionDraft = FavoriteCollectionDraft(parentCategoryId = categoryId)
                    .copy(title = "新增集合", showRemoveOriginalOption = false)
            },
            onConfirm = { selectedCategories, selectedCollections ->
                scope.launch {
                    val current = state as? FavoritePageState.Ready ?: return@launch
                    withContext(Dispatchers.Default) {
                        favoriteRepository.setItemsLocations(selectedItemIds, selectedCategories, selectedCollections)
                    }
                    showMoveDialog = false
                    pickerOpenedCategoryId = null
                    pickerReturnState = null
                    selectedItemIds = emptySet()
                    selectedCollectionIds = emptySet()
                    mode = FavoritePageMode.Normal
                    reload(current.selectedCategoryId)
                    showSnackbarMessage("已更新收藏位置")
                }
            },
        )
    }

    collectionDraft?.let { draft ->
        CollectionEditorDialog(
            draft = draft,
            onDismiss = {
                val returnState = pickerReturnState
                collectionDraft = null
                if (returnState != null) {
                    pickerOpenedCategoryId = returnState.openedCategoryId
                    pickerCategorySelection = returnState.categorySelection
                    pickerCollectionSelection = returnState.collectionSelection
                    showMoveDialog = true
                    pickerReturnState = null
                }
            },
            onConfirm = { name, colorKey, removeOriginal ->
                scope.launch {
                    try {
                        val current = state as? FavoritePageState.Ready ?: return@launch
                        if (draft.collectionId != null) {
                            withContext(Dispatchers.Default) {
                                favoriteRepository.updateCollection(draft.collectionId, name, colorKey)
                            }
                            collectionDraft = null
                            selectedCollectionIds = emptySet()
                            mode = FavoritePageMode.Normal
                            reload(current.selectedCategoryId)
                            showSnackbarMessage("已更新集合")
                        } else if (draft.parentCategoryId != null) {
                            val createdCollection = withContext(Dispatchers.Default) {
                                favoriteRepository.createCollection(
                                    categoryId = draft.parentCategoryId,
                                    name = name,
                                    colorKey = colorKey,
                                )
                            }
                            val returnState = pickerReturnState
                            if (returnState == null) {
                                withContext(Dispatchers.Default) {
                                    favoriteRepository.addItemsToLocations(
                                        itemIds = selectedItemIds,
                                        collectionIds = setOf(createdCollection.id),
                                    )
                                    if (removeOriginal) {
                                        favoriteRepository.removeItemsFromCategory(selectedItemIds, draft.parentCategoryId)
                                    }
                                }
                                collectionDraft = null
                                selectedItemIds = emptySet()
                                selectedCollectionIds = emptySet()
                                mode = FavoritePageMode.Normal
                                reload(current.selectedCategoryId)
                                showSnackbarMessage("已建立集合")
                            } else {
                                val (categories, options) = withContext(Dispatchers.Default) {
                                    favoriteRepository.getCategories() to favoriteRepository.getCollectionOptions()
                                }
                                pickerCategories = categories
                                pickerOptions = options
                                pickerCategorySelection = returnState.categorySelection
                                pickerCollectionSelection = returnState.collectionSelection
                                pickerOpenedCategoryId = returnState.openedCategoryId
                                collectionDraft = null
                                showMoveDialog = true
                                pickerReturnState = null
                                showSnackbarMessage("已新增集合")
                            }
                        } else {
                            collectionDraft = null
                        }
                    } catch (error: IllegalArgumentException) {
                        showSnackbarMessage(error.message ?: "保存失敗")
                    }
                }
            },
        )
    }

    if (showSortDialog) {
        FavoriteSortDialog(
            selected = sortMode,
            descending = sortDescending,
            onDismiss = { showSortDialog = false },
            onSelect = { mode ->
                if (sortMode == mode) {
                    sortDescending = !sortDescending
                } else {
                    sortMode = mode
                    sortDescending = mode != FavoriteSortMode.Name
                }
            },
            onConfirm = { showSortDialog = false },
        )
    }

    if (showGridModeDialog) {
        FavoriteGridModeDialog(
            selected = favoriteGridMode,
            onDismiss = { showGridModeDialog = false },
            onSelect = {
                appSettingsRepository.favoriteGridMode.setValue(it)
                showGridModeDialog = false
            },
        )
    }

    if (showDeleteConfirmDialog) {
        FavoriteRemovalConfirmDialog(
            onDismiss = {
                showDeleteConfirmDialog = false
                deleteRequest = null
            },
            onConfirm = { skipNextTime ->
                appSettingsRepository.skipFavoriteRemovalConfirm.setValue(skipNextTime)
                showDeleteConfirmDialog = false
                val request = deleteRequest ?: return@FavoriteRemovalConfirmDialog
                scope.launch {
                    if (request.hasMultiPathItems) {
                        showDeleteScopeDialog = true
                    } else {
                        performDeleteAllFavorites(request)
                    }
                }
            },
        )
    }

    if (showDeleteScopeDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteScopeDialog = false
                deleteRequest = null
            },
            title = { Text("是否要刪除所有路徑下的收藏", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
            text = { Text("目前選取的收藏中，至少有一項存在於多個收藏路徑。", color = colors.textDark, fontSize = 14.sp) },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("取消") {
                        showDeleteScopeDialog = false
                        deleteRequest = null
                    }
                    ActionChip("是") {
                        val request = deleteRequest ?: return@ActionChip
                        showDeleteScopeDialog = false
                        scope.launch { performDeleteAllFavorites(request) }
                    }
                    Surface(
                        onClick = {
                            val request = deleteRequest ?: return@Surface
                            showDeleteScopeDialog = false
                            scope.launch { performDeleteCurrentDirectory(request) }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = colors.brownDeep,
                    ) {
                        Text(
                            text = "僅移除當下目錄",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = colors.creamSurface,
        )
    }
}

@Composable
private fun FavoritePageContent(
    ready: FavoritePageState.Ready,
    mode: FavoritePageMode,
    sortMode: FavoriteSortMode,
    sortDescending: Boolean,
    favoriteGridMode: FavoriteGridMode,
    gridEntries: List<FavoriteGridEntry>,
    openedCollection: FavoriteCollectionWithItems?,
    selectedItemIds: Set<Long>,
    selectedCollectionIds: Set<Long>,
    lastReadMap: Map<Long, Long>,
    onCreateCategory: () -> Unit,
    onManageCategory: () -> Unit,
    onSelectCategory: (Long) -> Unit,
    onShowGridMode: () -> Unit,
    onShowSort: () -> Unit,
    onBackToTab: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onCancelSelection: () -> Unit,
    onOpenMoveDialog: () -> Unit,
    onOpenMergeDialog: () -> Unit,
    onEditSelectedCollection: () -> Unit,
    onDissolveSelectedCollections: () -> Unit,
    onDeleteSelectedItems: () -> Unit,
    onOpenCollection: (Long) -> Unit,
    onToggleItem: (Long) -> Unit,
    onToggleCollection: (Long) -> Unit,
    onEnterSelectItem: (Long) -> Unit,
    onEnterSelectCollection: (Long) -> Unit,
    onOpenItem: (FavoriteItem) -> Unit,
) {
    val colors = YamiboTheme.colors
    val selectedCount = selectedItemIds.size + selectedCollectionIds.size
    Column(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = 500f)) + slideInVertically { -it / 4 }) togetherWith
                    (fadeOut(animationSpec = spring(stiffness = 700f)) + slideOutVertically { -it / 4 })
            },
            label = "favorite_header_mode",
        ) { currentMode ->
            if (currentMode == FavoritePageMode.Normal) {
                HeaderRow("我的收藏", listOf("建新類別" to onCreateCategory, "管理類別" to onManageCategory))
            } else {
                HeaderRow("已選 $selectedCount 項", buildList {
                    add("全選" to onSelectAll)
                    if (selectedItemIds.isNotEmpty() && selectedCollectionIds.isEmpty()) {
                        add("移動" to onOpenMoveDialog)
                        add("合成集合" to onOpenMergeDialog)
                        add("刪除" to onDeleteSelectedItems)
                    }
                    if (selectedCollectionIds.size == 1 && selectedItemIds.isEmpty()) add("編輯" to onEditSelectedCollection)
                    if (selectedCollectionIds.isNotEmpty() && selectedItemIds.isEmpty() && openedCollection == null) add("解散" to onDissolveSelectedCollections)
                    if (selectedCollectionIds.isNotEmpty() && selectedItemIds.isEmpty()) add("清空" to onClearSelection)
                    add("返回" to onCancelSelection)
                })
            }
        }

        ScrollableTabRow(
            selectedTabIndex = ready.categories.indexOfFirst { it.id == ready.selectedCategoryId }.coerceAtLeast(0),
            edgePadding = 8.dp,
            containerColor = colors.creamBackground,
            divider = { HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.12f)) },
            indicator = {},
        ) {
            ready.categories.forEach { category ->
                Tab(
                    selected = category.id == ready.selectedCategoryId,
                    onClick = { onSelectCategory(category.id) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(category.name, fontSize = 15.sp, fontWeight = if (category.id == ready.selectedCategoryId) FontWeight.Bold else FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .width(48.dp)
                                    .height(3.dp)
                                    .background(if (category.id == ready.selectedCategoryId) colors.brownDeep else Color.Transparent, RoundedCornerShape(999.dp))
                            )
                        }
                    },
                    selectedContentColor = colors.brownDeep,
                    unselectedContentColor = colors.textDark.copy(alpha = 0.66f),
                )
            }
        }

        if (openedCollection != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionChip(YamiboIcons.Back, onBackToTab)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(openedCollection.collection.name, color = colors.brownDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${openedCollection.items.size} 項", color = colors.textDark.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("排列: ${favoriteGridMode.label}", onShowGridMode)
                    ActionChip("排序: ${sortMode.label}${if (sortDescending) " ↓" else " ↑"}", onShowSort)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(ready.categories.firstOrNull { it.id == ready.selectedCategoryId }?.name.orEmpty(), color = colors.textDark.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("排列: ${favoriteGridMode.label}", onShowGridMode)
                    ActionChip("排序: ${sortMode.label}${if (sortDescending) " ↓" else " ↑"}", onShowSort)
                }
            }
        }

        if (openedCollection != null) {
            Box(Modifier.fillMaxSize()) {
                if (gridEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("這個集合還沒有內容", color = colors.textDark.copy(alpha = 0.52f), fontSize = 16.sp)
                    }
                } else {
                    FavoriteGridLayout(
                        entries = gridEntries,
                        favoriteGridMode = favoriteGridMode,
                        selecting = mode == FavoritePageMode.Select,
                        selectedItemIds = selectedItemIds,
                        selectedCollectionIds = selectedCollectionIds,
                        lastReadMap = lastReadMap,
                        onOpenCollection = onOpenCollection,
                        onToggleCollection = onToggleCollection,
                        onEnterSelectCollection = onEnterSelectCollection,
                        onOpenItem = onOpenItem,
                        onToggleItem = onToggleItem,
                        onEnterSelectItem = onEnterSelectItem,
                    )
                }
            }
        } else {
            val selectedTabIndex = ready.categories.indexOfFirst { it.id == ready.selectedCategoryId }.coerceAtLeast(0)
            val previousTabIndex = remember { mutableIntStateOf(selectedTabIndex) }
            val density = LocalDensity.current
            val direction = when {
                selectedTabIndex > previousTabIndex.intValue -> 1f
                selectedTabIndex < previousTabIndex.intValue -> -1f
                else -> 0f
            }
            val initialOffsetPx = with(density) { (16.dp * direction).toPx() }
            val initialAlpha = if (direction == 0f) 1f else 0.28f
            val contentOffsetX = remember(selectedTabIndex) { Animatable(initialOffsetPx) }
            val contentAlpha = remember(selectedTabIndex) { Animatable(initialAlpha) }
            LaunchedEffect(selectedTabIndex) {
                if (direction == 0f) {
                    contentOffsetX.snapTo(0f)
                    contentAlpha.snapTo(1f)
                } else {
                    launch {
                        contentOffsetX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 150),
                        )
                    }
                    contentAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 150),
                    )
                }
                previousTabIndex.intValue = selectedTabIndex
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = contentOffsetX.value
                        alpha = contentAlpha.value
                    }
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (gridEntries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("目前還沒有收藏", color = colors.textDark.copy(alpha = 0.52f), fontSize = 16.sp)
                        }
                    } else {
                        FavoriteGridLayout(
                            entries = gridEntries,
                            favoriteGridMode = favoriteGridMode,
                            selecting = mode == FavoritePageMode.Select,
                            selectedItemIds = selectedItemIds,
                            selectedCollectionIds = selectedCollectionIds,
                            lastReadMap = lastReadMap,
                            onOpenCollection = onOpenCollection,
                            onToggleCollection = onToggleCollection,
                            onEnterSelectCollection = onEnterSelectCollection,
                            onOpenItem = onOpenItem,
                            onToggleItem = onToggleItem,
                            onEnterSelectItem = onEnterSelectItem,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteGridLayout(
    entries: List<FavoriteGridEntry>,
    favoriteGridMode: FavoriteGridMode,
    selecting: Boolean,
    selectedItemIds: Set<Long>,
    selectedCollectionIds: Set<Long>,
    lastReadMap: Map<Long, Long>,
    onOpenCollection: (Long) -> Unit,
    onToggleCollection: (Long) -> Unit,
    onEnterSelectCollection: (Long) -> Unit,
    onOpenItem: (FavoriteItem) -> Unit,
    onToggleItem: (Long) -> Unit,
    onEnterSelectItem: (Long) -> Unit,
) {
    val contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp)
    if (favoriteGridMode == FavoriteGridMode.FIXED_GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            gridItems(entries, key = { it.key }, span = { GridItemSpan(1) }) { entry ->
                FavoriteGridEntryCard(
                    entry = entry,
                    selecting = selecting,
                    selectedItemIds = selectedItemIds,
                    selectedCollectionIds = selectedCollectionIds,
                    lastReadMap = lastReadMap,
                    onOpenCollection = onOpenCollection,
                    onToggleCollection = onToggleCollection,
                    onEnterSelectCollection = onEnterSelectCollection,
                    onOpenItem = onOpenItem,
                    onToggleItem = onToggleItem,
                    onEnterSelectItem = onEnterSelectItem,
                )
            }
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
        ) {
            staggeredItems(entries, key = { it.key }, span = { StaggeredGridItemSpan.SingleLane }) { entry ->
                FavoriteGridEntryCard(
                    entry = entry,
                    selecting = selecting,
                    selectedItemIds = selectedItemIds,
                    selectedCollectionIds = selectedCollectionIds,
                    lastReadMap = lastReadMap,
                    onOpenCollection = onOpenCollection,
                    onToggleCollection = onToggleCollection,
                    onEnterSelectCollection = onEnterSelectCollection,
                    onOpenItem = onOpenItem,
                    onToggleItem = onToggleItem,
                    onEnterSelectItem = onEnterSelectItem,
                )
            }
        }
    }
}

@Composable
private fun FavoriteGridEntryCard(
    entry: FavoriteGridEntry,
    selecting: Boolean,
    selectedItemIds: Set<Long>,
    selectedCollectionIds: Set<Long>,
    lastReadMap: Map<Long, Long>,
    onOpenCollection: (Long) -> Unit,
    onToggleCollection: (Long) -> Unit,
    onEnterSelectCollection: (Long) -> Unit,
    onOpenItem: (FavoriteItem) -> Unit,
    onToggleItem: (Long) -> Unit,
    onEnterSelectItem: (Long) -> Unit,
) {
    Box {
        when (entry) {
            is FavoriteGridEntry.Collection -> CollectionCardUi(
                collection = entry.value,
                selected = entry.value.collection.id in selectedCollectionIds,
                selecting = selecting,
                onOpen = { onOpenCollection(entry.value.collection.id) },
                onToggle = { onToggleCollection(entry.value.collection.id) },
                onEnterSelect = { onEnterSelectCollection(entry.value.collection.id) },
            )

            is FavoriteGridEntry.Item -> ItemCardUi(
                item = entry.value,
                selected = entry.value.id in selectedItemIds,
                selecting = selecting,
                lastReadAt = lastReadMap[entry.value.id],
                onOpen = { onOpenItem(entry.value) },
                onToggle = { onToggleItem(entry.value.id) },
                onEnterSelect = { onEnterSelectItem(entry.value.id) },
            )
        }
    }
}

@Composable
private fun HeaderRow(title: String, actions: List<Pair<String, () -> Unit>>) {
    val colors = YamiboTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.brownDeep, modifier = Modifier.weight(1f))
        actions.forEachIndexed { index, action ->
            ActionChip(action.first, action.second)
            if (index != actions.lastIndex) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = colors.brownPrimary.copy(alpha = 0.12f), border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.12f))) {
        Text(text, color = colors.brownDeep, fontSize = if (text == YamiboIcons.Back) 18.sp else 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
    }
}

@Composable
private fun CollectionCardUi(collection: FavoriteCollectionWithItems, selected: Boolean, selecting: Boolean, onOpen: () -> Unit, onToggle: () -> Unit, onEnterSelect: () -> Unit) {
    val colors = YamiboTheme.colors
    val borderColor = if (selected) colors.brownDeep else collectionColor(collection.collection.colorKey).copy(alpha = 0.45f)
    Surface(
        modifier = Modifier.fillMaxWidth().pointerInput(selecting, selected, collection.collection.id) {
            detectTapGestures(onTap = { if (selecting) onToggle() else onOpen() }, onLongPress = { if (selecting) onToggle() else onEnterSelect() })
        },
        shape = RoundedCornerShape(16.dp),
        color = colors.creamSurface,
        border = BorderStroke(1.5.dp, borderColor),
    ) {
        Column(Modifier.padding(6.dp)) {
            PreviewGrid(collection.items, collection.collection.colorKey)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(collection.collection.name, color = colors.brownDeep, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    SelectedDot()
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${collection.items.size} 項", color = colors.textDark.copy(alpha = 0.55f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun PreviewGrid(items: List<FavoriteItem>, colorKey: String) {
    val previewItems = items.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(2) { rowIndex ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(2) { columnIndex ->
                    val item = previewItems.getOrNull(rowIndex * 2 + columnIndex)
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(collectionColor(colorKey).copy(alpha = 0.18f))) {
                        val coverUrl = item?.coverUrl
                        if (coverUrl != null) {
                            AsyncImage(model = rememberImageRequest(coverUrl), contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else if (item != null) {
                            CoverTextFallback(title = item.title, color = collectionColor(colorKey))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCardUi(item: FavoriteItem, selected: Boolean, selecting: Boolean, lastReadAt: Long?, onOpen: () -> Unit, onToggle: () -> Unit, onEnterSelect: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().pointerInput(selecting, selected, item.id) {
            detectTapGestures(onTap = { if (selecting) onToggle() else onOpen() }, onLongPress = { if (selecting) onToggle() else onEnterSelect() })
        },
        shape = RoundedCornerShape(16.dp),
        color = colors.creamSurface,
        border = BorderStroke(1.5.dp, if (selected) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(6.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(12.dp)).background(colors.brownPrimary.copy(alpha = 0.12f))) {
                val coverUrl = item.coverUrl
                if (coverUrl != null) {
                    AsyncImage(model = rememberImageRequest(coverUrl), contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    CoverTextFallback(title = item.title, color = colors.brownDeep.copy(alpha = 0.75f))
                }
                /**
                 * IMPORTANT: Use fully-qualified androidx.compose.animation.AnimatedVisibility.
                 *
                 * If import is auto-optimized or replaced with implicit receiver version, Kotlin may
                 * resolve to a LayoutScope extension overload and fail with: "cannot be called in this
                 * context with an implicit receiver".
                 *
                 * @suppress Do NOT clean up this path.
                 */
                @Suppress("RemoveRedundantQualifierName")
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    SelectedDot()
                }
            }
            Spacer(Modifier.height(8.dp))
            item.forumName?.takeIf { it.isNotBlank() }?.let {
                Text("#$it", color = colors.textDark.copy(alpha = 0.58f), fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
            }
            Text(item.title, color = colors.textDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (lastReadAt != null) "最近閱讀 ${formatFavoriteTime(lastReadAt)}" else "最近閱讀 -",
                color = if (lastReadAt != null) colors.textDark.copy(alpha = 0.45f) else colors.textDark.copy(alpha = 0f),
                fontSize = 10.sp,
                minLines = 1,
            )
        }
    }
}

@Composable
private fun SelectedDot() {
    val colors = YamiboTheme.colors
    Box(Modifier.size(20.dp).background(colors.brownDeep, CircleShape), contentAlignment = Alignment.Center) {
        Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CoverTextFallback(title: String, color: Color) {
    val fontSize = when {
        title.length <= 6 -> 32.sp
        title.length <= 12 -> 24.sp
        title.length <= 24 -> 19.sp
        title.length <= 40 -> 15.sp
        else -> 12.sp
    }
    Text(
        text = title,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxSize().padding(8.dp),
        lineHeight = fontSize * 1.15f,
        textAlign = TextAlign.Center,
        softWrap = true,
    )
}

@Composable
private fun FavoriteSortDialog(
    selected: FavoriteSortMode,
    descending: Boolean,
    onDismiss: () -> Unit,
    onSelect: (FavoriteSortMode) -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoriteSortMode.entries.forEach { mode ->
                    val isSelected = selected == mode
                    Surface(
                        onClick = { onSelect(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(mode.label, color = colors.textDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Text(if (descending) "↓" else "↑", color = colors.brownDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { ActionChip("確定", onConfirm) },
        dismissButton = { ActionChip("返回", onDismiss) },
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun FavoriteGridModeDialog(
    selected: FavoriteGridMode,
    onDismiss: () -> Unit,
    onSelect: (FavoriteGridMode) -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排列方式", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoriteGridMode.entries.forEach { mode ->
                    val isSelected = selected == mode
                    Surface(
                        onClick = { onSelect(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(mode.label, color = colors.textDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Text("已選擇", color = colors.brownDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { ActionChip("返回", onDismiss) },
        dismissButton = {},
        containerColor = colors.creamSurface,
    )
}

@Composable
private fun CollectionEditorDialog(draft: FavoriteCollectionDraft, onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
    val colors = YamiboTheme.colors
    var name by remember(draft.collectionId, draft.initialName) { mutableStateOf(draft.initialName) }
    var colorKey by remember(draft.collectionId, draft.initialColorKey) { mutableStateOf(draft.initialColorKey) }
    var removeOriginal by remember(draft.collectionId, draft.removeOriginalItems) { mutableStateOf(draft.removeOriginalItems) }
    val palette = listOf("brown", "rose", "blue", "green", "gold")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(draft.title, color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("名稱") })
                Text("顏色", color = colors.textDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { paletteKey ->
                        Box(Modifier.size(26.dp).clip(CircleShape).background(collectionColor(paletteKey)).pointerInput(paletteKey) { detectTapGestures(onTap = { colorKey = paletteKey }) }.border(if (paletteKey == colorKey) 2.dp else 0.dp, colors.brownDeep, CircleShape))
                    }
                }
                if (draft.showRemoveOriginalOption) {
                    Row(Modifier.fillMaxWidth().pointerInput(removeOriginal) { detectTapGestures(onTap = { removeOriginal = !removeOriginal }) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = removeOriginal, onCheckedChange = { removeOriginal = it })
                        Spacer(Modifier.width(6.dp))
                        Text("移除原始條目", color = colors.textDark, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { ActionChip("確定") { if (name.isNotBlank()) onConfirm(name.trim(), colorKey, removeOriginal) } },
        dismissButton = { ActionChip("返回", onDismiss) },
        containerColor = colors.creamSurface,
    )
}

private suspend fun favoriteItemLastReadAt(item: FavoriteItem, repo: ReadHistoryRepository): Long {
    return when (item.targetType) {
        FavoriteTargetType.ThreadNormal -> repo.getPosition(ThreadId(item.targetId.toInt()), ReadHistoryRepository.ThreadEntryType.Normal)?.lastVisitTime ?: 0L
        FavoriteTargetType.ThreadNovel -> repo.getPosition(ThreadId(item.targetId.toInt()), ReadHistoryRepository.ThreadEntryType.Novel, item.authorId)?.lastVisitTime ?: 0L
        FavoriteTargetType.TagManga -> repo.getTagMangaReaderModeHistoryPosition(TagId(item.targetId.toInt()))?.lastVisitTime ?: 0L
    }
}

private fun sortCollections(
    items: List<FavoriteCollectionWithItems>,
    mode: FavoriteSortMode,
    descending: Boolean,
    lastReadMap: Map<Long, Long>,
): List<FavoriteCollectionWithItems> {
    val sorted = when (mode) {
        FavoriteSortMode.Default -> items.sortedWith(compareBy<FavoriteCollectionWithItems> { it.collection.sortOrder }.thenByDescending { it.collection.createdAt })
        FavoriteSortMode.UpdatedAt -> items.sortedByDescending { maxOf(it.collection.updatedAt, it.items.maxOfOrNull(FavoriteItem::updatedAt) ?: 0L) }
        FavoriteSortMode.Name -> items.sortedBy { it.collection.name.lowercase() }
        FavoriteSortMode.LastRead -> items.sortedByDescending { it.items.maxOfOrNull { item -> lastReadMap[item.id] ?: 0L } ?: 0L }
    }
    return if (descending) sorted else sorted.reversed()
}

private fun sortItems(
    items: List<FavoriteItem>,
    mode: FavoriteSortMode,
    descending: Boolean,
    lastReadMap: Map<Long, Long>,
): List<FavoriteItem> {
    val sorted = when (mode) {
        FavoriteSortMode.Default -> items.sortedByDescending(FavoriteItem::createdAt)
        FavoriteSortMode.UpdatedAt -> items.sortedByDescending(FavoriteItem::updatedAt)
        FavoriteSortMode.Name -> items.sortedBy { it.title.lowercase() }
        FavoriteSortMode.LastRead -> items.sortedByDescending { lastReadMap[it.id] ?: 0L }
    }
    return if (descending) sorted else sorted.reversed()
}

private fun openFavoriteItem(navigator: ComposableNavigator, item: FavoriteItem) {
    when (item.targetType) {
        FavoriteTargetType.ThreadNormal -> navigator.navigate(IThreadReaderScreen(tid = ThreadId(item.targetId.toInt()), title = item.title, threadType = ReadHistoryRepository.ThreadEntryType.Normal))
        FavoriteTargetType.ThreadNovel -> navigator.navigate(INovelThreadDetailScreen(tid = ThreadId(item.targetId.toInt()), title = item.title, authorId = item.authorId))
        FavoriteTargetType.TagManga -> navigator.navigate(ITagDetailScreen(tagId = TagId(item.targetId.toInt()), title = item.title))
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun formatFavoriteTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    val elapsed = (currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = elapsed / 1000L / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        days > 0L -> "${days}天前"
        hours > 0L -> "${hours}小時前"
        minutes > 0L -> "${minutes}分鐘前"
        else -> "剛剛"
    }
}

fun collectionColor(colorKey: String): Color = when (colorKey) {
    "rose" -> Color(0xFFD28B9C)
    "blue" -> Color(0xFF89A8C9)
    "green" -> Color(0xFF8FAE8A)
    "gold" -> Color(0xFFD6B46F)
    else -> Color(0xFFB4977A)
}
