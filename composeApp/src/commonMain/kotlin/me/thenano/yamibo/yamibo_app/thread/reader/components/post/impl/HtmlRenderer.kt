package me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.littlesurvival.dto.value.ThreadId
import me.thenano.yamibo.yamibo_app.LocalFontRepository
import me.thenano.yamibo.yamibo_app.LocalNovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.components.text.rememberConvertedText
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.navigation.IInAppLinkResolvingScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.looksLikeSupportedYamiboInAppLink
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.InAppLinkContext
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.image.ImageViewer
import me.thenano.yamibo.yamibo_app.thread.reader.debug.DebugRecomposeProbe
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.webview.IPlatformWebView

internal fun normalizeHtmlBlocks(rawBlocks: List<HtmlBlock>): List<HtmlBlock> {
    val maxMergedTextLength = 320
    val filteredBlocks = rawBlocks.filterIndexed { index, block ->
        if (block is HtmlBlock.Text) {
            val content = block.annotatedString.text.trim()
            if (content.isEmpty()) {
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

    val mergedBlocks = mutableListOf<HtmlBlock>()
    var pendingTextBuilder: AnnotatedString.Builder? = null
    var pendingTextAlign: TextAlign? = null
    var pendingTextAnchorId: String? = null
    val pendingRubies = mutableListOf<HtmlBlock.RubyText>()

    fun flushPendingText() {
        val builder = pendingTextBuilder ?: return
        val text = builder.toAnnotatedString()
        if (text.isNotEmpty()) {
            mergedBlocks += HtmlBlock.Text(
                annotatedString = text,
                textAlign = pendingTextAlign ?: TextAlign.Start,
                rubies = pendingRubies.toList(),
                anchorId = pendingTextAnchorId.orEmpty(),
            )
        }
        pendingTextBuilder = null
        pendingTextAlign = null
        pendingTextAnchorId = null
        pendingRubies.clear()
    }

    fun appendTextBlock(block: HtmlBlock.Text) {
        val builder = pendingTextBuilder
        val pendingLength = builder?.length ?: 0
        val shouldSplitLongText = builder != null &&
            pendingLength > 0 &&
            pendingLength + block.annotatedString.length > maxMergedTextLength
        if (builder == null || pendingTextAlign != block.textAlign || shouldSplitLongText) {
            flushPendingText()
            pendingTextBuilder = AnnotatedString.Builder()
            pendingTextAlign = block.textAlign
            pendingTextAnchorId = block.anchorId
        } else {
            val currentText = builder.toAnnotatedString().text
            val nextText = block.annotatedString.text
            if (currentText.isNotEmpty() && currentText.last() != '\n' && nextText.firstOrNull() != '\n') {
                builder.append("\n")
            }
        }
        pendingTextBuilder?.append(block.annotatedString)
        pendingRubies += block.rubies
    }

    fun splitLongTextBlock(block: HtmlBlock.Text): List<HtmlBlock.Text> {
        if (block.annotatedString.length <= maxMergedTextLength) return listOf(block)

        val chunks = mutableListOf<HtmlBlock.Text>()
        val text = block.annotatedString.text
        var start = 0
        while (start < text.length) {
            val preferredEnd = (start + maxMergedTextLength).coerceAtMost(text.length)
            val end = if (preferredEnd < text.length) {
                val newline = text.lastIndexOf('\n', preferredEnd)
                if (newline > start + maxMergedTextLength / 3) newline + 1 else preferredEnd
            } else {
                preferredEnd
            }
            val chunk = block.annotatedString.subSequence(start, end)
            if (chunk.isNotEmpty()) {
                chunks += block.copy(
                    annotatedString = chunk,
                    anchorId = if (start == 0) block.anchorId else "${block.anchorId}-$start",
                )
            }
            start = end
        }
        return chunks
    }

    filteredBlocks.forEach { block ->
        if (block is HtmlBlock.Text) {
            splitLongTextBlock(block).forEach(::appendTextBlock)
        } else {
            flushPendingText()
            mergedBlocks += block
        }
    }
    flushPendingText()

    return mergedBlocks
}

private data class LinkMenuState(
    val url: String,
    val linkText: String,
    val canOpenInApp: Boolean,
)

private fun normalizeYamiboLink(url: String): String {
    val cleaned = url.trim().replace("&amp;", "&")
    return when {
        cleaned.startsWith("http://") || cleaned.startsWith("https://") -> cleaned
        cleaned.startsWith("//") -> "https:$cleaned"
        cleaned.startsWith("/") -> "https://bbs.yamibo.com$cleaned"
        else -> "https://bbs.yamibo.com/$cleaned"
    }
}

private fun applyThemedLinkStyle(text: AnnotatedString, linkColor: Color): AnnotatedString {
    val links = text.getStringAnnotations("URL", 0, text.length)
    if (links.isEmpty()) return text
    val builder = AnnotatedString.Builder(text)
    links.forEach { link ->
        builder.addStyle(
            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
            link.start,
            link.end,
        )
    }
    return builder.toAnnotatedString()
}

private fun htmlTextLineHeightSp(
    baseFontSizeSp: Float,
    lineSpacing: Float,
    text: AnnotatedString,
    hasRuby: Boolean = false,
): Float {
    val largestSpanFontSizeSp = text.spanStyles
        .mapNotNull { it.item.fontSize.toAbsoluteSpOrNull(baseFontSizeSp) }
        .maxOrNull()
        ?: baseFontSizeSp
    val largestFontSizeSp = maxOf(baseFontSizeSp, largestSpanFontSizeSp)
    val configuredLineHeightSp = baseFontSizeSp * lineSpacing
    val requiredLineHeightSp = largestFontSizeSp * maxOf(lineSpacing, 1.2f)
    val rubyLineHeightSp = if (hasRuby) largestFontSizeSp * maxOf(lineSpacing, 2.15f) else 0f
    return maxOf(configuredLineHeightSp, requiredLineHeightSp, rubyLineHeightSp)
}

private fun TextUnit.toAbsoluteSpOrNull(baseFontSizeSp: Float): Float? {
    return when (type) {
        TextUnitType.Sp -> value
        TextUnitType.Em -> value * baseFontSizeSp
        else -> null
    }
}

private fun String.isHtmlBlankText(): Boolean {
    return all { it.isWhitespace() || it == '\u3000' }
}

@Composable
fun HtmlRenderer(
    html: String,
    tid: ThreadId? = null,
    linkContext: InAppLinkContext = InAppLinkContext(currentTid = tid),
    modifier: Modifier = Modifier,
    onImageSuccess: ((String) -> Unit)? = null,
    onImageError: ((String, String) -> Unit)? = null,
    onImageReload: ((String) -> Unit)? = null,
    imageErrorMessageFor: ((String) -> String?)? = null,
    imageRetryKeyFor: ((String) -> Int)? = null,
    imageCachedHeightFor: ((String) -> Int?)? = null,
    imagePlaceholderAspectRatioFor: ((String) -> Float?)? = null,
    onImageHeightChanged: ((String, Int) -> Unit)? = null,
    onImageAspectRatioChanged: ((String, Float) -> Unit)? = null,
) {
    DebugRecomposeProbe("HtmlRenderer", html.hashCode().toString())
    val convertedHtml = rememberConvertedText(html)
    val rawBlocks = remember(convertedHtml) { HtmlParser.parseHtml(convertedHtml) }
    val blocks = remember(rawBlocks) { normalizeHtmlBlocks(rawBlocks) }
    HtmlBlocksRenderer(
        blocks = blocks,
        tid = tid,
        linkContext = linkContext,
        modifier = modifier,
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

@Composable
fun HtmlBlocksRenderer(
    blocks: List<HtmlBlock>,
    tid: ThreadId? = null,
    linkContext: InAppLinkContext = InAppLinkContext(currentTid = tid),
    modifier: Modifier = Modifier,
    onImageSuccess: ((String) -> Unit)? = null,
    onImageError: ((String, String) -> Unit)? = null,
    onImageReload: ((String) -> Unit)? = null,
    imageErrorMessageFor: ((String) -> String?)? = null,
    imageRetryKeyFor: ((String) -> Int)? = null,
    imageCachedHeightFor: ((String) -> Int?)? = null,
    imagePlaceholderAspectRatioFor: ((String) -> Float?)? = null,
    onImageHeightChanged: ((String, Int) -> Unit)? = null,
    onImageAspectRatioChanged: ((String, Float) -> Unit)? = null,
) {
    val hasSelectableText = remember(blocks) {
        blocks.any { it is HtmlBlock.Text }
    }
    val content: @Composable () -> Unit = {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
            blocks.forEach { block ->
                HtmlBlockRenderer(
                    block = block,
                    tid = tid,
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
    }
    if (hasSelectableText) {
        SelectionContainer {
            content()
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RubyTextBlock(
    text: AnnotatedString,
    rubies: List<HtmlBlock.RubyText>,
    textAlign: TextAlign,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    lineHeightSp: Float,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val segments = remember(text, rubies) {
        buildRubySegments(text, rubies)
    }
    val lines = remember(segments) {
        buildRubyLines(segments)
    }
    val horizontalArrangement = when (textAlign) {
        TextAlign.Center -> Arrangement.Center
        TextAlign.Right, TextAlign.End -> Arrangement.End
        else -> Arrangement.Start
    }

    Column(modifier = modifier) {
        lines.forEach { line ->
            if (line.hasRuby) {
                RubyFlowLine(
                    segments = line.segments,
                    horizontalArrangement = horizontalArrangement,
                    fontFamily = fontFamily,
                    fontSizeSp = fontSizeSp,
                    lineHeightSp = lineHeightSp,
                    textColor = textColor,
                )
            } else {
                line.segments.filterIsInstance<RubySegment.Text>().forEach { segment ->
                    Text(
                        text = segment.text,
                        color = textColor,
                        fontFamily = fontFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeightSp.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RubyFlowLine(
    segments: List<RubySegment>,
    horizontalArrangement: Arrangement.Horizontal,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    lineHeightSp: Float,
    textColor: Color,
) {
    FlowRow(
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        segments.forEach { segment ->
            when (segment) {
                is RubySegment.Text -> {
                    if (segment.text.isNotEmpty()) {
                        RubyPlainTextSegment(
                            text = segment.text,
                            fontFamily = fontFamily,
                            fontSizeSp = fontSizeSp,
                            lineHeightSp = lineHeightSp,
                            textColor = textColor,
                        )
                    }
                }

                is RubySegment.Ruby -> {
                    RubySegmentView(
                        ruby = segment.ruby,
                        fontFamily = fontFamily,
                        fontSizeSp = fontSizeSp,
                        lineHeightSp = lineHeightSp,
                        textColor = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun RubyPlainTextSegment(
    text: AnnotatedString,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    lineHeightSp: Float,
    textColor: Color,
) {
    val density = LocalDensity.current
    val baseTop = with(density) { (fontSizeSp * 0.82f).sp.toDp() }
    val segmentHeight = with(density) { lineHeightSp.sp.toDp() }
    Box(modifier = Modifier.height(segmentHeight)) {
        Text(
            text = text,
            color = textColor,
            fontFamily = fontFamily,
            fontSize = fontSizeSp.sp,
            lineHeight = fontSizeSp.sp,
            modifier = Modifier.offset(y = baseTop),
        )
    }
}

@Composable
private fun RubySegmentView(
    ruby: HtmlBlock.RubyText,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    lineHeightSp: Float,
    textColor: Color,
) {
    val rubyFontSizeSp = fontSizeSp * 0.72f
    val density = LocalDensity.current
    val baseTop = with(density) { (fontSizeSp * 0.82f).sp.toDp() }
    val segmentHeight = with(density) { lineHeightSp.sp.toDp() }
    Box(
        modifier = Modifier
            .height(segmentHeight)
            .padding(horizontal = 1.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = ruby.rubyText,
            color = textColor,
            fontFamily = fontFamily,
            fontSize = rubyFontSizeSp.sp,
            lineHeight = rubyFontSizeSp.sp,
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Text(
            text = ruby.baseText,
            color = textColor,
            fontFamily = fontFamily,
            fontSize = fontSizeSp.sp,
            lineHeight = fontSizeSp.sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = baseTop),
        )
    }
}

private sealed class RubySegment {
    data class Text(val text: AnnotatedString) : RubySegment()
    data class Ruby(val ruby: HtmlBlock.RubyText) : RubySegment()
}

private data class RubyLine(
    val segments: List<RubySegment>,
    val hasRuby: Boolean,
)

private fun buildRubyLines(segments: List<RubySegment>): List<RubyLine> {
    val lines = mutableListOf<RubyLine>()
    var current = mutableListOf<RubySegment>()

    fun flush() {
        lines += RubyLine(current.toList(), current.any { it is RubySegment.Ruby })
        current = mutableListOf()
    }

    segments.forEach { segment ->
        when (segment) {
            is RubySegment.Ruby -> current += segment
            is RubySegment.Text -> {
                var start = 0
                val source = segment.text.text
                source.forEachIndexed { index, char ->
                    if (char == '\n') {
                        if (start < index) {
                            current += RubySegment.Text(segment.text.subSequence(start, index))
                        }
                        flush()
                        start = index + 1
                    }
                }
                if (start < source.length) {
                    current += RubySegment.Text(segment.text.subSequence(start, source.length))
                }
            }
        }
    }
    if (current.isNotEmpty() || lines.isEmpty()) {
        flush()
    }
    return lines
}

private fun buildRubySegments(
    text: AnnotatedString,
    rubies: List<HtmlBlock.RubyText>,
): List<RubySegment> {
    val segments = mutableListOf<RubySegment>()
    var cursor = 0
    val source = text.text

    if (source.any { it == '\uFFFC' }) {
        var rubyIndex = 0
        var start = 0
        source.forEachIndexed { index, char ->
            if (char == '\uFFFC') {
                if (start < index) {
                    segments += RubySegment.Text(text.subSequence(start, index))
                }
                rubies.getOrNull(rubyIndex)?.let { segments += RubySegment.Ruby(it) }
                rubyIndex += 1
                start = index + 1
            }
        }
        if (start < source.length) {
            segments += RubySegment.Text(text.subSequence(start, source.length))
        }
        return segments
    }

    rubies.forEach { ruby ->
        val index = source.indexOf(ruby.baseText, startIndex = cursor)
        if (index < 0) return@forEach
        if (cursor < index) {
            segments += RubySegment.Text(text.subSequence(cursor, index))
        }
        segments += RubySegment.Ruby(ruby)
        cursor = index + ruby.baseText.length
    }
    if (cursor < source.length) {
        segments += RubySegment.Text(text.subSequence(cursor, source.length))
    }
    return segments
}

@Composable
private fun HtmlBlockRenderer(
    block: HtmlBlock,
    tid: ThreadId? = null,
    linkContext: InAppLinkContext = InAppLinkContext(currentTid = tid),
    onImageSuccess: ((String) -> Unit)? = null,
    onImageError: ((String, String) -> Unit)? = null,
    onImageReload: ((String) -> Unit)? = null,
    imageErrorMessageFor: ((String) -> String?)? = null,
    imageRetryKeyFor: ((String) -> Int)? = null,
    imageCachedHeightFor: ((String) -> Int?)? = null,
    imagePlaceholderAspectRatioFor: ((String) -> Float?)? = null,
    onImageHeightChanged: ((String, Int) -> Unit)? = null,
    onImageAspectRatioChanged: ((String, Float) -> Unit)? = null,
) {
    DebugRecomposeProbe("HtmlBlockRenderer", "${block::class.simpleName}:${block.hashCode()}")
    val colors = YamiboTheme.colors
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.current
    
    val novelSettingsRepo = LocalNovelReaderSettingsRepository.current
    val fontRepository = LocalFontRepository.current
    val fontSize = novelSettingsRepo.fontSize.state()
    val lineSpacing = novelSettingsRepo.lineSpacing.state()
    val readerFontId = novelSettingsRepo.readerFontId.state()
    val fontList by fontRepository.fonts.collectAsState()
    val readerFontFamily = remember(readerFontId, fontList) {
        fontRepository.getReaderFontFamily() ?: HtmlDefaultFontFamily
    }
    @Suppress("DEPRECATION") val clipboardManager = LocalClipboardManager.current
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
                input.getStringAnnotations(0, input.length).forEach {
                    newBuilder.addStringAnnotation(it.tag, it.item, it.start, it.end) 
                }
                newBuilder.toAnnotatedString()
            }
        }
    }

    when (block) {
        is HtmlBlock.Text -> {
            var showLongPressMenu by remember { mutableStateOf<LinkMenuState?>(null) }
            val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
            val baseAdjustedAnnotatedString = adjustAnnotatedString(block.annotatedString)
            val adjustedAnnotatedString = remember(baseAdjustedAnnotatedString, colors.htmlTextDark) {
                applyThemedLinkStyle(baseAdjustedAnnotatedString, colors.htmlTextDark)
            }
            val lineHeightSp = remember(adjustedAnnotatedString, fontSize, lineSpacing) {
                htmlTextLineHeightSp(
                    baseFontSizeSp = fontSize.toFloat(),
                    lineSpacing = lineSpacing,
                    text = adjustedAnnotatedString,
                    hasRuby = block.rubies.isNotEmpty(),
                )
            }
            val isBlankText = remember(adjustedAnnotatedString) {
                adjustedAnnotatedString.text.isHtmlBlankText()
            }
            val inlineContent = remember { emptyMap<String, InlineTextContent>() }
            val hasLinks = remember(adjustedAnnotatedString) {
                adjustedAnnotatedString.getStringAnnotations("URL", 0, adjustedAnnotatedString.length).isNotEmpty()
            }
            val textModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .then(
                    if (hasLinks) {
                        Modifier.pointerInput(adjustedAnnotatedString, linkContext) {
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

                                                // Manual wait for up or timeout
                                                val upOrNull = withTimeoutOrNull(longPressTimeout) {
                                                    waitForUpOrCancellation()
                                                }

                                                if (upOrNull == null) {
                                                    val link =
                                                        adjustedAnnotatedString.getStringAnnotations("URL", offset, offset)
                                                            .firstOrNull()
                                                    if (link != null) {
                                                        val fullUrl = normalizeYamiboLink(link.item)
                                                        val linkText = adjustedAnnotatedString.substring(link.start, link.end)
                                                        showLongPressMenu = LinkMenuState(
                                                            url = fullUrl,
                                                            linkText = linkText,
                                                            canOpenInApp = looksLikeSupportedYamiboInAppLink(fullUrl),
                                                        )
                                                        // Consume all movement until release
                                                        while (true) {
                                                            val moveEvent = awaitPointerEvent()
                                                            moveEvent.changes.forEach { it.consume() }
                                                            if (moveEvent.changes.none { it.pressed }) break
                                                        }
                                                    }
                                                } else {
                                                    val link =
                                                        adjustedAnnotatedString.getStringAnnotations("URL", offset, offset)
                                                            .firstOrNull()
                                                    if (link != null) {
                                                        val fullUrl = normalizeYamiboLink(link.item)
                                                        if (looksLikeSupportedYamiboInAppLink(fullUrl)) {
                                                            navigator.navigate(IInAppLinkResolvingScreen(fullUrl, linkContext))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )

            if (isBlankText) {
                val density = LocalDensity.current
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { lineHeightSp.sp.toDp() })
                )
            } else if (block.rubies.isNotEmpty()) {
                RubyTextBlock(
                    text = adjustedAnnotatedString,
                    rubies = block.rubies,
                    textAlign = block.textAlign,
                    fontFamily = readerFontFamily,
                    fontSizeSp = fontSize.toFloat(),
                    lineHeightSp = lineHeightSp,
                    textColor = colors.htmlTextDark,
                    modifier = textModifier,
                )
            } else {
                Text(
                    text = adjustedAnnotatedString,
                    style = TextStyle(
                        color = colors.htmlTextDark,
                        fontFamily = readerFontFamily,
                        fontSize = fontSize.sp,
                        lineHeight = lineHeightSp.sp,
                        textAlign = block.textAlign
                    ),
                    modifier = textModifier,
                    inlineContent = inlineContent,
                    onTextLayout = { layoutResult.value = it }
                )
            }

            if (!isBlankText && showLongPressMenu != null) {
                DisableSelection {
                    val menu = showLongPressMenu!!
                    val fullUrl = menu.url

                    AlertDialog(
                        onDismissRequest = { showLongPressMenu = null },
                        title = { Text(i18n("連結選項"), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                    Text(
                                        text = menu.linkText,
                                        color = colors.htmlTextDark,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = fullUrl,
                                        color = colors.htmlTextDark.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    color = colors.brownPrimary.copy(alpha = 0.1f),
                                )

                                if (menu.canOpenInApp) {
                                    TextButton(onClick = {
                                        navigator.navigate(IInAppLinkResolvingScreen(fullUrl, linkContext))
                                        showLongPressMenu = null
                                    }) { Text(i18n("App 內打開"), color = colors.brownPrimary, fontSize = 16.sp) }
                                }

                                TextButton(onClick = {
                                    navigator.navigate(IPlatformWebView(fullUrl))
                                    showLongPressMenu = null
                                }) { Text(i18n("WebView 打開"), color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(fullUrl))
                                    showLongPressMenu = null
                                }) { Text(i18n("複製連結"), color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(menu.linkText))
                                    showLongPressMenu = null
                                }) { Text(i18n("複製文字"), color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    try {
                                        uriHandler.openUri(fullUrl)
                                    } catch (_: Exception) {
                                    }
                                    showLongPressMenu = null
                                }) { Text(i18n("外部瀏覽器"), color = colors.brownPrimary, fontSize = 16.sp) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showLongPressMenu = null }) {
                                Text(i18n("取消"), color = colors.textDark.copy(alpha = 0.5f))
                            }
                        },
                        containerColor = colors.creamSurface,
                        titleContentColor = colors.textDark,
                    )
                }
            }
        }

        is HtmlBlock.Image -> {
            val url = if (block.url.startsWith("http")) block.url else "https://bbs.yamibo.com/${block.url}"
            if (block.isEmoticon) {
                AsyncImage(
                    model = rememberImageRequest(url, enableCrossfade = false),
                    contentDescription = block.alt,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .size(40.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                ImageViewer(
                    url = url,
                    contentDescription = block.alt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    contentScale = ContentScale.FillWidth,
                    enableContextMenu = true,
                    isDarkTheme = false,
                    enableCrossfade = false,
                    onSuccess = onImageSuccess,
                    onError = onImageError,
                    blockedErrorMessage = imageErrorMessageFor?.invoke(url),
                    externalRetryKey = imageRetryKeyFor?.invoke(url) ?: 0,
                    onReload = { onImageReload?.invoke(url) },
                    cachedHeightPx = imageCachedHeightFor?.invoke(url),
                    placeholderAspectRatio = imagePlaceholderAspectRatioFor?.invoke(url),
                    onRenderedHeightChanged = { heightPx -> onImageHeightChanged?.invoke(url, heightPx) },
                    onRenderedAspectRatioChanged = { ratio -> onImageAspectRatioChanged?.invoke(url, ratio) },
                )
            }
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
                        title = { Text(i18n("附件選項"), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) },
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
                                }) { Text(i18n("在應用內開啟連結"), color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(fullUrl))
                                    showAttachmentMenu = false
                                }) { Text(i18n("複製連結地址"), color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(block.fileName))
                                    showAttachmentMenu = false
                                }) { Text(i18n("複製連結文字"), color = colors.brownPrimary, fontSize = 16.sp) }

                                TextButton(onClick = {
                                    try {
                                        uriHandler.openUri(fullUrl)
                                    } catch (_: Exception) {
                                    }
                                    showAttachmentMenu = false
                                }) { Text(i18n("使用外部瀏覽器開啟"), color = colors.brownPrimary, fontSize = 16.sp) }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showAttachmentMenu = false }) {
                                Text(i18n("取消"), color = colors.textDark.copy(alpha = 0.5f))
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
                        text = if (expanded) "▼ ${block.title ?: i18n("點擊展開 / 收起")}" else "▶ ${block.title ?: i18n("點擊展開 / 收起")}",
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
                            block.contentBlocks.forEach {
                                HtmlBlockRenderer(
                                    block = it,
                                    tid = tid,
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
                            text = i18n("本帖隱藏內容需要積分: {}", block.cost.toString()),
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    block.contentBlocks.forEach {
                        HtmlBlockRenderer(
                            block = it,
                            tid = tid,
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
                    block.contentBlocks.forEach {
                        HtmlBlockRenderer(
                            block = it,
                            tid = tid,
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
            val useLazyRows = rows.size > 40

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
            ) {
                val scrollState = rememberScrollState()
                val renderRow: @Composable (Int, HtmlBlock.TableRow) -> Unit = { rowIdx, row ->
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
                                                    val tableFontSize = (fontSize - 3).coerceAtLeast(10)
                                                    val adjustedTableText = adjustAnnotatedString(innerBlock.annotatedString)
                                                    val tableLineHeightSp = htmlTextLineHeightSp(
                                                        baseFontSizeSp = tableFontSize.toFloat(),
                                                        lineSpacing = lineSpacing,
                                                        text = adjustedTableText,
                                                    )
                                                    Text(
                                                        text = adjustedTableText,
                                                        style = TextStyle(
                                                            color = cellTextColor,
                                                            fontFamily = readerFontFamily,
                                                            fontSize = tableFontSize.sp,
                                                            lineHeight = tableLineHeightSp.sp,
                                                            fontWeight = if (cell.isHeader) FontWeight.Bold else FontWeight.Normal,
                                                            textAlign = innerBlock.textAlign
                                                        )
                                                    )
                                                }
                                                else -> HtmlBlockRenderer(
                                                    block = innerBlock,
                                                    tid = tid,
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
                                    }
                                }
                            }

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

                    if (rowIdx < rows.size - 1) {
                        HorizontalDivider(color = colors.brownPrimary.copy(alpha = 0.15f))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    if (useLazyRows) {
                        LazyColumn(modifier = Modifier.heightIn(max = 560.dp)) {
                            itemsIndexed(rows) { rowIdx, row -> renderRow(rowIdx, row) }
                        }
                    } else {
                        Column {
                            rows.forEachIndexed { rowIdx, row -> renderRow(rowIdx, row) }
                        }
                    }
                }
            }
        }
    }
}
