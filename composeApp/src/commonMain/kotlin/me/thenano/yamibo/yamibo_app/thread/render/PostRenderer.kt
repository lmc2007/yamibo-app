package me.thenano.yamibo.yamibo_app.thread.render

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.littlesurvival.dto.page.Post
import me.thenano.yamibo.yamibo_app.thread.render.components.AttachmentRenderer
import me.thenano.yamibo.yamibo_app.thread.render.components.CommentRenderer
import me.thenano.yamibo.yamibo_app.thread.render.components.HtmlRenderer
import me.thenano.yamibo.yamibo_app.thread.render.components.PollRenderer
import me.thenano.yamibo.yamibo_app.thread.render.components.RateRenderer

@Composable
fun PostRenderer(post: Post, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Content HTML
        HtmlRenderer(post.contentHtml)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Poll
        post.poll?.let { poll ->
            PollRenderer(poll)
        }
        
        // Rates
        if (post.rateBlock.rates.isNotEmpty()) {
            RateRenderer(post.rateBlock)
        }
        
        // Comments
        if (post.comments.isNotEmpty()) {
            CommentRenderer(post.comments)
        }
        
        // Attachments
        if (post.attachments.isNotEmpty()) {
            AttachmentRenderer(post.attachments)
        }
    }
}
