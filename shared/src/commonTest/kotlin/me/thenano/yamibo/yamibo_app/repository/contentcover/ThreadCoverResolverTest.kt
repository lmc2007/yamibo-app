package me.thenano.yamibo.yamibo_app.repository.contentcover

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.Tags
import io.github.littlesurvival.dto.model.TimeInfo
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.page.PostImage
import io.github.littlesurvival.dto.page.RatePopoutPage
import io.github.littlesurvival.dto.page.ThreadInfo
import io.github.littlesurvival.dto.page.ThreadPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository

class ThreadCoverResolverTest {
    @Test
    fun findsOwnersLaterPostAndIgnoresOtherAuthorsImage() = runBlocking {
        val tid = ThreadId(10)
        val owner = User(UserId(7), "owner")
        val other = User(UserId(8), "other")
        val firstPage = page(
            tid,
            posts = listOf(
                post(1, owner),
                post(2, other, "https://example.com/wrong.jpg"),
            ),
            totalPages = 2,
        )
        val ownerPageOne = page(tid, listOf(post(1, owner)), totalPages = 2)
        val ownerPageTwo = page(
            tid,
            listOf(post(3, owner, "data/attachment/forum/correct.jpg")),
            totalPages = 2,
        )
        val repository = FakeThreadRepository(
            mapOf(
                Key(1, null) to firstPage,
                Key(1, 7) to ownerPageOne,
                Key(2, 7) to ownerPageTwo,
            )
        )

        assertEquals(
            "https://bbs.yamibo.com/data/attachment/forum/correct.jpg",
            ThreadCoverResolver(repository).resolve(tid),
        )
    }

    private fun page(tid: ThreadId, posts: List<Post>, totalPages: Int): ThreadPage =
        ThreadPage(
            thread = ThreadInfo(
                tid = tid,
                title = "title",
                forum = ForumSummary(ForumId(1), "forum", "forum.php?fid=1"),
            ),
            posts = posts,
            pageNav = PageNav(totalPages = totalPages),
        )

    private fun post(floor: Int, author: User, image: String? = null): Post =
        Post(
            pid = PostId(floor),
            floor = floor,
            title = "",
            author = author,
            timeCreate = TimeInfo("2026-01-01", null, 0),
            contentHtml = "",
            images = image?.let { listOf(PostImage(it)) }.orEmpty(),
            tags = Tags(),
            poll = null,
        )

    private data class Key(val page: Int, val authorId: Int?)

    private class FakeThreadRepository(
        private val pages: Map<Key, ThreadPage>,
    ) : ThreadRepository {
        override suspend fun fetchThread(
            tid: ThreadId,
            authorId: UserId?,
            page: Int,
            reverse: Boolean,
        ): YamiboResult<ThreadPage> = YamiboResult.Success(
            pages.getValue(Key(page, authorId?.value)),
        )

        override fun getCachedThread(tid: ThreadId, authorId: UserId?, page: Int): ThreadPage? = null
        override fun setCachedThread(tid: ThreadId, authorId: UserId?, page: Int, threadPage: ThreadPage) = Unit
        override fun clearCachedThread(tid: ThreadId) = Unit
        override suspend fun fetchFindPost(tid: ThreadId, postId: PostId, authorId: UserId?) = error("unused")
        override suspend fun addFavorite(tid: ThreadId, formHash: FormHash) = error("unused")
        override suspend fun votePoll(fId: ForumId, tId: ThreadId, pollOptionIds: List<PollOptionId>, formHash: FormHash) = error("unused")
        override suspend fun fetchRatePopoutPage(tId: ThreadId, pId: PostId): YamiboResult<RatePopoutPage> = error("unused")
        override suspend fun ratePost(tId: ThreadId, pId: PostId, score: Int, reason: String, formHash: FormHash, noticeAuthor: Boolean) = error("unused")
        override suspend fun commentPost(tId: ThreadId, pId: PostId, message: String, formHash: FormHash) = error("unused")
    }
}
