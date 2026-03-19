package me.thenano.yamibo.yamibo_app.thread.image

import coil3.PlatformContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UIKit.UIPasteboard

private val client = HttpClient()

private suspend fun downloadImageBytes(url: String, cookie: String, referer: String): ByteArray? {
    return withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.get(url) {
                headers {
                    append("Cookie", cookie)
                    append("Referer", referer)
                }
            }
            response.readBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toUIImage(): UIImage? {
    val nsData = this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
    return UIImage.imageWithData(nsData)
}

actual suspend fun copyImageToClipboard(context: PlatformContext, url: String, cookie: String, referer: String) {
    val bytes = downloadImageBytes(url, cookie, referer) ?: return
    val image = bytes.toUIImage() ?: return
    withContext(Dispatchers.Main) {
        UIPasteboard.generalPasteboard.image = image
    }
}

actual suspend fun shareImageToApp(context: PlatformContext, url: String, cookie: String, referer: String) {
    val bytes = downloadImageBytes(url, cookie, referer) ?: return
    val image = bytes.toUIImage() ?: return
    withContext(Dispatchers.Main) {
        val activityViewController = UIActivityViewController(listOf(image), null)
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveImageToGallery(context: PlatformContext, url: String, cookie: String, referer: String) {
    val bytes = downloadImageBytes(url, cookie, referer) ?: return
    val image = bytes.toUIImage() ?: return
    withContext(Dispatchers.Main) {
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
    }
}
