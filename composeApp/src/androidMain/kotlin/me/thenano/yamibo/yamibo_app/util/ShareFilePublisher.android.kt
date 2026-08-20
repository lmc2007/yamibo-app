package me.thenano.yamibo.yamibo_app.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * 把待分享文件发布到公共下载目录（Download/Yamibo），返回 MediaStore content URI。
 *
 * 微信等应用读取公共下载目录的 URI（`content://media/...`）比读取 app 私有目录的
 * FileProvider URI 更可靠，可避免「获取资源失败」。API 29 以下或发布失败时返回 null，
 * 由调用方回退到 FileProvider 授权方案。
 */
fun publishShareFileToDownloads(context: Context, source: File, fileName: String, mimeType: String): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return try {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Yamibo")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        val output = context.contentResolver.openOutputStream(uri) ?: return null
        output.use { out -> source.inputStream().use { it.copyTo(out) } }
        uri
    } catch (_: Exception) {
        null
    }
}
