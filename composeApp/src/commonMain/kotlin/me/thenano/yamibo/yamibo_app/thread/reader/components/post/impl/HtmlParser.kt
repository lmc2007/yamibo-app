package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode

object HtmlParser {

    private data class ParsedAttachment(
        val url: String,
        val iconUrl: String?,
        val fileName: String,
        val uploadInfo: String?,
        val statInfo: String?,
    )

    /** Generate a stable content-hash based ID */
    private fun hashId(type: String, content: String, index: Int): String {
        val raw = "${type}_${index}_${content.take(64)}"
        return raw.hashCode().toUInt().toString(16)
    }

    /** Parses raw HTML string into a list of HtmlBlock for Compose to render */
    fun parseHtml(html: String): List<HtmlBlock> {
        // Strip raw newlines and convert &nbsp; to four-per-em space (1/4 em width)
        val cleanHtml = html.replace("\r", "").replace("\n", "").replace("&nbsp;", "\u2005")
        val document: Document = Ksoup.parseBodyFragment(cleanHtml)
        
        val blocks = mutableListOf<HtmlBlock>()
        val globalBuilder = AnnotatedString.Builder()
        var lastCommitIndex = 0
        var currentLinkHref: String? = null
        var blockCounter = 0
        var currentAlign = TextAlign.Start

        fun commitText() {
            if (globalBuilder.length > lastCommitIndex) {
                val textStr = globalBuilder.toAnnotatedString().subSequence(lastCommitIndex, globalBuilder.length)
                
                if (textStr.isNotEmpty()) {
                    val aid = hashId("t", textStr.text, blockCounter++)
                    blocks.add(HtmlBlock.Text(textStr, currentAlign, anchorId = aid))
                }
                lastCommitIndex = globalBuilder.length
            }
        }

        fun parseNode(node: Node, parentAlign: TextAlign = TextAlign.Start) {
            when (node) {
                is TextNode -> {
                    val txt = node.text()
                    if (txt.isNotEmpty()) globalBuilder.append(txt)
                }
                is Element -> {
                    val tag = node.tagName().lowercase()
                    when (tag) {
                        "br" -> {
                            globalBuilder.append("\n")
                        }
                        "hr" -> {
                            commitText()
                            val aid = hashId("hr", blockCounter.toString(), blockCounter++)
                            blocks.add(HtmlBlock.Hr(anchorId = aid))
                        }
                        "img" -> {
                            commitText()
                            val srcRaw = node.attr("file").takeIf { it.isNotBlank() }
                                ?: node.attr("zoomfile").takeIf { it.isNotBlank() }
                                ?: node.attr("src")
                            val src = if (srcRaw.contains("none.gif")) "" else srcRaw
                            val alt = node.attr("alt").takeIf { it.isNotBlank() }
                            if (src.isNotBlank()) {
                                val aid = hashId("i", src, blockCounter++)
                                blocks.add(HtmlBlock.Image(src, alt, currentLinkHref, anchorId = aid))
                            }
                        }
                        "div" -> {
                            val clazz = node.attr("class")
                            val alignAttr = node.attr("align").lowercase()
                            val newAlign = when (alignAttr) {
                                "center" -> TextAlign.Center
                                "right" -> TextAlign.Right
                                "left" -> TextAlign.Left
                                else -> parentAlign
                            }
                            
                            when {
                                clazz.contains("showcollapse_box") -> {
                                    commitText()
                                    val titleNode = node.selectFirst(".showcollapse_title")
                                    val titleText = titleNode?.text()?.takeIf { it.isNotBlank() } ?: "點擊展開 / 收起"
                                    titleNode?.remove()
                                    val innerBlocks = parseHtml(node.html())
                                    val aid = hashId("col", titleText, blockCounter++)
                                    blocks.add(HtmlBlock.Collapse(title = titleText, contentBlocks = innerBlocks, anchorId = aid))
                                }
                                clazz.contains("locked-content") -> {
                                    commitText()
                                    val costText = node.select(".locked-tip").text()
                                    val cost = costText.toIntOrNull() ?: 0
                                    val innerBlocks = parseHtml(node.html())
                                    val aid = hashId("lck", cost.toString(), blockCounter++)
                                    blocks.add(HtmlBlock.Locked(cost = cost, contentBlocks = innerBlocks, anchorId = aid))
                                }
                                clazz.contains("quote") || clazz.contains("blockquote") -> {
                                    commitText()
                                    val innerBlocks = parseHtml(node.html())
                                    val aid = hashId("q", node.text().take(32), blockCounter++)
                                    blocks.add(HtmlBlock.Quote(innerBlocks, anchorId = aid))
                                }
                                clazz.contains("blockcode") -> {
                                    commitText()
                                    val codeText = node.text()
                                    val aid = hashId("c", codeText.take(32), blockCounter++)
                                    blocks.add(HtmlBlock.Code(codeText, anchorId = aid))
                                }
                                else -> {
                                    // Handle nested alignment
                                    val prevAlign = currentAlign
                                    if (newAlign != prevAlign) {
                                        commitText()
                                        currentAlign = newAlign
                                    }
                                    
                                    if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                        globalBuilder.append("\n")
                                    }
                                    node.childNodes().forEach { parseNode(it, newAlign) }
                                    if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                        globalBuilder.append("\n")
                                    }
                                    
                                    if (newAlign != prevAlign) {
                                        commitText()
                                        currentAlign = prevAlign
                                    }
                                }
                            }
                        }
                        "table" -> {
                            // Detect if this is a multi-cell table (data table) or single-cell wrapper
                            val trs = node.select("tr")
                            val isDataTable = trs.any { tr ->
                                tr.select("td, th").size > 1
                            }

                            if (isDataTable) {
                                commitText()
                                val rows = trs.map { tr ->
                                    val cells = tr.select("td, th").map { cell ->
                                        val cellBlocks = parseHtml(cell.html())
                                        val isHeader = cell.tagName().lowercase() == "th" ||
                                            cell.select("strong, b").isNotEmpty()
                                        HtmlBlock.TableCell(blocks = cellBlocks, isHeader = isHeader)
                                    }
                                    HtmlBlock.TableRow(cells = cells)
                                }
                                val aid = hashId("tbl", rows.size.toString(), blockCounter++)
                                blocks.add(HtmlBlock.Table(rows = rows, anchorId = aid))
                            } else {
                                // Single-cell wrapper table — treat as inline content
                                if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                    globalBuilder.append("\n")
                                }
                                node.childNodes().forEach { parseNode(it, parentAlign) }
                                if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                    globalBuilder.append("\n")
                                }
                            }
                        }
                        "ul" -> {
                            val clazz = node.attr("class")
                            val attachment = parseAttachment(node)
                            if (clazz.contains("post_attlist") && attachment != null) {
                                commitText()
                                val aid = hashId("att", attachment.fileName, blockCounter++)
                                blocks.add(
                                    HtmlBlock.Attachment(
                                        url = attachment.url,
                                        iconUrl = attachment.iconUrl,
                                        fileName = attachment.fileName,
                                        uploadInfo = attachment.uploadInfo,
                                        statInfo = attachment.statInfo,
                                        anchorId = aid,
                                    )
                                )
                            } else {
                                if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                    globalBuilder.append("\n")
                                }
                                node.childNodes().forEach { parseNode(it, parentAlign) }
                                if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                    globalBuilder.append("\n")
                                }
                            }
                        }
                        "p", "ol", "tbody", "tr", "td", "th" -> {
                            if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                globalBuilder.append("\n")
                            }
                            node.childNodes().forEach { parseNode(it, parentAlign) }
                            if (globalBuilder.length > 0 && globalBuilder.toAnnotatedString().lastOrNull() != '\n') {
                                globalBuilder.append("\n")
                            }
                        }
                        "b", "strong" -> globalBuilder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { node.childNodes().forEach { parseNode(it, parentAlign) } }
                        "i", "em" -> globalBuilder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { node.childNodes().forEach { parseNode(it, parentAlign) } }
                        "u" -> globalBuilder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { node.childNodes().forEach { parseNode(it, parentAlign) } }
                        "s", "strike" -> globalBuilder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { node.childNodes().forEach { parseNode(it, parentAlign) } }
                        "a" -> {
                            val href = node.attr("href")
                            val prevLink = currentLinkHref
                            if (href.isNotBlank()) currentLinkHref = href
                            
                            val start = globalBuilder.length
                            node.childNodes().forEach { parseNode(it, parentAlign) }
                            val end = globalBuilder.length
                            
                            if (href.isNotBlank() && start < end) {
                                val textContent = globalBuilder.toAnnotatedString().substring(start, end)
                                if (textContent.trim().isNotEmpty()) {
                                    globalBuilder.addStringAnnotation("URL", href, start, end)
                                    globalBuilder.addStyle(SpanStyle(color = Color(0xFF007BFF), textDecoration = TextDecoration.Underline), start, end)
                                }
                            }
                            currentLinkHref = prevLink
                        }
                        "font" -> {
                            val colorAttr = node.attr("color")
                            val sizeAttr = node.attr("size")
                            val styleAttr = node.attr("style")
                            
                            var spanStyle = SpanStyle()
                            parseColor(colorAttr)?.let { spanStyle = spanStyle.copy(color = it) }
                            if (sizeAttr.isNotEmpty()) {
                                spanStyle = spanStyle.copy(fontSize = fontSizeToSp(sizeAttr))
                            }
                            if (styleAttr.isNotEmpty()) {
                                spanStyle = parseStylesFromStyleAttr(styleAttr, spanStyle)
                            }
                            
                            globalBuilder.withStyle(spanStyle) { node.childNodes().forEach { parseNode(it, parentAlign) } }
                        }
                        "span" -> {
                            val styleAttr = node.attr("style")
                            var spanStyle = SpanStyle()
                            if (styleAttr.isNotEmpty()) {
                                spanStyle = parseStylesFromStyleAttr(styleAttr, spanStyle)
                            }
                            globalBuilder.withStyle(spanStyle) { node.childNodes().forEach { parseNode(it, parentAlign) } }
                        }
                        else -> { node.childNodes().forEach { parseNode(it, parentAlign) } }
                    }
                }
                else -> { /* Other node types, ignore */ }
            }
        }

        document.body().childNodes().forEach { parseNode(it) }
        commitText()

        return blocks
    }

    private fun parseAttachment(node: Element): ParsedAttachment? {
        val link = node.selectFirst("a[href]") ?: return null
        val href = link.attr("href").trim()
        if (href.isEmpty()) return null

        val iconUrl = link.selectFirst("img")?.attr("src")?.trim()?.takeIf { it.isNotEmpty() }
        val fileName = link.selectFirst(".link")?.text()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: link.selectFirst(".tit")?.ownText()?.trim()?.takeIf { it.isNotEmpty() }
            ?: link.text().lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?: return null

        val metadata = link.select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
        return ParsedAttachment(
            url = href,
            iconUrl = iconUrl,
            fileName = fileName,
            uploadInfo = metadata.getOrNull(0),
            statInfo = metadata.getOrNull(1),
        )
    }

    private fun parseColor(color: String?): Color? {
        if (color.isNullOrBlank()) return null
        return try {
            if (color.startsWith("#")) {
                val hex = color.removePrefix("#")
                if (hex.length == 3) {
                    val r = hex[0].toString().repeat(2).toInt(16)
                    val g = hex[1].toString().repeat(2).toInt(16)
                    val b = hex[2].toString().repeat(2).toInt(16)
                    Color(r shl 16 or (g shl 8) or b or (0xFF shl 24))
                } else {
                    Color(hex.toLong(16) or 0xFF000000)
                }
            } else {
                when (color.lowercase()) {
                    "red" -> Color.Red
                    "blue" -> Color.Blue
                    "green" -> Color.Green
                    "yellow" -> Color.Yellow
                    "black" -> Color.Black
                    "white" -> Color.White
                    "grey", "gray" -> Color.Gray
                    "pink" -> Color(0xFFFFC0CB)
                    "orange" -> Color(0xFFFFA500)
                    "purple" -> Color(0xFF800080)
                    "skyblue" -> Color(0xFF87CEEB)
                    "palegreen" -> Color(0xFF98FB98)
                    "cyan" -> Color.Cyan
                    "magenta" -> Color.Magenta
                    else -> null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Standard HTML base font size in sp (font size="3") */
    private const val HTML_BASE_FONT_SIZE_SP = 16f

    private fun fontSizeToSp(size: String?): TextUnit {
        val absoluteSp = when (size) {
            "1" -> 12f
            "2" -> 14f
            "3" -> 16f // Standard base
            "4" -> 18f
            "5" -> 24f
            "6" -> 32f
            "7" -> 48f // Huge
            else -> 16f
        }
        return (absoluteSp / HTML_BASE_FONT_SIZE_SP).em
    }

    private fun parseStylesFromStyleAttr(styleAttr: String, spanStyle: SpanStyle): SpanStyle {
        var current = spanStyle
        // Support color: #xxxxxx or color: name
        val colorMatch = Regex("color:\\s*([^;\\s]+)").find(styleAttr)
        colorMatch?.groupValues?.get(1)?.trim()?.let { colorStr ->
             parseColor(colorStr)?.let { current = current.copy(color = it) }
        }
        
        // Support background-color: #xxxxxx or background-color: name
        val bgMatch = Regex("background-color:\\s*([^;\\s]+)").find(styleAttr)
        bgMatch?.groupValues?.get(1)?.trim()?.let { bgStr ->
            parseColor(bgStr)?.let { current = current.copy(background = it) }
        }

        // Support font-size: Npx / Npt / Nem
        val fontSizeMatch = Regex("font-size:\\s*([\\d.]+)\\s*(px|pt|em)").find(styleAttr)
        fontSizeMatch?.let { match ->
            val value = match.groupValues[1].toFloatOrNull()
            val unit = match.groupValues[2]
            if (value != null) {
                val emValue = when (unit) {
                    "px" -> value / HTML_BASE_FONT_SIZE_SP
                    "pt" -> (value * 4f / 3f) / HTML_BASE_FONT_SIZE_SP // 1pt ≈ 1.333px
                    "em" -> value
                    else -> null
                }
                emValue?.let { current = current.copy(fontSize = it.em) }
            }
        }
        return current
    }
}
