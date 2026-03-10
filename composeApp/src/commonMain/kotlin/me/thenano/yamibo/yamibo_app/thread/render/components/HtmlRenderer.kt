package me.thenano.yamibo.yamibo_app.thread.render.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.image_icon
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.webview.IPlatformWebView

@Composable
fun HtmlRenderer(html: String, tid: ThreadId? = null, modifier: Modifier = Modifier) {
    val rawBlocks = remember(html) { HtmlParser.parseHtml(html) }
    // Filter out whitespace-only Text blocks that sit between images (produce extra blank lines)
    val blocks = remember(rawBlocks) {
        rawBlocks.filterIndexed { index, block ->
            if (block is HtmlBlock.Text) {
                val content = block.annotatedString.text.trim()
                if (content.isEmpty()) {
                    // Remove blank Text blocks that are sandwiched between or adjacent to Image blocks
                    val prev = rawBlocks.getOrNull(index - 1)
                    val next = rawBlocks.getOrNull(index + 1)
                    val adjImage = prev is HtmlBlock.Image || next is HtmlBlock.Image
                    return@filterIndexed !adjImage
                }
            }
            true
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        blocks.forEach { block ->
            HtmlBlockRenderer(block, tid)
        }
    }
}

@Composable
@Suppress("AssignedValueIsNeverRead")
private fun HtmlBlockRenderer(block: HtmlBlock, tid: ThreadId? = null) {
    val colors = YamiboTheme.colors
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.current

    when (block) {
        is HtmlBlock.Text -> {
            var showLongPressMenu by remember { mutableStateOf<Pair<String, String>?>(null) }
            val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
            val clipboardManager = LocalClipboardManager.current

            Text(
                text = block.annotatedString,
                style = TextStyle(
                    color = colors.textDark,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = block.textAlign
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp)
                    .pointerInput(block.annotatedString) {
                        awaitPointerEventScope {
                            while (true) {
                                // 1. Initial Pass: Intercept 'down' on links to disable global selection
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val down = event.changes.firstOrNull { it.changedToDown() }

                                if (down != null) {
                                    val layout = layoutResult.value
                                    if (layout != null) {
                                        val offset = layout.getOffsetForPosition(down.position)
                                        val hasLink = block.annotatedString.getStringAnnotations("URL", offset, offset)
                                            .isNotEmpty()
                                        if (hasLink) {
                                            // Consume down in Initial pass -> Parents/Selection internal won't see it (prevents selection)
                                            down.consume()

                                            // 2. Manual detection for this specific link tap/long press
                                            val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                                            var isLongPress = false

                                            // Manual wait for up or timeout
                                            val upOrNull = withTimeoutOrNull(longPressTimeout) {
                                                waitForUpOrCancellation()
                                            }

                                            if (upOrNull == null) {
                                                // Long press detected (timeout)
                                                isLongPress = true
                                                val link =
                                                    block.annotatedString.getStringAnnotations("URL", offset, offset)
                                                        .firstOrNull()
                                                if (link != null) {
                                                    showLongPressMenu = link.item to block.annotatedString.substring(
                                                        link.start,
                                                        link.end
                                                    )
                                                    // Consume all movement until release
                                                    while (true) {
                                                        val moveEvent = awaitPointerEvent()
                                                        moveEvent.changes.forEach { it.consume() }
                                                        if (moveEvent.changes.none { it.pressed }) break
                                                    }
                                                }
                                            } else {
                                                // Fast tap (up before timeout) - Do nothing to avoid mis-clicks
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                onTextLayout = { layoutResult.value = it }
            )


            if (showLongPressMenu != null) {
                DisableSelection {
                    val (url, linkText) = showLongPressMenu!!
                    val fullUrl = if (url.startsWith("http")) url else "https://bbs.yamibo.com/$url"

                    AlertDialog(
                        onDismissRequest = { showLongPressMenu = null },
                        title = { Text("連結選項", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Info Section
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                    Text(
                                        text = linkText,
                                        color = colors.textDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullUrl,
                                        color = colors.textDark.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                                
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    color = colors.brownPrimary.copy(alpha = 0.1f)
                                )

                                TextButton(onClick = {
                                    navigator.navigate(IPlatformWebView(fullUrl))
                                    showLongPressMenu = null
                                }) { Text("在應用內開啟連結", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fullUrl))
                                    showLongPressMenu = null
                                }) { Text("複製連結地址", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(linkText))
                                    showLongPressMenu = null
                                }) { Text("複製連結文字", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    try {
                                        uriHandler.openUri(fullUrl)
                                    } catch (_: Exception) {
                                    }
                                    showLongPressMenu = null
                                }) { Text("使用外部瀏覽器開啟", color = colors.brownPrimary, fontSize = 16.sp) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showLongPressMenu = null }) {
                                Text("取消", color = colors.textDark.copy(alpha = 0.5f))
                            }
                        },
                        containerColor = colors.creamSurface,
                        titleContentColor = colors.textDark
                    )
                }
            }
        }

        is HtmlBlock.Image -> {
            var retryKey by remember { mutableStateOf(0) }
            val url = if (block.url.startsWith("http")) block.url else "https://bbs.yamibo.com/${block.url}"

            val context = LocalPlatformContext.current
            val authRepo = LocalAuthRepository.current

            val cookie = authRepo.cookieStore.load() ?: ""
            val referer = if (tid != null) {
                YamiboRoute.Thread(tid).build()
            } else {
                "https://bbs.yamibo.com/"
            }

            val imageRequest = remember(url, tid, cookie, retryKey) {
                val builder = ImageRequest.Builder(context)
                    .data(url)
                    .httpHeaders(
                        NetworkHeaders.Builder()
                            .add("Cookie", cookie)
                            .add("Referer", referer)
                            .build()
                    )
                    .crossfade(true)

                if (retryKey > 0) {
                    builder.memoryCachePolicy(coil3.request.CachePolicy.READ_ONLY)
                        .diskCachePolicy(coil3.request.CachePolicy.READ_ONLY)
                }
                builder.build()
            }

            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = block.alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                contentScale = ContentScale.FillWidth
            ) {
                val state by painter.state.collectAsState()
                when (state) {
                    is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.brownPrimary)
                        }
                    }

                    is AsyncImagePainter.State.Error -> {
                        val errorState = state as AsyncImagePainter.State.Error
                        val errorMsg = errorState.result.throwable.message ?: "Unknown Error"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF3F3F3), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.image_icon),
                                contentDescription = "Image Load Failed",
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "載入失敗: $errorMsg",
                                color = colors.textDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                url,
                                color = colors.brownDeep,
                                fontSize = 10.sp,
                                style = TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { retryKey++ },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brownPrimary),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp)
                            ) {
                                Text("重新載入", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }
        }

        is HtmlBlock.Collapse -> {
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
                onClick = { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text(
                        text = if (expanded) "▼ ${block.title ?: "點擊展開 / 收起"}" else "▶ ${block.title ?: "點擊展開 / 收起"}",
                        color = colors.brownDeep,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(
                                Modifier.padding(bottom = 12.dp),
                                color = colors.brownPrimary.copy(alpha = 0.1f)
                            )
                            block.contentBlocks.forEach { HtmlBlockRenderer(it, tid) }
                        }
                    }
                }
            }
        }

        is HtmlBlock.Locked -> {
            val dashPattern = remember { PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFFD32F2F),
                            style = Stroke(width = 4f, pathEffect = dashPattern),
                            cornerRadius = CornerRadius(24f, 24f)
                        )
                    }
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔒",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "本帖隱藏內容需要積分: ${block.cost}",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    block.contentBlocks.forEach { HtmlBlockRenderer(it, tid) }
                }
            }
        }

        is HtmlBlock.Quote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.creamSurface)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(colors.brownPrimary)
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    block.contentBlocks.forEach { HtmlBlockRenderer(it) }
                }
            }
        }

        is HtmlBlock.Code -> {
            Surface(
                color = Color(0xFF2B2B2B), // very dark gray for code
                contentColor = Color(0xFFA9B7C6), // standard light text
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Text(
                    text = block.codeText,
                    modifier = Modifier.padding(12.dp),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
            }
        }
    }
}
