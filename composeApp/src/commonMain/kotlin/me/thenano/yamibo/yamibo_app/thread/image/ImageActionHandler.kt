package me.thenano.yamibo.yamibo_app.thread.image

import coil3.PlatformContext

/**
 * Handle cross-platform copying, sharing, and saving of raw image data.
 * The implementations download the image locally to process the action natively.
 */
expect suspend fun copyImageToClipboard(context: PlatformContext, url: String, cookie: String, referer: String)

expect suspend fun shareImageToApp(context: PlatformContext, url: String, cookie: String, referer: String)

expect suspend fun saveImageToGallery(context: PlatformContext, url: String, cookie: String, referer: String)
