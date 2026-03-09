package me.thenano.yamibo.yamibo_app.thread.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.value.PollOptionId
import me.thenano.yamibo.yamibo_app.thread.render.components.*
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import YamiboIcons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage

@Composable
@Suppress("AssignedValueIsNeverRead")
fun PostRenderer(
    post: Post,
    modifier: Modifier = Modifier,
    onVote: ((List<PollOptionId>) -> Unit)? = null,
    onRate: ((Int, String) -> Unit)? = null,
    onComment: ((String) -> Unit)? = null
) {
    var showRateDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }

    SelectionContainer {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Author Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatarUrl = post.author.avatarUrl
                // TODO: Long-press or tap to navigate to author profile (post.author.uid)
                Box(modifier = Modifier.clickable { /* TODO: navigate to user profile */ }) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        SubcomposeAsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = {
                                Icon(imageVector = YamiboIcons.PersonFill, contentDescription = null, modifier = Modifier.size(36.dp), tint = YamiboTheme.colors.textDark.copy(alpha = 0.5f))
                            },
                            loading = {
                                CircularProgressIndicator(
                                    color = YamiboTheme.colors.brownPrimary,
                                    modifier = Modifier.padding(8.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        )
                    } else {
                        Icon(imageVector = YamiboIcons.PersonFill, contentDescription = null, modifier = Modifier.size(36.dp), tint = YamiboTheme.colors.textDark.copy(alpha = 0.5f))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.author.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = YamiboTheme.colors.brownPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(post.timeText, fontSize = 12.sp, color = YamiboTheme.colors.textDark.copy(alpha = 0.5f))
                }
                
                Text("${post.floor}#", fontSize = 12.sp, color = YamiboTheme.colors.textDark.copy(alpha = 0.5f))
            }

            // Content HTML
            HtmlRenderer(post.contentHtml)

            // Edited Text
            if (!post.editedText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = post.editedText!!,
                    fontSize = 12.sp,
                    color = YamiboTheme.colors.textDark.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Poll
            post.poll?.let { poll ->
                PollRenderer(poll, onVote = onVote)
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

            // Action Buttons Row (Bottom)
            if (onRate != null || onComment != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = YamiboTheme.colors.brownPrimary.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRate != null) {
                        TextButton(onClick = { showRateDialog = true }) {
                            Icon(imageVector = YamiboIcons.Heart, contentDescription = "評分", modifier = Modifier.size(18.dp), tint = YamiboTheme.colors.brownPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("評分", fontSize = 13.sp, color = YamiboTheme.colors.brownPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (onComment != null) {
                        TextButton(onClick = { showCommentDialog = true }) {
                            Icon(imageVector = YamiboIcons.Comment, contentDescription = "點評", modifier = Modifier.size(18.dp), tint = YamiboTheme.colors.brownPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("點評", fontSize = 13.sp, color = YamiboTheme.colors.brownPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showRateDialog) {
        var scoreInput by remember { mutableStateOf("") }
        var reasonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                showRateDialog = false
            },
            title = { Text("本帖評分", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = { scoreInput = it },
                        label = { Text("分數", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YamiboTheme.colors.brownPrimary, focusedLabelColor = YamiboTheme.colors.brownPrimary)
                    )
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text("評分理由", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YamiboTheme.colors.brownPrimary, focusedLabelColor = YamiboTheme.colors.brownPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = scoreInput.toIntOrNull() ?: 0
                        onRate?.invoke(score, reasonInput)
                        showRateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YamiboTheme.colors.brownPrimary)
                ) { Text("提交", color = YamiboTheme.colors.creamBackground) }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) { Text("取消", color = YamiboTheme.colors.brownPrimary) }
            },
            containerColor = YamiboTheme.colors.creamSurface,
            titleContentColor = YamiboTheme.colors.brownPrimary,
            textContentColor = YamiboTheme.colors.textDark
        )
    }

    if (showCommentDialog) {
        var commentInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("新增點評", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    label = { Text("輸入內容", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YamiboTheme.colors.brownPrimary, focusedLabelColor = YamiboTheme.colors.brownPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onComment?.invoke(commentInput)
                        showCommentDialog = false
                    },
                    enabled = commentInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = YamiboTheme.colors.brownPrimary)
                ) { Text("提交", color = YamiboTheme.colors.creamBackground) }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) { Text("取消", color = YamiboTheme.colors.brownPrimary) }
            },
            containerColor = YamiboTheme.colors.creamSurface,
            titleContentColor = YamiboTheme.colors.brownPrimary,
            textContentColor = YamiboTheme.colors.textDark
        )
    }
}
