package me.thenano.yamibo.yamibo_app.favorite

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

sealed interface FavoriteTargetPayload {
    data class Thread(
        val tid: ThreadId,
        val title: String,
        val threadType: ReadHistoryRepository.ThreadEntryType,
        val authorId: UserId?,
        val coverUrl: String?,
        val forumId: ForumId?,
        val forumName: String?,
    ) : FavoriteTargetPayload

    data class TagManga(
        val tagId: TagId,
        val tagName: String,
        val coverUrl: String?,
    ) : FavoriteTargetPayload
}

private data class FavoritePickerCategory(
    val categoryId: Long,
    val categoryName: String,
    val collections: List<LocalFavoriteRepository.FavoriteCollectionOption>,
)

data class FavoriteLocationSelection(
    val item: LocalFavoriteRepository.FavoriteItem?,
    val categoryIds: Set<Long>,
    val collectionIds: Set<Long>,
    val paths: List<String>,
)

suspend fun LocalFavoriteRepository.saveFavorite(
    target: FavoriteTargetPayload,
    categoryIds: List<Long> = emptyList(),
    collectionIds: List<Long> = emptyList(),
) {
    when (target) {
        is FavoriteTargetPayload.Thread -> {
            if (target.threadType == ReadHistoryRepository.ThreadEntryType.Novel) {
                addNovelThreadFavorite(
                    tid = target.tid,
                    title = target.title,
                    authorId = target.authorId,
                    coverUrl = target.coverUrl,
                    forumId = target.forumId,
                    forumName = target.forumName,
                    categoryIds = categoryIds,
                    collectionIds = collectionIds,
                )
            } else {
                addNormalThreadFavorite(
                    tid = target.tid,
                    title = target.title,
                    coverUrl = target.coverUrl,
                    forumId = target.forumId,
                    forumName = target.forumName,
                    categoryIds = categoryIds,
                    collectionIds = collectionIds,
                )
            }
        }

        is FavoriteTargetPayload.TagManga -> {
            addTagMangaFavorite(
                tagId = target.tagId,
                tagName = target.tagName,
                coverUrl = target.coverUrl,
                categoryIds = categoryIds,
                collectionIds = collectionIds,
            )
        }
    }
}

@Composable
fun FavoriteActionButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    tint: Color = YamiboTheme.colors.brownDeep,
    iconSize: Int = 20,
    filled: Boolean = false,
) {
    Surface(
        modifier = modifier.pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onTap = { onClick() },
                onLongPress = { onLongClick?.invoke() },
            )
        },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (filled) YamiboIcons.StarFilled else YamiboIcons.StarOutline,
                contentDescription = "收藏",
                tint = tint,
                modifier = Modifier.size(iconSize.dp),
            )
        }
    }
}

@Composable
fun FavoriteCollectionPickerDialog(
    categories: List<LocalFavoriteRepository.FavoriteCategory>,
    options: List<LocalFavoriteRepository.FavoriteCollectionOption>,
    initialCategorySelection: Set<Long>,
    initialCollectionSelection: Set<Long>,
    initialOpenedCategoryId: Long? = null,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCreateCollection: (Long) -> Unit = {},
    onConfirm: (Set<Long>, Set<Long>) -> Unit,
) {
    val colors = YamiboTheme.colors
    val checkboxColors = CheckboxDefaults.colors(
        checkedColor = colors.brownDeep,
        uncheckedColor = colors.textDark.copy(alpha = 0.4f),
        checkmarkColor = Color.White,
    )
    val groupedCategories = remember(categories, options) {
        val collectionsByCategory = options.groupBy { it.categoryId }
        categories
            .sortedWith(compareBy<LocalFavoriteRepository.FavoriteCategory> { it.sortOrder }.thenBy { it.id })
            .map { category ->
                FavoritePickerCategory(
                    categoryId = category.id,
                    categoryName = category.name,
                    collections = collectionsByCategory[category.id].orEmpty().sortedBy { it.id },
                )
            }
    }
    var selectedCategories by remember(categories, options, initialCategorySelection) {
        mutableStateOf(initialCategorySelection)
    }
    var selectedCollections by remember(categories, options, initialCollectionSelection) {
        mutableStateOf(initialCollectionSelection)
    }
    var currentCategoryId by remember(categories, options, initialOpenedCategoryId) {
        mutableStateOf(initialOpenedCategoryId)
    }

    val currentCategory = groupedCategories.firstOrNull { it.categoryId == currentCategoryId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.creamSurface,
            border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.15f)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "選擇類別",
                    color = colors.brownDeep,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.size(6.dp))
                Text(
                    text = currentCategory?.let { "${it.categoryName}/" } ?: "勾選大類，或點進去選擇小集合",
                    color = colors.textDark.copy(alpha = if (currentCategory == null) 0.58f else 0.82f),
                    fontSize = 12.sp,
                    fontWeight = if (currentCategory == null) FontWeight.Normal else FontWeight.Medium,
                )

                Spacer(Modifier.size(16.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.creamBackground,
                    border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.16f)),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (currentCategory == null) {
                            items(groupedCategories, key = { it.categoryId }) { category ->
                                FavoriteCategoryRow(
                                    category = category,
                                    selectedCategories = selectedCategories,
                                    selectedCollections = selectedCollections,
                                    triStateColors = checkboxColors,
                                    onToggleCategory = {
                                        val categoryCollectionIds = category.collections.map { it.id }.toSet()
                                        val hasAnySelection =
                                            category.categoryId in selectedCategories ||
                                                categoryCollectionIds.any(selectedCollections::contains)
                                        if (hasAnySelection) {
                                            selectedCategories = selectedCategories - category.categoryId
                                            selectedCollections = selectedCollections - categoryCollectionIds
                                        } else {
                                            selectedCategories = selectedCategories + category.categoryId
                                        }
                                    },
                                    onOpenCategory = { currentCategoryId = category.categoryId },
                                )
                            }
                        } else {
                            item(key = "category_root_${currentCategory.categoryId}") {
                                FavoriteCategoryRootRow(
                                    category = currentCategory,
                                    selectedCategories = selectedCategories,
                                    selectedCollections = selectedCollections,
                                    triStateColors = checkboxColors,
                                    onToggle = {
                                        val categoryCollectionIds = currentCategory.collections.map { it.id }.toSet()
                                        val hasAnySelection =
                                            currentCategory.categoryId in selectedCategories ||
                                                categoryCollectionIds.any(selectedCollections::contains)
                                        if (hasAnySelection) {
                                            selectedCategories = selectedCategories - currentCategory.categoryId
                                            selectedCollections = selectedCollections - categoryCollectionIds
                                        } else {
                                            selectedCategories = selectedCategories + currentCategory.categoryId
                                        }
                                    },
                                )
                            }
                            items(currentCategory.collections, key = { it.id }) { option ->
                                FavoriteCollectionRow(
                                    option = option,
                                    checked = option.id in selectedCollections,
                                    checkboxColors = checkboxColors,
                                    onToggle = {
                                        selectedCollections = selectedCollections.toggle(option.id)
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.size(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentCategory != null) {
                        FavoriteDialogButton(
                            text = "返回",
                            background = colors.brownPrimary.copy(alpha = 0.12f),
                            contentColor = colors.brownDeep,
                            onClick = { currentCategoryId = null },
                        )
                    }

                    FavoriteDialogButton(
                        text = "編輯",
                        background = colors.brownPrimary.copy(alpha = 0.08f),
                        contentColor = colors.brownDeep,
                        onClick = onEdit,
                    )

                    if (currentCategory != null) {
                        FavoriteDialogButton(
                            text = "新增集合",
                            background = colors.brownPrimary.copy(alpha = 0.08f),
                            contentColor = colors.brownDeep,
                            onClick = { onCreateCollection(currentCategory.categoryId) },
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FavoriteDialogButton(
                        text = "取消",
                        background = colors.textDark.copy(alpha = 0.06f),
                        contentColor = colors.textDark.copy(alpha = 0.8f),
                        onClick = onDismiss,
                    )

                    FavoriteDialogButton(
                        text = "確定",
                        background = colors.brownDeep,
                        contentColor = Color.White,
                        onClick = { onConfirm(selectedCategories, selectedCollections) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCategoryRow(
    category: FavoritePickerCategory,
    selectedCategories: Set<Long>,
    selectedCollections: Set<Long>,
    triStateColors: CheckboxColors,
    onToggleCategory: () -> Unit,
    onOpenCategory: () -> Unit,
) {
    val colors = YamiboTheme.colors
    val selectedCollectionCount = category.collections.count { it.id in selectedCollections }
    val toggleState = when {
        category.categoryId in selectedCategories -> ToggleableState.On
        selectedCollectionCount > 0 -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            state = toggleState,
            onClick = onToggleCategory,
            colors = triStateColors,
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.categoryName,
                color = colors.textDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildString {
                    if (category.categoryId in selectedCategories) {
                        append("已加入大類")
                    }
                    if (selectedCollectionCount > 0) {
                        if (isNotEmpty()) append(" / ")
                        append("$selectedCollectionCount 個集合")
                    }
                    if (isEmpty()) append("尚未加入")
                },
                color = colors.textDark.copy(alpha = 0.5f),
                fontSize = 11.sp,
            )
        }
        Surface(
            onClick = onOpenCategory,
            shape = CircleShape,
            color = colors.brownPrimary.copy(alpha = 0.12f),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ">",
                    color = colors.brownDeep,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FavoriteCategoryRootRow(
    category: FavoritePickerCategory,
    selectedCategories: Set<Long>,
    selectedCollections: Set<Long>,
    triStateColors: CheckboxColors,
    onToggle: () -> Unit,
) {
    val colors = YamiboTheme.colors
    val categoryCollectionIds = remember(category) { category.collections.map { it.id }.toSet() }
    val toggleState = when {
        category.categoryId in selectedCategories -> ToggleableState.On
        categoryCollectionIds.any(selectedCollections::contains) -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(category.categoryId, toggleState) {
                detectTapGestures(onTap = { onToggle() })
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(
                state = toggleState,
                onClick = onToggle,
                colors = triStateColors,
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.categoryName,
                    color = colors.textDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = when (toggleState) {
                        ToggleableState.On -> "已加入大類"
                        ToggleableState.Indeterminate -> "只加入部分集合"
                        ToggleableState.Off -> "未加入大類"
                    },
                    color = colors.textDark.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 48.dp, top = 4.dp),
            color = colors.brownPrimary.copy(alpha = 0.1f),
        )
    }
}

@Composable
private fun FavoriteCollectionRow(
    option: LocalFavoriteRepository.FavoriteCollectionOption,
    checked: Boolean,
    checkboxColors: CheckboxColors,
    onToggle: () -> Unit,
) {
    val colors = YamiboTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(option.id, checked) {
                detectTapGestures(onTap = { onToggle() })
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = checkboxColors,
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(collectionColor(option.colorKey), CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.collectionName,
                    color = colors.textDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "#${option.categoryName}",
                    color = colors.textDark.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 48.dp, top = 4.dp),
            color = colors.brownPrimary.copy(alpha = 0.1f),
        )
    }
}

@Composable
internal fun FavoriteDialogButton(
    text: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = BorderStroke(1.dp, background.copy(alpha = 0.9f)),
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = if (text == YamiboIcons.Back) 18.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

internal suspend fun LocalFavoriteRepository.findFavoriteItem(
    target: FavoriteTargetPayload,
): LocalFavoriteRepository.FavoriteItem? {
    val targetType = when (target) {
        is FavoriteTargetPayload.Thread -> {
            if (target.threadType == ReadHistoryRepository.ThreadEntryType.Novel) {
                LocalFavoriteRepository.FavoriteTargetType.ThreadNovel
            } else {
                LocalFavoriteRepository.FavoriteTargetType.ThreadNormal
            }
        }

        is FavoriteTargetPayload.TagManga -> LocalFavoriteRepository.FavoriteTargetType.TagManga
    }
    val targetId = when (target) {
        is FavoriteTargetPayload.Thread -> target.tid.value.toLong()
        is FavoriteTargetPayload.TagManga -> target.tagId.value.toLong()
    }
    val authorId = when (target) {
        is FavoriteTargetPayload.Thread -> target.authorId
        is FavoriteTargetPayload.TagManga -> null
    }
    return getFavoriteItem(targetType, targetId, authorId)
}

internal suspend fun LocalFavoriteRepository.getFavoriteLocationSelection(
    target: FavoriteTargetPayload,
): FavoriteLocationSelection {
    val item = findFavoriteItem(target) ?: return FavoriteLocationSelection(
        item = null,
        categoryIds = emptySet(),
        collectionIds = emptySet(),
        paths = emptyList(),
    )

    return FavoriteLocationSelection(
        item = item,
        categoryIds = getCategoryIdsForItem(item.id),
        collectionIds = getCollectionIdsForItem(item.id),
        paths = getFavoritePaths(item.id),
    )
}

internal suspend fun LocalFavoriteRepository.removeFavorite(target: FavoriteTargetPayload) {
    findFavoriteItem(target)?.let { item ->
        deleteFavoriteItems(setOf(item.id))
    }
}

internal suspend fun LocalFavoriteRepository.removeFavoriteFromLocation(
    target: FavoriteTargetPayload,
    categoryId: Long? = null,
    collectionId: Long? = null,
) {
    val item = findFavoriteItem(target) ?: return
    when {
        collectionId != null -> removeItemsFromCollections(setOf(item.id), setOf(collectionId))
        categoryId != null -> removeItemsFromCategory(setOf(item.id), categoryId)
        else -> deleteFavoriteItems(setOf(item.id))
    }
}

@Composable
fun FavoriteRemovalConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    val colors = YamiboTheme.colors
    var skipNextTime by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("確定要取消收藏嗎", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "取消後會移除目前這個收藏項目。",
                    color = colors.textDark,
                    fontSize = 14.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = skipNextTime,
                        onCheckedChange = { skipNextTime = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.brownDeep,
                            uncheckedColor = colors.textDark.copy(alpha = 0.4f),
                            checkmarkColor = Color.White,
                        ),
                    )
                    Text(
                        text = "下次不再提示",
                        color = colors.textDark,
                        fontSize = 13.sp,
                    )
                }
            }
        },
        confirmButton = {
            FavoriteDialogButton(
                text = "確定",
                background = colors.brownDeep,
                contentColor = Color.White,
                onClick = { onConfirm(skipNextTime) },
            )
        },
        dismissButton = {
            FavoriteDialogButton(
                text = "取消",
                background = colors.brownPrimary.copy(alpha = 0.1f),
                contentColor = colors.brownDeep,
                onClick = onDismiss,
            )
        },
        containerColor = colors.creamSurface,
    )
}

@Composable
fun FavoriteMultiPathRemoveDialog(
    paths: List<String>,
    tip: String,
    onDismiss: () -> Unit,
    onRemoveAll: () -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("取消全部收藏嗎", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "這個項目目前收藏在：${paths.joinToString("、")}",
                    color = colors.textDark,
                    fontSize = 14.sp,
                )
                Text(
                    text = tip,
                    color = colors.textDark.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            FavoriteDialogButton(
                text = "取消全部收藏",
                background = colors.brownPrimary.copy(alpha = 0.1f),
                contentColor = colors.brownDeep,
                onClick = onRemoveAll,
            )
        },
        dismissButton = {
            FavoriteDialogButton(
                text = "否",
                background = colors.textDark.copy(alpha = 0.06f),
                contentColor = colors.textDark.copy(alpha = 0.8f),
                onClick = onDismiss,
            )
        },
        containerColor = colors.creamSurface,
    )
}

private fun Set<Long>.toggle(id: Long): Set<Long> {
    return if (id in this) this - id else this + id
}
