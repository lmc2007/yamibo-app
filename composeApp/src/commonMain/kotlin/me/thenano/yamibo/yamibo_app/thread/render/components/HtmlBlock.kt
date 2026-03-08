package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.ui.text.AnnotatedString

sealed class HtmlBlock {
    data class Text(val annotatedString: AnnotatedString) : HtmlBlock()
    data class Image(val url: String, val alt: String? = null) : HtmlBlock()
    data class Collapse(val title: String?, val contentBlocks: List<HtmlBlock>) : HtmlBlock()
    data class Locked(val cost: Int, val contentBlocks: List<HtmlBlock>) : HtmlBlock()
    data class Quote(val contentBlocks: List<HtmlBlock>) : HtmlBlock()
    data class Code(val codeText: String) : HtmlBlock()
}
