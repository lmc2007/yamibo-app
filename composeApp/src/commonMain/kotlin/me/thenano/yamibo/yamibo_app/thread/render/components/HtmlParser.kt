package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode

object HtmlParser {

    /** Parses raw HTML string into a list of HtmlBlock for Compose to render */
    fun parseHtml(html: String): List<HtmlBlock> {
        // Wrap with a root div to ensure we have a single root element if possible, 
        // but parseBodyFragment does this effectively on its own for the body.
        val document: Document = Ksoup.parseBodyFragment(html)
        val body = document.body()
        return parseChildren(body)
    }

    private fun parseChildren(parent: Element): List<HtmlBlock> {
        val blocks = mutableListOf<HtmlBlock>()
        var currentTextBuilder = AnnotatedString.Builder()

        fun commitText() {
            if (currentTextBuilder.length > 0) {
                blocks.add(HtmlBlock.Text(currentTextBuilder.toAnnotatedString()))
                currentTextBuilder = AnnotatedString.Builder()
            }
        }

        // We process nodes. If we hit a block element, we commit the text, 
        // add the block, and start a new text builder.
        for (node in parent.childNodes()) {
            when (node) {
                is TextNode -> {
                    // Extract text but retain some structure, let's just append
                    currentTextBuilder.append(node.text())
                }
                is Element -> {
                    when (node.tagName().lowercase()) {
                        "br" -> currentTextBuilder.append("\n")
                        "img" -> {
                            commitText()
                            val src = node.attr("src")
                            val alt = node.attr("alt").takeIf { it.isNotBlank() }
                            // Only add if src is not empty
                            if (src.isNotBlank()) {
                                blocks.add(HtmlBlock.Image(src, alt))
                            }
                        }
                        "div" -> {
                            // Check for special blocks or just ordinary div.
                            // If it's ordinary div, treat it as a block container -> commit text, recurse children
                            commitText()
                            
                            val clazz = node.attr("class")
                            when {
                                clazz.contains("showcollapse_box") -> {
                                    // Parse title if any, the title is usually not explicit, but sometimes it is.
                                    // For simplicity we just take everything inside as the content, 
                                    // or if there's a specific title span we extract it.
                                    blocks.add(HtmlBlock.Collapse(title = "點擊展開 / 收起", contentBlocks = parseChildren(node)))
                                }
                                clazz.contains("locked-content") -> {
                                    val costText = node.select(".locked-tip").text()
                                    val cost = costText.toIntOrNull() ?: 0
                                    blocks.add(HtmlBlock.Locked(cost = cost, contentBlocks = parseChildren(node)))
                                }
                                clazz.contains("quote") || clazz.contains("blockquote") -> {
                                    blocks.add(HtmlBlock.Quote(contentBlocks = parseChildren(node)))
                                }
                                clazz.contains("blockcode") -> {
                                    blocks.add(HtmlBlock.Code(codeText = node.text()))
                                }
                                else -> {
                                    // Ordinary div -> just block of text/children
                                    blocks.addAll(parseChildren(node))
                                }
                            }
                        }
                        "p", "ul", "ol", "table", "tbody", "tr", "td" -> {
                            // Simple block level elements
                            commitText()
                            blocks.addAll(parseChildren(node))
                            commitText() // In case children had inline text
                        }
                        // Inline elements
                        "b", "strong" -> {
                            currentTextBuilder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                parseInlineChildren(node, currentTextBuilder)
                            }
                        }
                        "i", "em" -> {
                            currentTextBuilder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                parseInlineChildren(node, currentTextBuilder)
                            }
                        }
                        "u" -> {
                            currentTextBuilder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                parseInlineChildren(node, currentTextBuilder)
                            }
                        }
                        "s", "strike" -> {
                            currentTextBuilder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                parseInlineChildren(node, currentTextBuilder)
                            }
                        }
                        "a" -> {
                            val href = node.attr("href")
                            // 1.7+ compose clickable text is simpler: we can use LinkAnnotation, 
                            // but LinkAnnotation requires specific handling in Compose 1.7.
                            // However, we can also use string annotation.
                            val start = currentTextBuilder.length
                            parseInlineChildren(node, currentTextBuilder)
                            val end = currentTextBuilder.length
                            if (href.isNotBlank()) {
                                currentTextBuilder.addStringAnnotation(tag = "URL", annotation = href, start = start, end = end)
                                currentTextBuilder.addStyle(
                                    style = SpanStyle(color = Color(0xFF007BFF), textDecoration = TextDecoration.Underline),
                                    start = start,
                                    end = end
                                )
                            }
                        }
                        "font" -> {
                            val colorAttr = node.attr("color")
                            var color: Color? = null
                            if (colorAttr.isNotEmpty()) {
                                color = try {
                                    if (colorAttr.startsWith("#")) {
                                        Color(colorAttr.removePrefix("#").toLong(16) or 0xFF000000)
                                    } else {
                                        // Handle simple named colors or fallback
                                        Color.Unspecified
                                    }
                                } catch (e: Exception) {
                                    Color.Unspecified
                                }
                            }
                            
                            val style = if (color != null && color != Color.Unspecified) {
                                SpanStyle(color = color)
                            } else {
                                SpanStyle()
                            }
                            
                            currentTextBuilder.withStyle(style) {
                                parseInlineChildren(node, currentTextBuilder)
                            }
                        }
                        "span" -> {
                            // Check background color or color in style
                            val styleAttr = node.attr("style")
                            var color: Color? = null
                            var bgColor: Color? = null
                            
                            if (styleAttr.contains("color:")) {
                                // Super simple regex extraction, mostly for inline styles
                                val match = Regex("color:\\s*#([0-9a-fA-F]{6})").find(styleAttr)
                                if (match != null) {
                                    color = Color(match.groupValues[1].toLong(16) or 0xFF000000)
                                }
                            }
                            if (styleAttr.contains("background-color:")) {
                                val match = Regex("background-color:\\s*#([0-9a-fA-F]{6})").find(styleAttr)
                                if (match != null) {
                                    bgColor = Color(match.groupValues[1].toLong(16) or 0xFF000000)
                                }
                            }
                            
                            val style = SpanStyle(
                                color = color ?: Color.Unspecified,
                                background = bgColor ?: Color.Unspecified
                            )
                            currentTextBuilder.withStyle(style) {
                                parseInlineChildren(node, currentTextBuilder)
                            }
                        }
                        else -> {
                            // Default to inline
                            parseInlineChildren(node, currentTextBuilder)
                        }
                    }
                }
            }
        }
        
        commitText()
        return blocks
    }

    private fun parseInlineChildren(element: Element, builder: AnnotatedString.Builder) {
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> builder.append(node.text())
                is Element -> {
                    when (node.tagName().lowercase()) {
                        "br" -> builder.append("\n")
                        "b", "strong" -> builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { parseInlineChildren(node, builder) }
                        "i", "em" -> builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { parseInlineChildren(node, builder) }
                        "u" -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { parseInlineChildren(node, builder) }
                        "s", "strike" -> builder.withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { parseInlineChildren(node, builder) }
                        "a" -> {
                            val href = node.attr("href")
                            val start = builder.length
                            parseInlineChildren(node, builder)
                            val end = builder.length
                            if (href.isNotBlank()) {
                                builder.addStringAnnotation("URL", href, start, end)
                                builder.addStyle(SpanStyle(color = Color(0xFF007BFF), textDecoration = TextDecoration.Underline), start, end)
                            }
                        }
                        "font" -> {
                            val colorAttr = node.attr("color")
                            var color: Color? = null
                            if (colorAttr.isNotEmpty()) {
                                color = try {
                                    if (colorAttr.startsWith("#")) {
                                        Color(colorAttr.removePrefix("#").toLong(16) or 0xFF000000)
                                    } else {
                                        Color.Unspecified
                                    }
                                } catch (e: Exception) { Color.Unspecified }
                            }
                            val style = if (color != null && color != Color.Unspecified) SpanStyle(color = color) else SpanStyle()
                            builder.withStyle(style) { parseInlineChildren(node, builder) }
                        }
                        "span" -> parseInlineChildren(node, builder) // simplified for recursion
                        else -> parseInlineChildren(node, builder)
                    }
                }
            }
        }
    }
}
