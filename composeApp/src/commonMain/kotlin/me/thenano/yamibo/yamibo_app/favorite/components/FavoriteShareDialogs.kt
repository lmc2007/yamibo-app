package me.thenano.yamibo.yamibo_app.favorite.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.components.controls.YamiboMultiSelectDialog
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.favorite.FavoriteDialogButton
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.repository.FavoriteShareRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteStoreRepository

@Composable
internal fun FavoriteShareExportPreviewDialog(
    folderCount: Int,
    itemCount: Int,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(i18n("分享收藏夾"), color = colors.textStrong, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = i18n(
                        "{} 個收藏夾，{} 個收藏項目",
                        folderCount,
                        itemCount,
                    ),
                    color = colors.textDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = i18n("可儲存為檔案，或直接分享給其他 App。"),
                    color = colors.textDark.copy(alpha = 0.68f),
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoriteDialogButton(
                    text = i18n("分享檔案"),
                    background = colors.brownDeep,
                    contentColor = Color.White,
                    onClick = onShare,
                )
                FavoriteDialogButton(
                    text = i18n("儲存檔案"),
                    background = colors.brownPrimary.copy(alpha = 0.12f),
                    contentColor = colors.textStrong,
                    onClick = onExport,
                )
            }
        },
        dismissButton = {
            FavoriteDialogButton(
                text = i18n("取消"),
                background = colors.textDark.copy(alpha = 0.06f),
                contentColor = colors.textDark.copy(alpha = 0.8f),
                onClick = onDismiss,
            )
        },
        containerColor = colors.creamSurface,
    )
}

@Composable
internal fun FavoriteShareExportDialog(
    options: List<FavoriteStoreRepository.FavoriteCategory>,
    selected: Set<FavoriteStoreRepository.FavoriteCategory>,
    onDismiss: () -> Unit,
    onConfirm: (Set<FavoriteStoreRepository.FavoriteCategory>) -> Unit,
) {
    YamiboMultiSelectDialog(
        title = i18n("分享哪些收藏夾"),
        options = options,
        selected = selected,
        onConfirm = onConfirm,
        onCancel = onDismiss,
        label = { it.name },
        modifier = Modifier.heightIn(max = 420.dp),
    )
}

@Composable
internal fun FavoriteShareImportPreviewDialog(
    preview: FavoriteShareRepository.ImportPreview,
    onDismiss: () -> Unit,
    onCreateFolders: () -> Unit,
    onAddToExistingFolders: () -> Unit,
) {
    val colors = YamiboTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(i18n("載入收藏夾"), color = colors.textStrong, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = i18n(
                        "{} 個收藏夾，{} 個收藏項目",
                        preview.folderCount,
                        preview.itemCount,
                    ),
                    color = colors.textDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = i18n(
                        "重複 {} / 不支援 {} / 無效 {}",
                        preview.duplicateCount,
                        preview.unsupportedCount,
                        preview.invalidCount,
                    ),
                    color = colors.textDark.copy(alpha = 0.68f),
                    fontSize = 12.sp,
                )
                preview.folders.take(4).forEach { folder ->
                    Text(
                        text = "• ${folder.name} (${folder.itemCount})",
                        color = colors.textDark.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                    )
                }
                if (preview.folders.size > 4) {
                    Text(
                        text = i18n("另有 {} 個收藏夾", preview.folders.size - 4),
                        color = colors.textDark.copy(alpha = 0.58f),
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FavoriteDialogButton(
                    text = i18n("建立新收藏夾"),
                    background = colors.brownDeep,
                    contentColor = Color.White,
                    onClick = onCreateFolders,
                )
                FavoriteDialogButton(
                    text = i18n("加入既有收藏夾"),
                    background = colors.brownPrimary.copy(alpha = 0.12f),
                    contentColor = colors.textStrong,
                    onClick = onAddToExistingFolders,
                )
            }
        },
        dismissButton = {
            FavoriteDialogButton(
                text = i18n("取消"),
                background = colors.textDark.copy(alpha = 0.06f),
                contentColor = colors.textDark.copy(alpha = 0.8f),
                onClick = onDismiss,
            )
        },
        containerColor = colors.creamSurface,
    )
}

@Composable
internal fun FavoriteShareImportTargetDialog(
    options: List<FavoriteStoreRepository.FavoriteCategory>,
    selected: Set<FavoriteStoreRepository.FavoriteCategory>,
    onDismiss: () -> Unit,
    onConfirm: (Set<FavoriteStoreRepository.FavoriteCategory>) -> Unit,
) {
    YamiboMultiSelectDialog(
        title = i18n("加入到哪些收藏夾"),
        options = options,
        selected = selected,
        onConfirm = onConfirm,
        onCancel = onDismiss,
        label = { it.name },
        modifier = Modifier.heightIn(max = 420.dp),
    )
}
