package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class IImageReaderScreen(
    private val tid: ThreadId,
    private val postId: PostId?,
    private val fid: ForumId?,
    private val threadTitle: String,
    private val imageList: List<String>,
    private val initialPage: Int = 1,
    private val loadHistory: Boolean = false,
    
    // Tag Manga Mode Fields:
    private val tagId: TagId? = null,
    private val tagName: String? = null,
    private val authorId: UserId? = null,
    private val tagThreads: List<ThreadSummary>? = null,
    private val tagPage: Int? = null,
    private val tagTotalPages: Int? = null,
) : Navigatable {
    override val id: String = buildId(tid, postId, tagId)

    @Composable
    override fun Content() {
        ImagesReaderScreen(
            tid = tid,
            postId = postId,
            fid = fid,
            threadTitle = threadTitle,
            imageList = imageList,
            initialPage = initialPage,
            loadHistory = loadHistory,
            tagId = tagId,
            tagName = tagName,
            authorId = authorId,
            tagThreads = tagThreads,
            tagPage = tagPage,
            tagTotalPages = tagTotalPages
        )
    }
}