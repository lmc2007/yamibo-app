package me.thenano.yamibo.yamibo_app.util

import coil3.PlatformContext

/**
 * Open the system share sheet to share a text string (typically a URL).
 * Platform implementations use native share APIs (Android Intent / iOS UIActivityViewController).
 */
expect fun shareText(context: PlatformContext, text: String, title: String? = null)
