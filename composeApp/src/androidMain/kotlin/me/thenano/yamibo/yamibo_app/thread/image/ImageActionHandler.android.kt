package me.thenano.yamibo.yamibo_app.thread.image

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import coil3.PlatformContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val client = HttpClient()

private suspend fun downloadImage(context: Context, url: String, cookie: String, referer: String, fileName: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get(url) {
                headers {
                    append("Cookie", cookie)
                    append("Referer", referer)
                }
            }
            val bytes = response.readBytes()
            val imagesDir = File(context.cacheDir, "images")
            imagesDir.mkdirs()
            val file = File(imagesDir, fileName)
            FileOutputStream(file).use { it.write(bytes) }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { Toast.makeText(context, "下載圖片失敗", Toast.LENGTH_SHORT).show() }
            null
        }
    }
}

actual suspend fun copyImageToClipboard(context: PlatformContext, url: String, cookie: String, referer: String) {
    val file = downloadImage(context, url, cookie, referer, "copy_image.jpg") ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newUri(context.contentResolver, "Image", uri)
    clipboard.setPrimaryClip(clip)
    withContext(Dispatchers.Main) { Toast.makeText(context, "已複製圖片", Toast.LENGTH_SHORT).show() }
}

actual suspend fun shareImageToApp(context: PlatformContext, url: String, cookie: String, referer: String) {
    val file = downloadImage(context, url, cookie, referer, "share_image.jpg") ?: return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Context from Compose might not be an Activity context, so add FLAG_ACTIVITY_NEW_TASK
    val chooser = Intent.createChooser(intent, "分享圖片").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

actual suspend fun saveImageToGallery(context: PlatformContext, url: String, cookie: String, referer: String) {
    val androidContext = context as Context
    withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get(url) {
                headers {
                    append("Cookie", cookie)
                    append("Referer", referer)
                }
            }
            val bytes = response.readBytes()
            val fileName = "yamibo_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Yamibo")
            }

            val uri = androidContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                androidContext.contentResolver.openOutputStream(uri)?.use { 
                    it.write(bytes) 
                }
                withContext(Dispatchers.Main) { 
                    Toast.makeText(androidContext, "已儲存圖片至相簿", Toast.LENGTH_SHORT).show() 
                }
            } else {
                withContext(Dispatchers.Main) { 
                    Toast.makeText(androidContext, "儲存失敗", Toast.LENGTH_SHORT).show() 
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { 
                Toast.makeText(androidContext, "下載失敗", Toast.LENGTH_SHORT).show() 
            }
        }
    }
}
