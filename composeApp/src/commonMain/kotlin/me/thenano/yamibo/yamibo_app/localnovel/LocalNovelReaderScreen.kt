package me.thenano.yamibo.yamibo_app.localnovel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.*
import me.thenano.yamibo.yamibo_app.thread.reader.components.novel.NovelReaderSettingsPanel
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.navigation.*
import me.thenano.yamibo.yamibo_app.repository.LocalNovelFileType
import me.thenano.yamibo.yamibo_app.repository.LocalNovelInfo
import me.thenano.yamibo.yamibo_app.repository.LocalNovelChapterInfo
import me.thenano.yamibo.yamibo_app.repository.localnovel.EpubFileParser
import me.thenano.yamibo.yamibo_app.repository.localnovel.TxtFileParser
import me.thenano.yamibo.yamibo_app.thread.reader.components.overlay.ReaderFloatButtons
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlRenderer
import me.thenano.yamibo.yamibo_app.thread.reader.MAX_READER_TEXT_SEGMENT_CHARS
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

@Serializable
private data class LocalNovelReaderRestorePayload(
    val novelId: Long,
)

@RestorableScreenEntry
class ILocalNovelReaderScreen(val novelId: Long) : RestorableNavigatable {
    override val id = buildId(novelId.toString())
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = LocalNovelReaderRestorePayload(novelId = novelId),
    )

    @Composable
    override fun Content() {
        LocalNovelReaderScreen(novelId)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ILocalNovelReaderScreen>(ILocalNovelReaderScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<LocalNovelReaderRestorePayload>(payload)
            return ILocalNovelReaderScreen(novelId = data.novelId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalNovelReaderScreen(novelId: Long) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val repository = LocalLocalNovelRepository.current
    val fileOps = LocalPlatformFileOperations.current
    val appSettingsRepo = LocalAppSettingsRepository.current
    val scope = rememberCoroutineScope()

    var novel by remember { mutableStateOf<LocalNovelInfo?>(null) }
    var chapters by remember { mutableStateOf<List<LocalNovelChapterInfo>>(emptyList()) }
    var currentChapterIndex by remember { mutableIntStateOf(0) }
    var chapterSegments by remember { mutableStateOf<List<String>>(emptyList()) }

    // UI state — mirrors ThreadReaderScreen's overlay pattern
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showMenu by remember { mutableStateOf(true) }
    var showSettingsPanel by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    fun loadChapter(index: Int) {
        scope.launch {
            val n = novel ?: return@launch
            val ch = chapters.getOrNull(index) ?: return@launch
            val segs = withContext(Dispatchers.Default) {
                buildChapterSegments(n, ch, fileOps)
            }
            chapterSegments = segs
            currentChapterIndex = index
            listState.scrollToItem(0)
            withContext(Dispatchers.Default) {
                repository.saveProgress(
                    me.thenano.yamibo.yamibo_app.repository.LocalNovelProgressInfo(
                        novelId = novelId, chapterId = ch.id, charOffset = 0,
                    )
                )
                repository.updateLastReadAt(novelId, currentTimeMillis())
            }
        }
    }

    LaunchedEffect(novelId) {
        val result = withContext(Dispatchers.Default) {
            val n = repository.getNovelById(novelId) ?: return@withContext null
            val chs = repository.getChaptersByNovelId(novelId)
            val progress = repository.getProgress(novelId)
            val startChapterIndex = if (progress != null && progress.chapterId > 0) {
                chs.indexOfFirst { it.id == progress.chapterId }.coerceAtLeast(0)
            } else 0
            Triple(n, chs, startChapterIndex)
        }
        if (result != null) {
            val (n, chs, startIdx) = result
            novel = n
            chapters = chs
            loadChapter(startIdx)
        }
    }

    // When settings panel closes, restore menu visibility
    LaunchedEffect(showSettingsPanel) {
        if (!showSettingsPanel) showMenu = true
        else showMenu = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChapterDrawerContent(
                    novelTitle = novel?.title ?: "",
                    chapters = chapters,
                    currentIndex = currentChapterIndex,
                    onChapterClick = { index ->
                        scope.launch {
                            drawerState.close()
                            if (index != currentChapterIndex) loadChapter(index)
                        }
                    },
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize().background(colors.creamBackground)) {
            if (novel == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.brownPrimary)
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    // Top bar — matches ThreadReaderScreen pattern
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.creamSurface)
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { navigator.pop() }) {
                            Text(i18n("返回"), color = colors.brownPrimary, fontSize = 14.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            chapters.getOrNull(currentChapterIndex)?.title ?: "",
                            color = colors.textDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text(i18n("目錄"), color = colors.brownPrimary, fontSize = 14.sp)
                        }
                    }

                    // Content
                    if (chapterSegments.isEmpty()) {
                        Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.brownPrimary)
                        }
                    } else {
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { showMenu = !showMenu }
                                },
                            state = listState,
                        ) {
                                itemsIndexed(chapterSegments) { index, htmlChunk ->
                                    HtmlRenderer(
                                        html = htmlChunk,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    )
                                    if (index < chapterSegments.lastIndex) {
                                        HorizontalDivider(
                                            color = colors.textDark.copy(alpha = 0.08f),
                                            thickness = 0.5.dp,
                                        )
                                    }
                                }
                                // Bottom spacer for navigation bar
                                item { Spacer(Modifier.height(56.dp)) }
                            }
                    }

                    // Bottom navigation bar
                    AnimatedVisibility(
                        visible = showMenu && !showSettingsPanel,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.creamSurface)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            TextButton(
                                onClick = {
                                    if (currentChapterIndex > 0) loadChapter(currentChapterIndex - 1)
                                },
                                enabled = currentChapterIndex > 0,
                            ) { Text(i18n("上一章")) }
                            Text(
                                "${currentChapterIndex + 1} / ${chapters.size}",
                                color = colors.textOnBackground,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            TextButton(
                                onClick = {
                                    if (currentChapterIndex < chapters.lastIndex) loadChapter(currentChapterIndex + 1)
                                },
                                enabled = currentChapterIndex < chapters.lastIndex,
                            ) { Text(i18n("下一章")) }
                        }
                    }
                }
            }

            // Settings panel — at Box level (not inside Column)
            if (showSettingsPanel) {
                // Dismiss overlay
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) { showSettingsPanel = false; showMenu = true }
                )
                NovelReaderSettingsPanel(
                    visible = showSettingsPanel,
                    appSettingsRepo = appSettingsRepo,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            // Floating buttons — bottom-end, same position as ThreadReaderScreen
            AnimatedVisibility(
                visible = showMenu && !showSettingsPanel,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 56.dp, end = 16.dp),
            ) {
                ReaderFloatButtons(
                    visible = true,
                    onRefresh = { loadChapter(currentChapterIndex) },
                    onSettings = {
                        showSettingsPanel = true
                        showMenu = false
                    },
                )
            }
        }
    }
}

// ---- Chapter drawer content ----

@Composable
private fun ChapterDrawerContent(
    novelTitle: String,
    chapters: List<LocalNovelChapterInfo>,
    currentIndex: Int,
    onChapterClick: (Int) -> Unit,
) {
    val colors = YamiboTheme.colors
    Column(Modifier.fillMaxWidth()) {
        // Drawer header
        Text(
            i18n("目錄"),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textDark,
            modifier = Modifier.padding(16.dp),
        )
        Text(
            novelTitle,
            fontSize = 14.sp,
            color = colors.textOnBackground,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
        )
        HorizontalDivider(color = colors.textDark.copy(alpha = 0.1f))

        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(chapters) { index, chapter ->
                Text(
                    chapter.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChapterClick(index) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    color = if (index == currentIndex) colors.brownPrimary else colors.textDark,
                    fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

// ---- Segment builders (unchanged) ----

private fun buildChapterSegments(
    novel: LocalNovelInfo,
    chapter: LocalNovelChapterInfo,
    fileOps: me.thenano.yamibo.yamibo_app.repository.localnovel.PlatformFileOperations,
): List<String> {
    val html = when (novel.fileType) {
        LocalNovelFileType.TXT -> {
            val text = TxtFileParser(fileOps).readChapterText(
                fileUri = novel.fileUri,
                encoding = novel.encoding,
                startOffset = chapter.startOffset,
                endOffset = chapter.endOffset,
            )
            textToHtml(text)
        }
        LocalNovelFileType.EPUB -> {
            EpubFileParser(fileOps).readChapterHtml(chapter.internalPath)
        }
    }
    return segmentHtml(html, MAX_READER_TEXT_SEGMENT_CHARS)
}

private fun textToHtml(text: String): String {
    val escaped = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    val lines = escaped.split("\n")
    return buildString {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                append("<br/>\n")
            } else {
                append("<p>")
                append(trimmed)
                append("</p>\n")
            }
        }
    }
}

private fun segmentHtml(html: String, maxChars: Int): List<String> {
    if (html.length <= maxChars) return listOf(html)

    val chunks = mutableListOf<String>()
    var remaining = html

    while (remaining.length > maxChars) {
        val searchStart = (maxChars * 2 / 3).coerceAtLeast(0)
        val searchEnd = maxChars
        var breakPoint = -1

        for (i in searchEnd downTo searchStart) {
            if (i + 4 <= remaining.length) {
                val window = remaining.substring(i, (i + 4).coerceAtMost(remaining.length))
                if (window.startsWith("<p>") || window.startsWith("<br") ||
                    window.startsWith("</p>") || window.startsWith("</d")
                ) {
                    breakPoint = i
                    break
                }
            }
        }

        if (breakPoint == -1) breakPoint = maxChars
        chunks.add(remaining.substring(0, breakPoint).trim())
        remaining = remaining.substring(breakPoint).trim()
    }

    if (remaining.isNotBlank()) chunks.add(remaining)
    return chunks.ifEmpty { listOf(html) }
}
