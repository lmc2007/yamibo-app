package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderTextSegmentationTest {
    @Test
    fun longAnnotatedTextIsSplitWithoutLosingTextOrStyles() {
        val source = AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("段落內容\n".repeat(2_000))
            pop()
        }.toAnnotatedString()
        val block = HtmlBlock.Text(annotatedString = source, anchorId = "source")

        val segments = splitLongReaderTextBlock(block)

        assertTrue(segments.size > 1)
        assertTrue(segments.all { it.annotatedString.length <= MAX_READER_TEXT_SEGMENT_CHARS })
        assertEquals(source.text, segments.joinToString(separator = "") { it.annotatedString.text })
        assertTrue(segments.all { it.annotatedString.spanStyles.isNotEmpty() })
        assertEquals(segments.size, segments.map { it.anchorId }.distinct().size)
    }

    @Test
    fun shortTextKeepsOriginalBlock() {
        val block = HtmlBlock.Text(AnnotatedString("短內容"), anchorId = "source")

        assertEquals(listOf(block), splitLongReaderTextBlock(block))
    }

    @Test
    fun longTextNestedInQuoteIsSplitIntoIndependentQuoteBlocks() {
        val text = "引文內容\n".repeat(2_000)
        val quote = HtmlBlock.Quote(
            contentBlocks = listOf(HtmlBlock.Text(AnnotatedString(text), anchorId = "text")),
            anchorId = "quote",
        )

        val segments = splitLongReaderBlock(quote)

        assertTrue(segments.size > 1)
        assertTrue(segments.all { it is HtmlBlock.Quote })
        assertEquals(
            text,
            segments.joinToString(separator = "") { segment ->
                (segment as HtmlBlock.Quote).contentBlocks.joinToString(separator = "") {
                    (it as HtmlBlock.Text).annotatedString.text
                }
            },
        )
        assertEquals(segments.size, segments.map { it.anchorId }.distinct().size)
    }
}
