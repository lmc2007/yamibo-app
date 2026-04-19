package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.thread.image.ImageViewer
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
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
                    val adjacentVisualBlock =
                        prev is HtmlBlock.Image || next is HtmlBlock.Image ||
                            prev is HtmlBlock.Attachment || next is HtmlBlock.Attachment
                    return@filterIndexed !adjacentVisualBlock
                }
            }
            true
        }
    }
    SelectionContainer {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            blocks.forEach { block ->
                HtmlBlockRenderer(block, tid)
            }
        }
    }
}

@Composable
@Suppress("AssignedValueIsNeverRead")
private fun HtmlBlockRenderer(block: HtmlBlock, tid: ThreadId? = null) {
    val colors = YamiboTheme.colors
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.current
    
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()
    val lineSpacing = novelSettingsRepo.lineSpacing.state()
    val clipboardManager = LocalClipboardManager.current
    val isDarkTheme = (colors.creamBackground.red + colors.creamBackground.green + colors.creamBackground.blue) < 1.5f

    val adjustAnnotatedString: @Composable (AnnotatedString) -> AnnotatedString = { input ->
        remember(input, isDarkTheme) {
            if (!isDarkTheme) {
                input
            } else {
                val newBuilder = AnnotatedString.Builder(input.text)
                input.spanStyles.forEach { range ->
                    var style = range.item
                    if (style.color != Color.Unspecified) {
                        if (style.color.red <= 0.2f && style.color.green <= 0.2f && style.color.blue <= 0.2f && style.color.alpha > 0f) {
                            style = style.copy(color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                    newBuilder.addStyle(style, range.start, range.end)
                }
                input.paragraphStyles.forEach { newBuilder.addStyle(it.item, it.start, it.end) }
                input.getStringAnnotations("URL", 0, input.length).forEach { 
                    newBuilder.addStringAnnotation(it.tag, it.item, it.start, it.end) 
                }
                newBuilder.toAnnotatedString()
            }
        }
    }

    when (block) {
        is HtmlBlock.Text -> {
            var showLongPressMenu by remember { mutableStateOf<Pair<String, String>?>(null) }
            val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
            val adjustedAnnotatedString = adjustAnnotatedString(block.annotatedString)

            Text(
                text = adjustedAnnotatedString,
                style = TextStyle(
                    color = colors.htmlTextDark,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * lineSpacing).sp,
                    textAlign = block.textAlign
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp)
                    .pointerInput(adjustedAnnotatedString) {
                        awaitPointerEventScope {
                            while (true) {
                                // 1. Initial Pass: Intercept 'down' on links to disable global selection
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val down = event.changes.firstOrNull { it.changedToDown() }

                                if (down != null) {
                                    val layout = layoutResult.value
                                    if (layout != null) {
                                        val offset = layout.getOffsetForPosition(down.position)
                                        val hasLink = adjustedAnnotatedString.getStringAnnotations("URL", offset, offset)
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
                                                    adjustedAnnotatedString.getStringAnnotations("URL", offset, offset)
                                                        .firstOrNull()
                                                if (link != null) {
                                                    showLongPressMenu = link.item to adjustedAnnotatedString.substring(
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
                                        color = colors.htmlTextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullUrl,
                                        color = colors.htmlTextDark.copy(alpha = 0.6f),
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
                                    clipboardManager.setText(AnnotatedString(fullUrl))
                                    showLongPressMenu = null
                                }) { Text("複製連結地址", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(linkText))
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
            val url = if (block.url.startsWith("http")) block.url else "https://bbs.yamibo.com/${block.url}"
            ImageViewer(
                url = url,
                contentDescription = block.alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                contentScale = ContentScale.FillWidth,
                enableContextMenu = true,
                isDarkTheme = false
            )
        }

        is HtmlBlock.Attachment -> {
            val fullUrl = if (block.url.startsWith("http")) block.url else "https://bbs.yamibo.com/${block.url.removePrefix("/")}"
            val iconUrl = block.iconUrl?.let {
                if (it.startsWith("http")) it else "https://bbs.yamibo.com/${it.removePrefix("/")}"
            }
            var showAttachmentMenu by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        showAttachmentMenu = true
                    },
                shape = RoundedCornerShape(10.dp),
                color = colors.creamSurface,
                border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.12f)),
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.creamBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (iconUrl != null) {
                            AsyncImage(
                                model = rememberImageRequest(iconUrl, enableCrossfade = false),
                                contentDescription = block.fileName,
                                modifier = Modifier.size(30.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Text(
                                text = "?"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = block.fileName,
                            color = colors.brownDeep,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        block.uploadInfo?.let {
                            Text(
                                text = it,
                                color = colors.htmlTextDark.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }

                        block.statInfo?.let {
                            Text(
                                text = it,
                                color = colors.htmlTextDark.copy(alpha = 0.72f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                }
            }

            if (showAttachmentMenu) {
                DisableSelection {
                    AlertDialog(
                        onDismissRequest = { showAttachmentMenu = false },
                        title = { Text("附件選項", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                    Text(
                                        text = block.fileName,
                                        color = colors.htmlTextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullUrl,
                                        color = colors.htmlTextDark.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                    block.uploadInfo?.let {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = it,
                                            color = colors.htmlTextDark.copy(alpha = 0.72f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    block.statInfo?.let {
                                        Text(
                                            text = it,
                                            color = colors.htmlTextDark.copy(alpha = 0.72f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    color = colors.brownPrimary.copy(alpha = 0.1f)
                                )

                                TextButton(onClick = {
                                    navigator.navigate(IPlatformWebView(fullUrl))
                                    showAttachmentMenu = false
                                }) { Text("在應用內開啟連結", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(fullUrl))
                                    showAttachmentMenu = false
                                }) { Text("複製連結地址", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(block.fileName))
                                    showAttachmentMenu = false
                                }) { Text("複製連結文字", color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    try {
                                        uriHandler.openUri(fullUrl)
                                    } catch (_: Exception) {
                                    }
                                    showAttachmentMenu = false
                                }) { Text("使用外部瀏覽器開啟", color = colors.brownPrimary, fontSize = 16.sp) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showAttachmentMenu = false }) {
                                Text("取消", color = colors.textDark.copy(alpha = 0.5f))
                            }
                        },
                        containerColor = colors.creamSurface,
                        titleContentColor = colors.textDark
                    )
                }
            }
        }

        is HtmlBlock.Hr -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 1.dp,
                color = colors.brownPrimary.copy(alpha = 0.3f)
            )
        }

        is HtmlBlock.Collapse -> {
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.2f)),
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

        is HtmlBlock.Table -> {
            val rows = block.rows
            if (rows.isEmpty()) return

            val maxCols = rows.maxOf { it.cells.size }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
            ) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    Column {
                        rows.forEachIndexed { rowIdx, row ->
                            val isFirstRow = rowIdx == 0
                            val isHeaderRow = isFirstRow && row.cells.any { it.isHeader }

                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (colIdx in 0 until maxCols) {
                                    val cell = row.cells.getOrNull(colIdx)
                                    val cellBg = when {
                                        isHeaderRow -> colors.brownDeep
                                        rowIdx % 2 == 0 -> colors.creamSurface
                                        else -> colors.creamBackground
                                    }
                                    val cellTextColor = if (isHeaderRow) Color.White else colors.htmlTextDark

                                    Box(
                                        modifier = Modifier
                                            .widthIn(min = 80.dp, max = 240.dp)
                                            .background(cellBg)
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        if (cell != null && cell.blocks.isNotEmpty()) {
                                            Column {
                                                cell.blocks.forEach { innerBlock ->
                                                    when (innerBlock) {
                                                        is HtmlBlock.Text -> {
                                                            Text(
                                                                text = adjustAnnotatedString(innerBlock.annotatedString),
                                                                style = TextStyle(
                                                                    color = cellTextColor,
                                                                    fontSize = (fontSize - 3).coerceAtLeast(10).sp,
                                                                    lineHeight = ((fontSize - 3).coerceAtLeast(10) * lineSpacing).sp,
                                                                    fontWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal,
                                                                    textAlign = innerBlock.textAlign
                                                                )
                                                            )
                                                        }
                                                        else -> HtmlBlockRenderer(innerBlock, tid)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Column divider
                                    if (colIdx < maxCols - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .heightIn(min = 36.dp)
                                                .background(colors.brownPrimary.copy(alpha = 0.15f))
                                        )
                                    }
                                }
                            }

                            // Row divider
                            if (rowIdx < rows.size - 1) {
                                HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }
    }
}
