package me.thenano.yamibo.yamibo_app.thread.reader.components.post

import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlBlock
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlParserRubyTest {
    @Test
    fun rubyKeepsBaseTextAndRangeWithoutParenthesisFallback() {
        val blocks = HtmlParser.parseHtml(
            """
            <p>原文中使用了<ruby>性騷擾<rt>Sexual Harassment</rt></ruby>。</p>
            """.trimIndent(),
        )

        val textBlocks = blocks.filterIsInstance<HtmlBlock.Text>()

        assertTrue(textBlocks.isNotEmpty())
        assertEquals(
            "原文中使用了性騷擾。",
            textBlocks.joinToString(separator = "") { it.annotatedString.text }.trim(),
        )
        val ruby = textBlocks.single().rubies.single()
        assertEquals("性騷擾", ruby.baseText)
        assertEquals("Sexual Harassment", ruby.rubyText)
        assertEquals("原文中使用了".length, ruby.start)
        assertEquals("原文中使用了性騷擾".length, ruby.end)
    }

    @Test
    fun rubyParenthesisTagsAreNotDuplicated() {
        val blocks = HtmlParser.parseHtml("<ruby>帆華<rp>(</rp><rt>Hanaho</rt><rp>)</rp></ruby>")
        val text = blocks.filterIsInstance<HtmlBlock.Text>().joinToString(separator = "") { it.annotatedString.text }

        assertEquals("帆華", text)
        val ruby = blocks.filterIsInstance<HtmlBlock.Text>().single().rubies.single()
        assertEquals("帆華", ruby.baseText)
        assertEquals("Hanaho", ruby.rubyText)
    }

    @Test
    fun multipleRubiesKeepStableOffsets() {
        val blocks = HtmlParser.parseHtml("<p><ruby>性<rt>Sexual</rt></ruby>與<ruby>騷擾<rt>Harassment</rt></ruby></p>")
        val textBlock = blocks.filterIsInstance<HtmlBlock.Text>().single()

        assertEquals("性與騷擾", textBlock.annotatedString.text)
        assertEquals(listOf(0 to 1, 2 to 4), textBlock.rubies.map { it.start to it.end })
        assertEquals(listOf("Sexual", "Harassment"), textBlock.rubies.map { it.rubyText })
    }
}
