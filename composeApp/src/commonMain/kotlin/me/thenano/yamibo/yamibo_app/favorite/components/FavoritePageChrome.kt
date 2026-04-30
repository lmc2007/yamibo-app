package me.thenano.yamibo.yamibo_app.favorite.components

import YamiboIcons
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.favorite.FavoriteDialogButton
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository.FavoriteCategory
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun HeaderRow(title: String, actions: List<Pair<String, () -> Unit>>) {
    val colors = YamiboTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.brownDeep, modifier = Modifier.weight(1f))
        actions.forEachIndexed { index, action ->
            ActionChip(action.first, action.second)
            if (index != actions.lastIndex) Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
fun FavoriteHeaderMenuRow(
    title: String,
    onSearch: () -> Unit,
    onCreateCategory: () -> Unit,
    onManageCategory: () -> Unit,
    onSyncFavorites: () -> Unit,
) {
    val colors = YamiboTheme.colors
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.brownDeep,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = YamiboIcons.Search,
                contentDescription = "搜尋收藏",
                modifier = Modifier.size(30.dp).offset(y = 5.dp),
                tint = colors.brownDeep,
            )
        }
        Spacer(Modifier.width(4.dp))
        RowScopeMenuBox(
            showMenu = showMenu,
            onShowMenu = { showMenu = true },
            onDismissMenu = { showMenu = false },
            onCreateCategory = onCreateCategory,
            onManageCategory = onManageCategory,
            onSyncFavorites = onSyncFavorites,
        )
    }
}

@Composable
private fun RowScopeMenuBox(
    showMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onCreateCategory: () -> Unit,
    onManageCategory: () -> Unit,
    onSyncFavorites: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Box {
        IconButton(onClick = onShowMenu, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = YamiboIcons.ThreeDots,
                contentDescription = "收藏選單",
                modifier = Modifier.size(30.dp),
                tint = colors.brownDeep,
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = onDismissMenu,
            modifier = Modifier.background(colors.creamSurface),
        ) {
            DropdownMenuItem(
                text = { Text("建新類別", color = colors.brownDeep) },
                leadingIcon = {
                    Icon(
                        imageVector = YamiboIcons.Plus,
                        contentDescription = null,
                        tint = colors.brownPrimary,
                    )
                },
                onClick = {
                    onDismissMenu()
                    onCreateCategory()
                },
            )
            DropdownMenuItem(
                text = { Text("管理類別", color = colors.brownDeep) },
                leadingIcon = {
                    Icon(
                        imageVector = YamiboIcons.Setting,
                        contentDescription = null,
                        tint = colors.brownPrimary,
                    )
                },
                onClick = {
                    onDismissMenu()
                    onManageCategory()
                },
            )
            DropdownMenuItem(
                text = { Text("同步百合會收藏", color = colors.brownDeep) },
                leadingIcon = {
                    Icon(
                        imageVector = YamiboIcons.Sync,
                        contentDescription = null,
                        tint = colors.brownPrimary,
                    )
                },
                onClick = {
                    onDismissMenu()
                    onSyncFavorites()
                },
            )
        }
    }
}

@Composable
fun FavoriteSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Text(YamiboIcons.Back, color = colors.brownDeep, fontSize = 18.sp)
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("搜尋收藏標題...", color = colors.textDark.copy(alpha = 0.4f), fontSize = 15.sp)
            },
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
                unfocusedContainerColor = colors.brownPrimary.copy(alpha = 0.03f),
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 15.sp),
        )
        Spacer(Modifier.width(6.dp))
        Surface(onClick = onSearch, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = colors.brownDeep) {
            Text(
                text = "搜尋",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun FavoriteSyncCategoryDialog(
    categories: List<FavoriteCategory>,
    selectedCategoryId: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "同步到哪個類別",
                color = colors.brownDeep,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(category.id) {
                                detectTapGestures(onTap = { onSelect(category.id) })
                            },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        color = if (selected) colors.brownPrimary.copy(alpha = 0.16f) else colors.creamBackground,
                        border = BorderStroke(
                            1.dp,
                            if (selected) colors.brownDeep else colors.brownPrimary.copy(alpha = 0.24f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = category.name,
                                modifier = Modifier.weight(1f),
                                color = if (selected) colors.brownDeep else colors.textDark,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                            if (selected) {
                                Text(
                                    text = "已選擇",
                                    color = colors.brownDeep,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FavoriteDialogButton(
                text = "開始同步",
                background = colors.brownDeep,
                contentColor = Color.White,
                onClick = { onConfirm(selectedCategoryId) },
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
        titleContentColor = colors.brownDeep,
        textContentColor = colors.textDark,
    )
}

@Composable
fun ActionChip(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = colors.brownPrimary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.12f)),
    ) {
        Text(
            text,
            color = colors.brownDeep,
            fontSize = if (text == YamiboIcons.Back) 18.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        )
    }
}
