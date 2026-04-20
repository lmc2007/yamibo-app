package me.thenano.yamibo.yamibo_app.util

import android.content.Intent
import coil3.PlatformContext

actual fun shareText(context: PlatformContext, text: String, title: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (title != null) {
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
    }
    val chooser = Intent.createChooser(intent, title ?: "分享").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
