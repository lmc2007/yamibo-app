package me.thenano.yamibo.yamibo_app.thread.novel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.Post
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** First floor body preview — scrollable with max half-screen height */
@Composable
internal fun FirstFloorPreview(post: Post) {
    val colors = YamiboTheme.colors

    /** Strip HTML tags for plain text display */
    val plainText =
        remember(post.contentHtml) {
            post.contentHtml
                /** Normalize: </div> → newline, <br> → newline */
                .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<br\\s*/?>\r?\n?", RegexOption.IGNORE_CASE), "\n")
                /** Remove all remaining HTML tags */
                .replace(Regex("<[^>]*>"), "")
                /** Decode HTML entities */
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                /** Collapse 2+ consecutive newlines into 1 (no blank lines) */
                .replace(Regex("(\r?\n){2,}"), "\n")
                .trim()
        }

    val maxPreviewHeight = 300.dp

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
        ) {
            val scrollState = rememberScrollState()

            Box(modifier = Modifier.heightIn(max = maxPreviewHeight)) {
                SelectionContainer {
                    Column(modifier = Modifier.verticalScroll(scrollState).padding(14.dp)) {
                        Text(
                            text = plainText,
                            fontSize = 14.sp,
                            color = colors.textDark.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        )
                    }
                }

                /** Scrollbar indicator */
                if (scrollState.maxValue > 0) {
                    val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue
                    val viewportFraction =
                        scrollState.viewportSize.toFloat() /
                            (scrollState.viewportSize + scrollState.maxValue).toFloat()
                    val thumbHeight = (viewportFraction * maxPreviewHeight.value).coerceAtLeast(24f)

                    Box(
                        modifier =
                            Modifier.align(Alignment.TopEnd)
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .width(3.dp)
                                .height(maxPreviewHeight)
                    ) {
                        Box(
                            modifier =
                                Modifier.align(Alignment.TopStart)
                                    .offset(
                                        y =
                                            ((maxPreviewHeight.value -
                                                thumbHeight) *
                                                scrollFraction)
                                                .dp
                                    )
                                    .width(3.dp)
                                    .height(thumbHeight.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(colors.brownPrimary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}
