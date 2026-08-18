package me.thenano.yamibo.yamibo_app.forumnovel

import androidx.compose.runtime.Composable

@Composable
actual fun rememberForumNovelShareActions(
    onShareFailed: (message: String) -> Unit,
): ForumNovelShareActions {
    return ForumNovelShareActions(
        createShareZipPath = { "" },
        shareZip = { _, _ -> onShareFailed("iOS 暂不支持分享论坛小说包") },
    )
}
