package me.thenano.yamibo.yamibo_app.favorite

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.favorite.components.ReorderActionChip
import me.thenano.yamibo.yamibo_app.favorite.components.calculateReorderTargetIndex
import me.thenano.yamibo.yamibo_app.favorite.components.fastReorderDrag
import me.thenano.yamibo.yamibo_app.favorite.components.rememberReorderGapOffset
import me.thenano.yamibo.yamibo_app.favorite.components.reorderedList
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository as FavoriteRepositoryContract
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCategory
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCategoryDeletePreview
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

private val ManageRowHeight = 82.dp
private val ManageRowSpacing = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoriteCategoryManageScreen() {
    val colors = YamiboTheme.colors
    val favoriteRepository = LocalFavoriteRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val rowStridePx = with(density) { (ManageRowHeight + ManageRowSpacing).toPx() }
    val rowHeightPx = with(density) { ManageRowHeight.toPx() }

    var categories by remember { mutableStateOf<List<FavoriteCategory>>(emptyList()) }
    var draggingCategoryId by remember { mutableLongStateOf(0L) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingStartIndex by remember { mutableIntStateOf(-1) }
    var draggingTravelY by remember { mutableFloatStateOf(0f) }
    var draggingOverlayTop by remember { mutableFloatStateOf(0f) }
    val itemTopMap = remember { mutableMapOf<Long, Float>() }
    var dragItemTopSnapshot by remember { mutableStateOf<Map<Long, Float>>(emptyMap()) }
    var dragOrderedIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var animateGapOffsets by remember { mutableStateOf(true) }
    var containerRootTop by remember { mutableFloatStateOf(0f) }
    var deletePreview by remember { mutableStateOf<FavoriteCategoryDeletePreview?>(null) }
    var moveItemsToDefaultOnDelete by remember { mutableStateOf(true) }

    suspend fun reload() {
        categories = withContext(Dispatchers.Default) {
            favoriteRepository.ensureDefaults()
            favoriteRepository.getCategories()
        }
    }

    fun requestDelete(categoryId: Long) {
        scope.launch {
            val preview = withContext(Dispatchers.Default) {
                favoriteRepository.getCategoryDeletePreview(categoryId)
            } ?: return@launch
            if (preview.isDefaultCategory) {
                snackbarHostState.showSnackbar(appString(Res.string.favorite_default_category_cannot_delete, FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME))
                return@launch
            }
            moveItemsToDefaultOnDelete = true
            deletePreview = preview
        }
    }

    fun finishDragging(commit: Boolean) {
        val draggedId = draggingCategoryId
        val startIndex = draggingStartIndex
        val targetIndex = draggingIndex
        if (commit && draggedId != 0L && startIndex >= 0 && targetIndex >= 0) {
            animateGapOffsets = false
            categories = reorderedList(categories, startIndex, targetIndex)
        }
        draggingCategoryId = 0L
        draggingIndex = -1
        draggingStartIndex = -1
        draggingTravelY = 0f
        draggingOverlayTop = 0f
        dragItemTopSnapshot = emptyMap()
        dragOrderedIds = emptyList()
        if (commit && draggedId != 0L && targetIndex >= 0) {
            scope.launch {
                withContext(Dispatchers.Default) {
                    favoriteRepository.moveCategoryToIndex(draggedId, targetIndex)
                }
                reload()
            }
        } else {
            scope.launch { reload() }
        }
    }

    LaunchedEffect(navigator.stack.size) { reload() }

    Scaffold(
        containerColor = colors.creamBackground,
        snackbarHost = {
            YamiboSnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = appString(Res.string.ui_management_category),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text(YamiboIcons.Back, color = colors.creamBackground, fontSize = 20.sp)
                    }
                },
                actions = {
                    Surface(
                        onClick = { navigator.navigate(IFavoriteCategoryEditorScreen()) },
                        color = colors.brownPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = appString(Res.string.ui_add_category),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.brownDeep),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .onGloballyPositioned { coordinates ->
                    containerRootTop = coordinates.positionInRoot().y
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(ManageRowSpacing),
            ) {
                itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                    val reorderOffset = rememberReorderGapOffset(
                        key = category.id,
                        index = index,
                        draggingKey = draggingCategoryId,
                        draggingStartIndex = draggingStartIndex,
                        draggingTargetIndex = draggingIndex,
                        rowStridePx = rowStridePx,
                        animate = animateGapOffsets,
                    )
                    FavoriteCategoryCard(
                        category = category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ManageRowHeight)
                            .onGloballyPositioned { coordinates ->
                                itemTopMap[category.id] = coordinates.positionInRoot().y - containerRootTop
                            }
                            .graphicsLayer {
                                translationY = reorderOffset
                                alpha = if (draggingCategoryId == category.id) 0f else 1f
                            },
                        dragEnabled = true,
                        dragging = false,
                        onEdit = { navigator.navigate(IFavoriteCategoryEditorScreen(category.id)) },
                        onDelete = { requestDelete(category.id) },
                        onDragStart = {
                            animateGapOffsets = true
                            draggingCategoryId = category.id
                            draggingIndex = index
                            draggingStartIndex = index
                            draggingTravelY = 0f
                            dragItemTopSnapshot = itemTopMap.toMap()
                            dragOrderedIds = categories.map { it.id }
                            draggingOverlayTop = itemTopMap[category.id] ?: (index * rowStridePx)
                        },
                        onDragEnd = { finishDragging(commit = true) },
                        onDragCancel = { finishDragging(commit = false) },
                        onDrag = { _, dragY ->
                            draggingTravelY += dragY
                            if (draggingStartIndex == -1) return@FavoriteCategoryCard
                            draggingIndex = calculateReorderTargetIndex(
                                draggingId = category.id,
                                draggingOverlayTop = draggingOverlayTop,
                                draggingTravelY = draggingTravelY,
                                itemTopMap = dragItemTopSnapshot,
                                orderedIds = dragOrderedIds,
                                itemHeightPx = rowHeightPx,
                            )
                        },
                    )
                }
            }

            val draggingCategory = categories.firstOrNull { it.id == draggingCategoryId }
            if (draggingCategory != null) {
                FavoriteCategoryCard(
                    category = draggingCategory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ManageRowHeight)
                        .absoluteOffset(y = with(density) { (draggingOverlayTop + draggingTravelY).toDp() })
                        .zIndex(3f),
                    dragEnabled = false,
                    dragging = true,
                    onEdit = { navigator.navigate(IFavoriteCategoryEditorScreen(draggingCategory.id)) },
                    onDelete = { requestDelete(draggingCategory.id) },
                    onDragStart = {},
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { _, _ -> },
                )
            }
        }
    }

    if (deletePreview != null) {
        val preview = deletePreview!!
        AlertDialog(
            onDismissRequest = { deletePreview = null },
            confirmButton = {
                Surface(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.Default) {
                                favoriteRepository.deleteCategory(
                                    preview.categoryId,
                                    moveItemsToDefaultOnDelete,
                                )
                            }
                            deletePreview = null
                            reload()
                        }
                    },
                    color = colors.brownDeep,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = appString(Res.string.common_delete),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                Surface(
                    onClick = { deletePreview = null },
                    color = colors.brownPrimary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.18f)),
                ) {
                    Text(
                        text = appString(Res.string.ui_return),
                        color = colors.brownDeep,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            title = {
                Text(appString(Res.string.ui_delete_category), color = colors.brownDeep, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = appString(Res.string.favorite_category_preview_summary, localizedAppMessage(preview.categoryName), preview.totalDistinctItemCount, preview.collectionCount),
                        color = colors.textDark,
                    )
                    Text(
                        text = appString(Res.string.favorite_category_preview_detail, preview.directItemCount, preview.collectionItemCount),
                        color = colors.textDark.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = moveItemsToDefaultOnDelete,
                            onCheckedChange = { moveItemsToDefaultOnDelete = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.brownPrimary,
                                uncheckedColor = colors.brownPrimary.copy(alpha = 0.65f),
                                checkmarkColor = colors.creamBackground,
                            ),
                        )
                        Text(
                            text = appString(Res.string.favorite_move_all_to_category, FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME),
                            color = colors.textDark,
                            fontSize = 14.sp,
                        )
                    }
                }
            },
            containerColor = colors.creamSurface,
        )
    }
}

@Composable
private fun FavoriteCategoryCard(
    category: FavoriteCategory,
    modifier: Modifier = Modifier,
    dragEnabled: Boolean,
    dragging: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (pointerY: Float, dragY: Float) -> Unit,
) {
    val colors = YamiboTheme.colors
    val isDefaultCategory = category.name == FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME

    Surface(
        modifier = modifier.fastReorderDrag(
            enabled = dragEnabled,
            key = category.id,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onDrag = onDrag,
        ),
        shape = RoundedCornerShape(20.dp),
        color = if (dragging) colors.creamSurface else colors.creamBackground,
        border = BorderStroke(
            1.dp,
            if (dragging) colors.brownDeep.copy(alpha = 0.45f) else colors.brownPrimary.copy(alpha = 0.16f),
        ),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⋮⋮", color = colors.brownDeep.copy(alpha = 0.8f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(localizedAppMessage(category.name), color = colors.textDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            ReorderActionChip(text = appString(Res.string.ui_edit), onClick = onEdit)
            Box(modifier = Modifier.size(8.dp))
            if (isDefaultCategory) {
                Surface(
                    color = colors.brownPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.14f)),
                ) {
                    Text(
                        text = FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME,
                        color = colors.brownDeep.copy(alpha = 0.62f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            } else {
                ReorderActionChip(text = appString(Res.string.common_delete), onClick = onDelete, emphasized = true)
            }
        }
    }
}


