package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.SystemFont

@OptIn(ExperimentalTextApi::class)
internal actual val HtmlDefaultFontFamily: FontFamily = FontFamily(
    SystemFont("Microsoft YaHei"),
    SystemFont(".AppleSystemUIFont"),
)
