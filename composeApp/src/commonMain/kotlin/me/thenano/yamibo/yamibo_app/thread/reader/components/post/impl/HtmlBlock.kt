package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign

sealed class HtmlBlock {
    /** Stable content-hash ID for position tracking */
    abstract val anchorId: String

    data class Text(
        val annotatedString: AnnotatedString,
        val textAlign: TextAlign = TextAlign.Start,
        override val anchorId: String = ""
    ) : HtmlBlock()
    data class Image(val url: String, val alt: String? = null, val linkAddress: String? = null, override val anchorId: String = "") : HtmlBlock()
    data class Collapse(val title: String?, val contentBlocks: List<HtmlBlock>, override val anchorId: String = "") : HtmlBlock()
    data class Locked(val cost: Int, val contentBlocks: List<HtmlBlock>, override val anchorId: String = "") : HtmlBlock()
    data class Quote(val contentBlocks: List<HtmlBlock>, override val anchorId: String = "") : HtmlBlock()
    data class Code(val codeText: String, override val anchorId: String = "") : HtmlBlock()

    /** Table: rows of cells, each cell containing parsed HtmlBlocks */
    data class Table(val rows: List<TableRow>, override val anchorId: String = "") : HtmlBlock()
    data class TableRow(val cells: List<TableCell>)
    data class TableCell(val blocks: List<HtmlBlock>, val isHeader: Boolean = false)
}
