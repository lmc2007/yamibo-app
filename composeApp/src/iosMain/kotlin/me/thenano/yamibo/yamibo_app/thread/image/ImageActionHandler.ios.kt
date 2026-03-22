package me.thenano.yamibo.yamibo_app.thread.image

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.*

private suspend fun downloadImageBytes(
    context: PlatformContext,
    url: String,
    cookie: String,
    referer: String
): ByteArray? {
    return withContext(Dispatchers.Default) {
        try {
            val imageLoader = SingletonImageLoader.get(context)

            val request = ImageRequest.Builder(context)
                .data(url)
                .httpHeaders(
                    NetworkHeaders.Builder()
                        .add("Cookie", cookie)
                        .add("Referer", referer)
                        .build()
                )
                .build()

            val result = imageLoader.execute(request) as? SuccessResult ?: return@withContext null

            val diskCacheKey = result.diskCacheKey
            if (diskCacheKey != null) {
                val diskCache = imageLoader.diskCache
                val snapshot = diskCache?.openSnapshot(diskCacheKey)
                if (snapshot != null) {
                    val bytes = diskCache.fileSystem.read(snapshot.data) { readByteArray() }
                    snapshot.close()
                    return@withContext bytes
                }
            }

            result.image.toPngByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private fun coil3.Image.toPngByteArray(): ByteArray? {
    return try {
        val skiaImage = Image.makeFromBitmap(toBitmap())
        val data = skiaImage.encodeToData(EncodedImageFormat.PNG) ?: return null
        ByteArray(data.size).also { bytes ->
            data.bytes.copyInto(bytes)
        }
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toUIImage(): UIImage? {
    val nsData = usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
    return UIImage.imageWithData(nsData)
}

private fun UIViewController.topMostViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

actual suspend fun copyImageToClipboard(
    context: PlatformContext,
    url: String,
    cookie: String,
    referer: String
) {
    val bytes = downloadImageBytes(context, url, cookie, referer) ?: return
    val image = bytes.toUIImage() ?: return
    withContext(Dispatchers.Main) {
        UIPasteboard.generalPasteboard.image = image
    }
}

actual suspend fun shareImageToApp(
    context: PlatformContext,
    url: String,
    cookie: String,
    referer: String
) {
    val bytes = downloadImageBytes(context, url, cookie, referer) ?: return
    val image = bytes.toUIImage() ?: return
    withContext(Dispatchers.Main) {
        val activityViewController = UIActivityViewController(listOf(image), null)
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return@withContext
        rootViewController.topMostViewController()
            .presentViewController(activityViewController, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveImageToGallery(
    context: PlatformContext,
    url: String,
    cookie: String,
    referer: String
) {
    val bytes = downloadImageBytes(context, url, cookie, referer) ?: return
    val image = bytes.toUIImage() ?: return
    withContext(Dispatchers.Main) {
        UIImageWriteToSavedPhotosAlbum(image, null, null, null)
    }
}