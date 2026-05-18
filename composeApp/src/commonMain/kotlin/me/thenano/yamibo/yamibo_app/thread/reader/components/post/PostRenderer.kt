package me.thenano.yamibo.yamibo_app.thread.reader.components.post

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import io.github.littlesurvival.dto.page.Post
import io.github.littlesurvival.dto.value.PollOptionId
import me.thenano.yamibo.yamibo_app.components.rememberConvertedText
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkContext
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.reader.debug.DebugRecomposeProbe
import me.thenano.yamibo.yamibo_app.thread.reader.debug.debugPerfLog
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.*
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.userspace.IUserSpaceScreen

@Composable
fun PostRenderer(
    post: Post,
    modifier: Modifier = Modifier,
    threadTitle: String? = null,
    onVote: (suspend (List<PollOptionId>) -> Boolean)? = null,
    onRate: ((Int, String) -> Unit)? = null,
    onComment: ((String) -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    cachedHeightPx: Int? = null,
    onHeightChanged: ((Int) -> Unit)? = null,
    onImageSuccess: ((String) -> Unit)? = null,
    onImageError: ((String, String) -> Unit)? = null,
    onImageReload: ((String) -> Unit)? = null,
    imageErrorMessageFor: ((String) -> String?)? = null,
    imageRetryKeyFor: ((String) -> Int)? = null,
    imageCachedHeightFor: ((String) -> Int?)? = null,
    imagePlaceholderAspectRatioFor: ((String) -> Float?)? = null,
    onImageHeightChanged: ((String, Int) -> Unit)? = null,
    onImageAspectRatioChanged: ((String, Float) -> Unit)? = null,
    bodyBlocks: List<HtmlBlock>? = null,
    showHeader: Boolean = true,
    showFooter: Boolean = true,
    verticalPadding: Dp = 8.dp,
    totalViews: Int? = null,
    totalReplies: Int? = null,
    linkContext: InAppLinkContext = InAppLinkContext(),
) {
    DebugRecomposeProbe("PostRenderer", "${post.pid.value}#${post.floor}")

    var showRateDialog by remember { mutableStateOf(false) }
    var showCommentDialog by remember { mutableStateOf(false) }
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val contentWidthFraction = novelSettingsRepo.contentWidthFraction.state()
    val density = LocalDensity.current
    val cachedMinHeight = remember(cachedHeightPx, density) {
        cachedHeightPx?.let { with(density) { it.toDp() } } ?: 0.dp
    }

    val heightTrackingModifier = if (cachedHeightPx != null || onHeightChanged != null) {
        Modifier
            .heightIn(min = cachedMinHeight)
            .onSizeChanged { size ->
                if (size.height > 0) {
                    debugPerfLog("post_height|pid=${post.pid.value}|floor=${post.floor}|height=${size.height}")
                    onHeightChanged?.invoke(size.height)
                }
            }
    } else {
        Modifier
    }
    val blocksToRender = bodyBlocks
    val convertedThreadTitle = if (threadTitle != null) rememberConvertedText(threadTitle) else null
    if (!showHeader && !showFooter && !blocksToRender.isNullOrEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(heightTrackingModifier),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(contentWidthFraction)
                    .padding(horizontal = 16.dp, vertical = verticalPadding)
            ) {
                HtmlBlocksRenderer(
                    blocks = blocksToRender,
                    linkContext = linkContext,
                    onImageSuccess = onImageSuccess,
                    onImageError = onImageError,
                    onImageReload = onImageReload,
                    imageErrorMessageFor = imageErrorMessageFor,
                    imageRetryKeyFor = imageRetryKeyFor,
                    imageCachedHeightFor = imageCachedHeightFor,
                    imagePlaceholderAspectRatioFor = imagePlaceholderAspectRatioFor,
                    onImageHeightChanged = onImageHeightChanged,
                    onImageAspectRatioChanged = onImageAspectRatioChanged,
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(heightTrackingModifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxWidth(contentWidthFraction).padding(horizontal = 16.dp, vertical = verticalPadding)) {
            if (showHeader) {
                // Title
                if (post.floor == 1 && !convertedThreadTitle.isNullOrEmpty()) {
                    Text(
                        text = convertedThreadTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = YamiboTheme.colors.textDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (totalViews != null || totalReplies != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            totalViews?.let {
                                ThreadReaderStatBadge(icon = YamiboIcons.Views, value = it.toString())
                            }
                            if (totalViews != null && totalReplies != null) Spacer(Modifier.width(10.dp))
                            totalReplies?.let {
                                ThreadReaderStatBadge(icon = YamiboIcons.Comment, value = it.toString())
                            }
                        }
                    }
                }

                // Author Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarUrl = post.author.avatarUrl
                    Box(
                        modifier = Modifier.clickable {
                            navigator.navigate(IUserSpaceScreen(post.author.uid, post.author.name))
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = YamiboIcons.PersonFill,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = YamiboTheme.colors.textDark.copy(alpha = 0.5f)
                            )
                            if (!avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = rememberImageRequest(url = avatarUrl, enableCrossfade = false),
                                    contentDescription = "Avatar",
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            post.author.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = YamiboTheme.colors.brownPrimary,
                            modifier = Modifier.clickable {
                                navigator.navigate(IUserSpaceScreen(post.author.uid, post.author.name))
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(post.timeCreate.text, fontSize = 12.sp, color = YamiboTheme.colors.textDark.copy(alpha = 0.5f))
                    }

                    Text("${post.floor}#", fontSize = 12.sp, color = YamiboTheme.colors.textDark.copy(alpha = 0.5f))
                }
            }

            if (blocksToRender != null) {
                HtmlBlocksRenderer(
                    blocks = blocksToRender,
                    linkContext = linkContext,
                    onImageSuccess = onImageSuccess,
                    onImageError = onImageError,
                    onImageReload = onImageReload,
                    imageErrorMessageFor = imageErrorMessageFor,
                    imageRetryKeyFor = imageRetryKeyFor,
                    imageCachedHeightFor = imageCachedHeightFor,
                    imagePlaceholderAspectRatioFor = imagePlaceholderAspectRatioFor,
                    onImageHeightChanged = onImageHeightChanged,
                    onImageAspectRatioChanged = onImageAspectRatioChanged,
                )
            } else {
                HtmlRenderer(
                    html = post.contentHtml,
                    linkContext = linkContext,
                    onImageSuccess = onImageSuccess,
                    onImageError = onImageError,
                    onImageReload = onImageReload,
                    imageErrorMessageFor = imageErrorMessageFor,
                    imageRetryKeyFor = imageRetryKeyFor,
                    imageCachedHeightFor = imageCachedHeightFor,
                    imagePlaceholderAspectRatioFor = imagePlaceholderAspectRatioFor,
                    onImageHeightChanged = onImageHeightChanged,
                    onImageAspectRatioChanged = onImageAspectRatioChanged,
                )
            }

            if (showFooter) {
                // Edited Text
                val lastEditedTime = post.lastEditedTime
                if (lastEditedTime != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = lastEditedTime.specialText ?: appString(Res.string.thread_post_last_edited, lastEditedTime.text),
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
                if (onRate != null || onComment != null || onReply != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = YamiboTheme.colors.brownPrimary.copy(alpha = 0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onRate != null) {
                            TextButton(onClick = { showRateDialog = true }) {
                                Icon(imageVector = YamiboIcons.Heart, contentDescription = appString(Res.string.ui_score), modifier = Modifier.size(18.dp), tint = YamiboTheme.colors.brownPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appString(Res.string.ui_score), fontSize = 13.sp, color = YamiboTheme.colors.brownPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (onComment != null) {
                            TextButton(onClick = { showCommentDialog = true }) {
                                Icon(imageVector = YamiboIcons.Comment, contentDescription = appString(Res.string.ui_comment_remark), modifier = Modifier.size(18.dp), tint = YamiboTheme.colors.brownPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appString(Res.string.ui_comment_remark), fontSize = 13.sp, color = YamiboTheme.colors.brownPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (onReply != null) {
                            TextButton(onClick = { onReply() }) {
                                Icon(imageVector = YamiboIcons.Reply, contentDescription = appString(Res.string.ui_reply), modifier = Modifier.size(18.dp), tint = YamiboTheme.colors.brownPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appString(Res.string.ui_reply), fontSize = 13.sp, color = YamiboTheme.colors.brownPrimary, fontWeight = FontWeight.SemiBold)
                            }
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
            title = { Text(appString(Res.string.ui_rate_post), fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = { scoreInput = it },
                        label = { Text(appString(Res.string.ui_fraction), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YamiboTheme.colors.brownPrimary, focusedLabelColor = YamiboTheme.colors.brownPrimary)
                    )
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        label = { Text(appString(Res.string.ui_reason_for_rating), fontSize = 12.sp) },
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
                ) { Text(appString(Res.string.ui_submit), color = YamiboTheme.colors.creamBackground) }
            },
            dismissButton = {
                TextButton(onClick = { showRateDialog = false }) { Text(appString(Res.string.common_cancel), color = YamiboTheme.colors.brownPrimary) }
            },
            containerColor = YamiboTheme.colors.creamSurface,
            titleContentColor = YamiboTheme.colors.brownPrimary,
            textContentColor = YamiboTheme.colors.textDark
        )
    }

    if (showCommentDialog) {
        var commentInput by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCommentDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = YamiboTheme.colors.creamSurface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = appString(Res.string.ui_comment_remark),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = YamiboTheme.colors.brownPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                YamiboTheme.colors.creamBackground.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(
                                color = YamiboTheme.colors.textDark,
                                fontSize = 16.sp
                            ),
                            cursorBrush = SolidColor(YamiboTheme.colors.brownPrimary)
                        )
                        if (commentInput.isEmpty()) {
                            Text(
                                appString(Res.string.ui_enter_content),
                                color = YamiboTheme.colors.textDark.copy(alpha = 0.4f),
                                fontSize = 16.sp,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            onComment?.invoke(commentInput)
                            showCommentDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YamiboTheme.colors.brownDeep,
                            disabledContainerColor = YamiboTheme.colors.brownDeep.copy(alpha = 0.5f)
                        ),
                        enabled = commentInput.isNotBlank()
                    ) {
                        Text(appString(Res.string.ui_release), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadReaderStatBadge(icon: ImageVector, value: String) {
    val colors = YamiboTheme.colors
    Surface(shape = RoundedCornerShape(12.dp), color = colors.orangeAccent.copy(alpha = 0.15f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.brownDeep,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                color = colors.brownDeep,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


