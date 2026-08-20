package me.thenano.yamibo.yamibo_app.forumnovel

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import me.thenano.yamibo.yamibo_app.Logger
import java.io.File

@Composable
actual fun rememberForumNovelShareActions(
    onShareFailed: (message: String) -> Unit,
): ForumNovelShareActions {
    val context = LocalContext.current
    val shareDir = remember { File(context.cacheDir, "forum_novel_share").apply { mkdirs() } }

    return ForumNovelShareActions(
        createShareZipPath = { fileName ->
            val safeName = fileName
                .replace(Regex("""[\\/:*?"<>|]"""), "_")
                .ifBlank { "yamibo-forum-novel.zip" }
                .let { if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip" }
            File(shareDir, safeName).absolutePath
        },
        shareZip = { zipPath, fileName ->
            runCatching {
                val shareFile = File(zipPath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile,
                )
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    putExtra(Intent.EXTRA_TITLE, fileName)
                    // 显式声明 URI 授权，确保目标应用（含微信）能读取 app 私有目录中的文件
                    clipData = ClipData.newRawUri(fileName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(sendIntent, fileName).apply {
                    // 部分 ROM 的 chooser 不会把 grant flag 转发给最终目标应用，重复声明以兼容微信
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }.onFailure { error ->
                Logger.e("ForumNovelShareActions", "Failed to share forum novel zip name=$fileName", error)
                val message = when (error) {
                    is ActivityNotFoundException -> "找不到可分享文件的 App"
                    else -> error.message ?: "分享论坛小说失败"
                }
                onShareFailed(message)
            }
        },
    )
}
