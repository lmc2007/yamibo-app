package me.thenano.yamibo.yamibo_app.thread.detail.novel.components

import Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.Post
import me.thenano.yamibo.yamibo_app.__info__tag
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** First floor body preview — scrollable with max half-screen height */
@Composable
internal fun FirstFloorPreview(post: Post) {
    val colors = YamiboTheme.colors

    /** Strip HTML tags for plain text display */
    val plainText = remember(post.contentHtml) {
        post.contentHtml.replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<br\\s*/?>\r?\n?", RegexOption.IGNORE_CASE), "\n").replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"")
            /** collapse blank lines (including spaces) */
            .replace(Regex("\\n\\s*\\n+"), "\n").trim()
    }
    Log.i(__info__tag("FirstFloorPreview"), "FirstFloorPreview:\n$plainText")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
        ) {
            val scrollState = rememberScrollState()
            var expanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = if (expanded) 350.dp else 160.dp)
                        .clipToBounds()
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState, enabled = expanded)
                                .padding(
                                    top = 14.dp,
                                    start = 14.dp,
                                    end = 14.dp,
                                    bottom = if (expanded) 14.dp else 40.dp
                                )
                        ) {
                            Text(
                                text = plainText,
                                fontSize = 14.sp,
                                color = colors.textDark.copy(alpha = 0.85f),
                                lineHeight = 22.sp
                            )
                        }
                    }
                    if (!expanded) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, colors.creamSurface)
                                    )
                                )
                        )
                    }

                    /** Custom Scrollbar indicator */
                    if (expanded && scrollState.maxValue > 0) {
                        val scrollFraction = scrollState.value.toFloat() / scrollState.maxValue
                        val viewportFraction = scrollState.viewportSize.toFloat() / (scrollState.viewportSize + scrollState.maxValue).toFloat()
                        
                        val availableTrackHeight = 350f - 28f // 350dp total max height minus 14dp top + 14dp bottom approx
                        val thumbHeight = (viewportFraction * availableTrackHeight).coerceAtLeast(24f)

                        Box(
                            modifier = Modifier.align(Alignment.CenterEnd)
                                .padding(vertical = 14.dp, horizontal = 4.dp)
                                .width(3.dp).fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier.align(Alignment.TopStart).offset(
                                    y = ((availableTrackHeight - thumbHeight) * scrollFraction).dp
                                ).width(3.dp).height(thumbHeight.dp).clip(RoundedCornerShape(1.5.dp))
                                    .background(colors.brownPrimary.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { expanded = !expanded }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (expanded) "⌃" else "⌄",
                        color = colors.brownPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}