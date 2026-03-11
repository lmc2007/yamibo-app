package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign

sealed class HtmlBlock {
    data class Text(
        val annotatedString: AnnotatedString,
        val textAlign: TextAlign = TextAlign.Start
    ) : HtmlBlock()
    data class Image(val url: String, val alt: String? = null, val linkAddress: String? = null) : HtmlBlock()
    data class Collapse(val title: String?, val contentBlocks: List<HtmlBlock>) : HtmlBlock()
    data class Locked(val cost: Int, val contentBlocks: List<HtmlBlock>) : HtmlBlock()
    data class Quote(val contentBlocks: List<HtmlBlock>) : HtmlBlock()
    data class Code(val codeText: String) : HtmlBlock()

    /** Table: rows of cells, each cell containing parsed HtmlBlocks */
    data class Table(val rows: List<TableRow>) : HtmlBlock()
    data class TableRow(val cells: List<TableCell>)
    data class TableCell(val blocks: List<HtmlBlock>, val isHeader: Boolean = false)
}
