package me.thenano.yamibo.yamibo_app.thread.image

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.BitmapImage
import coil3.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private suspend fun downloadImage(context: Context, url: String, cookie: String, referer: String, fileName: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .httpHeaders(
                    NetworkHeaders.Builder()
                        .add("Cookie", cookie)
                        .add("Referer", referer)
                        .build()
                )
                .build()

            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val diskCacheKey = result.diskCacheKey
                if (diskCacheKey != null) {
                    val snapshot = context.imageLoader.diskCache?.openSnapshot(diskCacheKey)
                    if (snapshot != null) {
                        val sourcePath = snapshot.data
                        val imagesDir = File(context.cacheDir, "images")
                        imagesDir.mkdirs()
                        val file = File(imagesDir, fileName)
                        val sourceFile = sourcePath.toFile()
                        sourceFile.copyTo(file, overwrite = true)
                        snapshot.close()
                        return@withContext file
                    }
                }
                
                // Fallback: compress decoded bitmap
                val image = result.image
                val bitmap = (image as? BitmapImage)?.bitmap
                if (bitmap != null) {
                    val imagesDir = File(context.cacheDir, "images")
                    imagesDir.mkdirs()
                    val file = File(imagesDir, fileName)
                    FileOutputStream(file).use { 
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }
                    return@withContext file
                }
            }
            withContext(Dispatchers.Main) { Toast.makeText(context, appString(Res.string.ui_failed_download_image), Toast.LENGTH_SHORT).show() }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { Toast.makeText(context, appString(Res.string.ui_failed_download_image), Toast.LENGTH_SHORT).show() }
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
    withContext(Dispatchers.Main) { Toast.makeText(context, appString(Res.string.ui_image_copied), Toast.LENGTH_SHORT).show() }
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
    val chooser = Intent.createChooser(intent, appString(Res.string.ui_share_pictures)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

actual suspend fun saveImageToGallery(context: PlatformContext, url: String, cookie: String, referer: String) {
    withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .httpHeaders(
                    NetworkHeaders.Builder()
                        .add("Cookie", cookie)
                        .add("Referer", referer)
                        .build()
                )
                .build()

            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val fileName = "yamibo_${System.currentTimeMillis()}.jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Yamibo")
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val diskCacheKey = result.diskCacheKey
                        var handled = false
                        if (diskCacheKey != null) {
                            val snapshot = context.imageLoader.diskCache?.openSnapshot(diskCacheKey)
                            if (snapshot != null) {
                                val sourceFile = snapshot.data.toFile()
                                sourceFile.inputStream().use { it.copyTo(outputStream) }
                                snapshot.close()
                                handled = true
                            }
                        }
                        
                        if (!handled) {
                            val image = result.image
                            val bitmap = (image as? BitmapImage)?.bitmap
                            if (bitmap != null) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                                handled = true
                            }
                        }
                        
                        if (handled) {
                            withContext(Dispatchers.Main) { 
                                Toast.makeText(context, appString(Res.string.ui_saved_picture_album), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) { 
                                Toast.makeText(context, appString(Res.string.ui_save_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) { 
                        Toast.makeText(context, appString(Res.string.ui_save_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, appString(Res.string.ui_download_failed), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { 
                Toast.makeText(context, appString(Res.string.ui_download_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

