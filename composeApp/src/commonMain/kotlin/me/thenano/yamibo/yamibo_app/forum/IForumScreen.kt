package me.thenano.yamibo.yamibo_app.forum

import androidx.compose.runtime.Composable
import io.github.littlesurvival.dto.value.ForumId
import me.thenano.yamibo.yamibo_app.navigation.Navigatable

/** Navigatable screen for a specific forum page. */
class IForumScreen(
    private val fid: ForumId,
    private val name: String
) : Navigatable {
    override val id = "ForumScreen_${fid.value}"

    @Composable
    override fun Content() {
        ForumPageScreen(fid = fid, name = name)
    }
}
