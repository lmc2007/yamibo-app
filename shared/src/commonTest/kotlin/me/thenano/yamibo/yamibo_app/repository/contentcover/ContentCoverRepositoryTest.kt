package me.thenano.yamibo.yamibo_app.repository.contentcover

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import me.thenano.yamibo.yamibo_app.repository.ContentCoverRepository

class ContentCoverRepositoryTest {
    private val key = ContentCoverRepository.Key(
        ContentCoverRepository.TargetType.TagManga,
        18666L,
    )

    @Test
    fun dynamicCoverPrefersAutomaticCover() {
        val cover = ContentCoverRepository.Cover(
            key = key,
            automaticCoverUrl = "https://example.com/automatic.jpg",
            manualCoverUrl = "https://example.com/manual.jpg",
            dynamicEnabled = true,
            updatedAt = 1L,
        )

        assertEquals("https://example.com/automatic.jpg", cover.resolvedUrl)
    }

    @Test
    fun disabledDynamicCoverPrefersManualCover() {
        val cover = ContentCoverRepository.Cover(
            key = key,
            automaticCoverUrl = "https://example.com/automatic.jpg",
            manualCoverUrl = "https://example.com/manual.jpg",
            dynamicEnabled = false,
            updatedAt = 1L,
        )

        assertEquals("https://example.com/manual.jpg", cover.resolvedUrl)
    }

    @Test
    fun missingPreferredCoverFallsBackWithoutClearing() {
        val dynamic = ContentCoverRepository.Cover(key, null, "manual.jpg", true, 1L)
        val manual = ContentCoverRepository.Cover(key, "automatic.jpg", null, false, 1L)

        assertEquals("manual.jpg", dynamic.resolvedUrl)
        assertEquals("automatic.jpg", manual.resolvedUrl)
    }

    @Test
    fun normalizesRelativeCoverUrls() {
        assertEquals(
            "https://bbs.yamibo.com/data/attachment/forum/cover.jpg",
            normalizeCoverUrl("data/attachment/forum/cover.jpg"),
        )
        assertEquals("https://cdn.example.com/cover.jpg", normalizeCoverUrl("//cdn.example.com/cover.jpg"))
        assertEquals("content://downloads/cover.jpg", normalizeCoverUrl("content://downloads/cover.jpg"))
        assertEquals("file:///tmp/cover.jpg", normalizeCoverUrl("file:///tmp/cover.jpg"))
        assertEquals(
            "content://downloads/cover.jpg",
            normalizeCoverUrl("https://bbs.yamibo.com/content://downloads/cover.jpg"),
        )
    }

    @Test
    fun rejectsNonContentImages() {
        assertNull(normalizeCoverUrl("data:image/png;base64,abc"))
        assertNull(normalizeCoverUrl("static/image/smiley/default/1.gif"))
        assertNull(normalizeCoverUrl("https://bbs.yamibo.com/uc_server/avatar/face/1.jpg"))
        assertNull(normalizeCoverUrl("none.gif"))
    }
}
