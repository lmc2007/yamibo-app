package me.thenano.yamibo.yamibo_app.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageRequestTest {
    @Test
    fun preservesLocalImageSchemes() {
        assertEquals("content://downloads/image.jpg", normalizeImageUrl("content://downloads/image.jpg"))
        assertEquals("file:///tmp/image.jpg", normalizeImageUrl("file:///tmp/image.jpg"))
        assertEquals(
            "content://downloads/image.jpg",
            normalizeImageUrl("https://bbs.yamibo.com/content://downloads/image.jpg"),
        )
    }
    @Test
    fun normalizesRelativeAttachmentUrl() {
        assertEquals(
            "https://bbs.yamibo.com/data/attachment/forum/example.png",
            normalizeImageUrl("data/attachment/forum/example.png"),
        )
        assertEquals(
            "https://bbs.yamibo.com/data/attachment/forum/example.png",
            normalizeImageUrl("/data/attachment/forum/example.png"),
        )
    }

    @Test
    fun preservesAbsoluteUrl() {
        assertEquals(
            "https://example.com/image.png",
            normalizeImageUrl("https://example.com/image.png"),
        )
    }
}
