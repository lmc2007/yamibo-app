package me.thenano.yamibo.yamibo_app.repository.forumnovel

import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.PostImage
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForumNovelPackageTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun withResolvedImageUrlsRewritesImagesAndHtml() {
        val page = threadPage(
            posts = listOf(
                post(
                    images = listOf(PostImage("https://bbs.yamibo.com/data/1.jpg", "1.jpg")),
                    contentHtml = """<img src="https://bbs.yamibo.com/data/1.jpg">""",
                ),
            ),
        )

        val resolved = page.withResolvedImageUrls { source ->
            if (source == "https://bbs.yamibo.com/data/1.jpg") {
                "file:///tmp/images/0001.jpg"
            } else {
                null
            }
        }

        val post = resolved.posts.single()
        assertEquals("file:///tmp/images/0001.jpg", post.images.single().url)
        assertTrue("file:///tmp/images/0001.jpg" in post.contentHtml)
    }

    @Test
    fun manifestJsonRoundTrips() {
        val manifest = ForumNovelPackageManifest(
            tid = 123,
            authorId = 7,
            title = "测试小说",
            sourceTotalPages = 3,
            exportedAt = 42L,
            pages = listOf(1, 2),
            missingPages = listOf(3),
            images = listOf(ForumNovelPackageImage("https://bbs.yamibo.com/data/1.jpg", "0001.jpg")),
        )

        val decoded = json.decodeFromString<ForumNovelPackageManifest>(
            json.encodeToString(ForumNovelPackageManifest.serializer(), manifest),
        )

        assertEquals(manifest, decoded)
        assertEquals(FORUM_NOVEL_PACKAGE_TYPE, decoded.packageType)
    }

    private fun threadPage(posts: List<Post>): ThreadPage {
        val forum = ForumSummary(fid = ForumId(49), name = "文学区", url = "forumdisplay")
        return ThreadPage(
            thread = ThreadInfo(tid = ThreadId(123), title = "测试小说", forum = forum),
            posts = posts,
            pageNav = PageNav(currentPage = 1, totalPages = 2),
        )
    }

    private fun post(images: List<PostImage>, contentHtml: String): Post = Post(
        pid = PostId(1),
        floor = 1,
        title = "第一章",
        author = User(uid = UserId(1), name = "author"),
        timeCreate = TimeInfo(text = "2026-1-1 00:00", epoch = 0),
        contentHtml = contentHtml,
        images = images,
        tags = Tags(emptyList()),
        poll = null,
    )
}
