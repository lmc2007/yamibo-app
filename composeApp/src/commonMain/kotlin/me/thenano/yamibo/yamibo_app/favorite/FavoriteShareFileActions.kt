package me.thenano.yamibo.yamibo_app.favorite

import androidx.compose.runtime.Composable

class FavoriteShareFileActions(
    val exportJson: (fileName: String, jsonText: String) -> Unit,
    val shareJson: (fileName: String, jsonText: String) -> Unit,
    val pickJson: () -> Unit,
)

@Composable
expect fun rememberFavoriteShareFileActions(
    onExported: (String) -> Unit,
    onExportFailed: (String) -> Unit,
    onImportPicked: (String) -> Unit,
    onImportFailed: (String) -> Unit,
): FavoriteShareFileActions
