package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class IImageReaderScreen(
    private val tid: ThreadId,
    private val postId: PostId,
    private val fid: ForumId?,
    private val threadTitle: String,
    private val imageList: List<String>,
    private val initialPage: Int = 1,
    private val loadHistory: Boolean = false
) : Navigatable {
    override val id: String = "MangaReaderScreen_${tid}_${postId}"

    @Composable
    override fun Content() {
        ImagesReaderScreen(
            tid = tid,
            postId = postId,
            fid = fid,
            threadTitle = threadTitle,
            imageList = imageList,
            initialPage = initialPage,
            loadHistory = loadHistory
        )
    }
}