package me.thenano.yamibo.yamibo_app.favorite

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFavoriteShareFileActions(
    onExported: (String) -> Unit,
    onExportFailed: (String) -> Unit,
    onImportPicked: (String) -> Unit,
    onImportFailed: (String) -> Unit,
): FavoriteShareFileActions {
    return FavoriteShareFileActions(
        exportJson = { _, _ -> onExportFailed("iOS 暫不支援收藏分享檔案") },
        shareJson = { _, _ -> onExportFailed("iOS 暫不支援收藏分享檔案") },
        pickJson = { onImportFailed("iOS 暫不支援載入收藏分享檔案") },
    )
}
