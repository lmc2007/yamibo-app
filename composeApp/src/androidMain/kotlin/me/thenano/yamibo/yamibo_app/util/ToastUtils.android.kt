package me.thenano.yamibo.yamibo_app.util

import android.widget.Toast
import coil3.PlatformContext

actual fun showToast(context: PlatformContext, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
