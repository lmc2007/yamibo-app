package me.thenano.yamibo.yamibo_app.favorite

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.favorite.components.ReorderActionChip
import me.thenano.yamibo.yamibo_app.favorite.components.calculateReorderTargetIndex
import me.thenano.yamibo.yamibo_app.favorite.components.collectionColor
import me.thenano.yamibo.yamibo_app.favorite.components.fastReorderDrag
import me.thenano.yamibo.yamibo_app.favorite.components.rememberReorderGapOffset
import me.thenano.yamibo.yamibo_app.favorite.components.reorderedList
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository as FavoriteRepositoryContract
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCollection
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

private val EditorRowHeight = 78.dp
private val EditorRowSpacing = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoriteCategoryEditorScreen(categoryId: Long?) {
    val colors = YamiboTheme.colors
    val favoriteRepository = LocalFavoriteRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val rowStridePx = with(density) { (EditorRowHeight + EditorRowSpacing).toPx() }
    val rowHeightPx = with(density) { EditorRowHeight.toPx() }

    var workingCategoryId by remember { mutableLongStateOf(categoryId ?: 0L) }
    var categoryName by remember { mutableStateOf("") }
    var isDefaultCategory by remember { mutableStateOf(false) }
    var collections by remember { mutableStateOf<List<FavoriteCollection>>(emptyList()) }
    var editingCollection by remember { mutableStateOf<FavoriteCollection?>(null) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var draggingCollectionId by remember { mutableLongStateOf(0L) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingStartIndex by remember { mutableIntStateOf(-1) }
    var draggingTravelY by remember { mutableFloatStateOf(0f) }
    var draggingOverlayTop by remember { mutableFloatStateOf(0f) }
    val itemTopMap = remember { mutableMapOf<Long, Float>() }
    var dragItemTopSnapshot by remember { mutableStateOf<Map<Long, Float>>(emptyMap()) }
    var dragOrderedIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var animateGapOffsets by remember { mutableStateOf(true) }
    var containerRootTop by remember { mutableFloatStateOf(0f) }

    suspend fun reloadCollections() {
        collections = if (workingCategoryId != 0L) {
            withContext(Dispatchers.Default) {
                favoriteRepository.getCollections(workingCategoryId)
            }
        } else {
            emptyList()
        }
    }

    suspend fun createCategoryIfNeeded(): Boolean {
        if (workingCategoryId != 0L) return true
        if (categoryName.isBlank()) {
            snackbarHostState.showSnackbar(appString(Res.string.ui_please_enter_category_name_first))
            return false
        }
        return try {
            val category = withContext(Dispatchers.Default) {
                favoriteRepository.createCategory(categoryName)
            }
            workingCategoryId = category.id
            categoryName = category.name
            isDefaultCategory = category.name == FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME
            reloadCollections()
            true
        } catch (error: IllegalArgumentException) {
            snackbarHostState.showSnackbar(error.message?.let(::localizedAppMessage) ?: appString(Res.string.ui_failed_create_category))
            false
        }
    }

    fun finishDragging(commit: Boolean) {
        val draggedId = draggingCollectionId
        val startIndex = draggingStartIndex
        val targetIndex = draggingIndex
        if (commit && draggedId != 0L && startIndex >= 0 && targetIndex >= 0) {
            animateGapOffsets = false
            collections = reorderedList(collections, startIndex, targetIndex)
        }
        draggingCollectionId = 0L
        draggingIndex = -1
        draggingStartIndex = -1
        draggingTravelY = 0f
        draggingOverlayTop = 0f
        dragItemTopSnapshot = emptyMap()
        dragOrderedIds = emptyList()
        if (commit && draggedId != 0L && targetIndex >= 0) {
            scope.launch {
                withContext(Dispatchers.Default) {
                    favoriteRepository.moveCollectionToIndex(draggedId, targetIndex)
                }
                reloadCollections()
            }
        } else {
            scope.launch { reloadCollections() }
        }
    }

    LaunchedEffect(categoryId) {
        favoriteRepository.ensureDefaults()
        if (categoryId != null) {
            val category = favoriteRepository.getCategories().firstOrNull { it.id == categoryId }
            if (category != null) {
                workingCategoryId = category.id
                categoryName = category.name
                isDefaultCategory = category.name == FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME
                reloadCollections()
            }
        }
    }

    LaunchedEffect(categoryName, workingCategoryId) {
        if (workingCategoryId != 0L) {
            isDefaultCategory = categoryName == FavoriteRepositoryContract.DEFAULT_CATEGORY_NAME
        }
    }

    Scaffold(
        containerColor = colors.creamBackground,
        snackbarHost = {
            YamiboSnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (categoryId == null) appString(Res.string.ui_add_category) else appString(Res.string.ui_edit_categories),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text(YamiboIcons.Back, color = colors.creamBackground, fontSize = 20.sp)
                    }
                },
                actions = {
                    Surface(
                        onClick = {
                            scope.launch {
                                try {
                                    if (isDefaultCategory) {
                                        navigator.pop()
                                        return@launch
                                    }
                                    if (categoryName.isBlank()) {
                                        snackbarHostState.showSnackbar(appString(Res.string.ui_please_enter_category_name))
                                        return@launch
                                    }
                                    if (workingCategoryId == 0L) {
                                        createCategoryIfNeeded()
                                    } else {
                                        withContext(Dispatchers.Default) {
                                            favoriteRepository.updateCategory(workingCategoryId, categoryName)
                                        }
                                    }
                                    navigator.pop()
                                } catch (error: IllegalArgumentException) {
                                    snackbarHostState.showSnackbar(error.message?.let(::localizedAppMessage) ?: appString(Res.string.ui_save_failed_2))
                                }
                            }
                        },
                        color = colors.brownPrimary.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (isDefaultCategory) appString(Res.string.ui_finish) else appString(Res.string.ui_save),
                            color = colors.creamBackground,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.brownDeep),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!isDefaultCategory) {
                item {
                    Text(appString(Res.string.ui_category_name), color = colors.textDark.copy(alpha = 0.6f), fontSize = 13.sp)
                    Spacer(Modifier.size(8.dp))
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { categoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(appString(Res.string.ui_gather), color = colors.textDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Surface(
                        onClick = {
                            scope.launch {
                                if (createCategoryIfNeeded()) {
                                    editingCollection = null
                                    showCollectionDialog = true
                                }
                            }
                        },
                        color = colors.brownPrimary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.2f)),
                    ) {
                        Text(
                            appString(Res.string.ui_add_new_collection),
                            color = colors.brownDeep,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (workingCategoryId != 0L) {
                item {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    containerRootTop = coordinates.positionInRoot().y
                                }
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(EditorRowSpacing),
                        ) {
                            collections.forEachIndexed { index, collection ->
                                val reorderOffset = rememberReorderGapOffset(
                                    key = collection.id,
                                    index = index,
                                    draggingKey = draggingCollectionId,
                                    draggingStartIndex = draggingStartIndex,
                                    draggingTargetIndex = draggingIndex,
                                    rowStridePx = rowStridePx,
                                    animate = animateGapOffsets,
                                )
                                FavoriteCollectionEditorCard(
                                    collection = collection,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(EditorRowHeight)
                                        .onGloballyPositioned { coordinates ->
                                            itemTopMap[collection.id] =
                                                coordinates.positionInRoot().y - containerRootTop
                                        }
                                        .graphicsLayer {
                                            translationY = reorderOffset
                                            alpha = if (draggingCollectionId == collection.id) 0f else 1f
                                        },
                                    dragEnabled = true,
                                    dragging = false,
                                    onEdit = {
                                        editingCollection = collection
                                        showCollectionDialog = true
                                    },
                                    onDelete = {
                                        scope.launch {
                                            withContext(Dispatchers.Default) {
                                                favoriteRepository.deleteCollection(collection.id)
                                            }
                                            reloadCollections()
                                        }
                                    },
                                    onDragStart = {
                                        animateGapOffsets = true
                                        draggingCollectionId = collection.id
                                        draggingIndex = index
                                        draggingStartIndex = index
                                        draggingTravelY = 0f
                                        dragItemTopSnapshot = itemTopMap.toMap()
                                        dragOrderedIds = collections.map { it.id }
                                        draggingOverlayTop = itemTopMap[collection.id] ?: (index * rowStridePx)
                                    },
                                    onDragEnd = { finishDragging(commit = true) },
                                    onDragCancel = { finishDragging(commit = false) },
                                    onDrag = { _, dragY ->
                                        draggingTravelY += dragY
                                        if (draggingStartIndex == -1) return@FavoriteCollectionEditorCard
                                        draggingIndex = calculateReorderTargetIndex(
                                            draggingId = collection.id,
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

                        val draggingCollection = collections.firstOrNull { it.id == draggingCollectionId }
                        if (draggingCollection != null) {
                            FavoriteCollectionEditorCard(
                                collection = draggingCollection,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(EditorRowHeight)
                                    .absoluteOffset(y = with(density) { (draggingOverlayTop + draggingTravelY).toDp() })
                                    .zIndex(3f),
                                dragEnabled = false,
                                dragging = true,
                                onEdit = {
                                    editingCollection = draggingCollection
                                    showCollectionDialog = true
                                },
                                onDelete = {
                                    scope.launch {
                                        withContext(Dispatchers.Default) {
                                            favoriteRepository.deleteCollection(draggingCollection.id)
                                        }
                                        reloadCollections()
                                    }
                                },
                                onDragStart = {},
                                onDragEnd = {},
                                onDragCancel = {},
                                onDrag = { _, _ -> },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCollectionDialog && workingCategoryId != 0L) {
        CollectionDialog(
            initialName = editingCollection?.name.orEmpty(),
            initialColor = editingCollection?.colorKey ?: FavoriteRepositoryContract.DEFAULT_COLLECTION_COLOR,
            onDismiss = { showCollectionDialog = false },
            onConfirm = { name, colorKey ->
                scope.launch {
                    try {
                        withContext(Dispatchers.Default) {
                            if (editingCollection == null) {
                                favoriteRepository.createCollection(workingCategoryId, name, colorKey)
                            } else {
                                favoriteRepository.updateCollection(editingCollection!!.id, name, colorKey)
                            }
                        }
                        showCollectionDialog = false
                        reloadCollections()
                    } catch (error: IllegalArgumentException) {
                        snackbarHostState.showSnackbar(error.message?.let(::localizedAppMessage) ?: appString(Res.string.ui_save_failed_2))
                    }
                }
            },
        )
    }
}

@Composable
private fun FavoriteCollectionEditorCard(
    collection: FavoriteCollection,
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

    Surface(
        modifier = modifier.fastReorderDrag(
            enabled = dragEnabled,
            key = collection.id,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onDrag = onDrag,
        ),
        shape = RoundedCornerShape(18.dp),
        color = if (dragging) colors.creamSurface else colors.creamBackground,
        border = BorderStroke(
            1.dp,
            if (dragging) colors.brownDeep.copy(alpha = 0.4f) else colors.brownPrimary.copy(alpha = 0.16f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(12.dp).background(collectionColor(collection.colorKey), CircleShape))
            Spacer(Modifier.size(12.dp))
            Text("⋮⋮", color = colors.brownDeep.copy(alpha = 0.78f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(localizedAppMessage(collection.name), color = colors.textDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            ReorderActionChip(text = appString(Res.string.ui_edit), onClick = onEdit)
            Spacer(Modifier.size(8.dp))
            ReorderActionChip(text = appString(Res.string.common_delete), onClick = onDelete, emphasized = true)
        }
    }
}

@Composable
private fun CollectionDialog(
    initialName: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val colors = YamiboTheme.colors
    var name by remember(initialName) { mutableStateOf(initialName) }
    var colorKey by remember(initialColor) { mutableStateOf(initialColor) }
    val palette = listOf("brown", "rose", "blue", "green", "gold")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            FavoriteDialogButton(
                text = appString(Res.string.ui_sure),
                background = colors.brownDeep,
                contentColor = Color.White,
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), colorKey) },
            )
        },
        dismissButton = {
            FavoriteDialogButton(
                text = appString(Res.string.ui_return),
                background = colors.brownPrimary.copy(alpha = 0.14f),
                contentColor = colors.brownDeep,
                onClick = onDismiss,
            )
        },
        title = { Text(appString(Res.string.ui_gather), color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp),
                    label = { Text(appString(Res.string.ui_name)) },
                )
                Text(appString(Res.string.ui_color), color = colors.textDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { paletteKey ->
                        Surface(
                            onClick = { colorKey = paletteKey },
                            shape = CircleShape,
                            color = collectionColor(paletteKey),
                            border = BorderStroke(
                                width = if (paletteKey == colorKey) 2.dp else 0.dp,
                                color = colors.brownDeep,
                            ),
                        ) {
                            Spacer(Modifier.size(26.dp))
                        }
                    }
                }
            }
        },
        containerColor = colors.creamSurface,
    )
}

