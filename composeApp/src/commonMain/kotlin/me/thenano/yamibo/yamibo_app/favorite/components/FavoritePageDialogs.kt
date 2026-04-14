package me.thenano.yamibo.yamibo_app.favorite.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.favorite.FavoriteCollectionDraft
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteGridMode
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteSortMode
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
internal fun FavoriteSortDialog(selected: FavoriteSortMode, descending: Boolean, onDismiss: () -> Unit, onSelect: (FavoriteSortMode) -> Unit, onConfirm: () -> Unit) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoriteSortMode.entries.forEach { mode ->
                    val isSelected = selected == mode
                    Surface(onClick = { onSelect(mode) }, shape = RoundedCornerShape(12.dp), color = if (isSelected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(mode.label, color = colors.textDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) Text(if (descending) "↓" else "↑", color = colors.brownDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
internal fun FavoriteGridModeDialog(selected: FavoriteGridMode, onDismiss: () -> Unit, onSelect: (FavoriteGridMode) -> Unit) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排列方式", color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoriteGridMode.entries.forEach { mode ->
                    val isSelected = selected == mode
                    Surface(onClick = { onSelect(mode) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = if (isSelected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(mode.label, color = colors.textDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) Text("已選擇", color = colors.brownDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
internal fun CollectionEditorDialog(draft: FavoriteCollectionDraft, onDismiss: () -> Unit, onConfirm: (String, String, Boolean) -> Unit) {
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
