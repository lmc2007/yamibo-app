package me.thenano.yamibo.yamibo_app.forumnovel

import androidx.compose.runtime.Composable

class ForumNovelShareActions(
    val createShareZipPath: (fileName: String) -> String,
    val shareZip: (zipPath: String, fileName: String) -> Unit,
)

@Composable
expect fun rememberForumNovelShareActions(
    onShareFailed: (message: String) -> Unit,
): ForumNovelShareActions
