package me.thenano.yamibo.yamibo_app.thread.reader

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

class IMangaReaderScreen(
    private val tid: ThreadId,
    private val threadTitle: String,
    private val imageList: List<String>
) : Navigatable {
    override val id: String = "MangaReaderScreen_$tid"

    @Composable
    override fun Content() {
        MangaReaderScreen(
            tid = tid,
            threadTitle = threadTitle,
            imageList = imageList
        )
    }
}