package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

internal actual val HtmlDefaultFontFamily: FontFamily = FontFamily(
    Font(DeviceFontFamilyName("Microsoft YaHei")),
    Font(DeviceFontFamilyName("sans-serif")),
)
