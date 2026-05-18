package me.thenano.yamibo.yamibo_app.favorite.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

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
        title = { Text(appString(Res.string.ui_sort), color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoriteSortMode.entries.forEach { mode ->
                    val isSelected = selected == mode
                    Surface(onClick = { onSelect(mode) }, shape = RoundedCornerShape(12.dp), color = if (isSelected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(mode.localizedLabel(), color = colors.textDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) Text(if (descending) "↓" else "↑", color = colors.brownDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { ActionChip(appString(Res.string.ui_sure), onConfirm) },
        dismissButton = { ActionChip(appString(Res.string.ui_return), onDismiss) },
        containerColor = colors.creamSurface,
    )
}

@Composable
internal fun FavoriteGridModeDialog(selected: FavoriteGridMode, onDismiss: () -> Unit, onSelect: (FavoriteGridMode) -> Unit) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appString(Res.string.ui_arrangement), color = colors.brownDeep, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoriteGridMode.entries.forEach { mode ->
                    val isSelected = selected == mode
                    Surface(onClick = { onSelect(mode) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = if (isSelected) colors.brownPrimary.copy(alpha = 0.12f) else Color.Transparent) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(mode.localizedLabel(), color = colors.textDark, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isSelected) Text(appString(Res.string.ui_selected), color = colors.brownDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = { ActionChip(appString(Res.string.ui_return), onDismiss) },
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
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(appString(Res.string.ui_name)) })
                Text(appString(Res.string.ui_color), color = colors.textDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    palette.forEach { paletteKey ->
                        Box(Modifier.size(26.dp).clip(CircleShape).background(collectionColor(paletteKey)).pointerInput(paletteKey) { detectTapGestures(onTap = { colorKey = paletteKey }) }.border(if (paletteKey == colorKey) 2.dp else 0.dp, colors.brownDeep, CircleShape))
                    }
                }
                if (draft.showRemoveOriginalOption) {
                    Row(Modifier.fillMaxWidth().pointerInput(removeOriginal) { detectTapGestures(onTap = { removeOriginal = !removeOriginal }) }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = removeOriginal, onCheckedChange = { removeOriginal = it })
                        Spacer(Modifier.width(6.dp))
                        Text(appString(Res.string.ui_remove_original_entry), color = colors.textDark, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = { ActionChip(appString(Res.string.ui_sure)) { if (name.isNotBlank()) onConfirm(name.trim(), colorKey, removeOriginal) } },
        dismissButton = { ActionChip(appString(Res.string.ui_return), onDismiss) },
        containerColor = colors.creamSurface,
    )
}

