package me.thenano.yamibo.yamibo_app.localnovel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.*
import me.thenano.yamibo.yamibo_app.components.controls.YamiboActionChip
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import YamiboIcons
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.navigation.*
import me.thenano.yamibo.yamibo_app.repository.LocalNovelInfo
import me.thenano.yamibo.yamibo_app.repository.LocalNovelFileType
import me.thenano.yamibo.yamibo_app.repository.localnovel.LocalNovelFileHandle
import me.thenano.yamibo.yamibo_app.repository.localnovel.rememberLocalNovelFilePicker
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis

@Serializable
private data class LocalNovelBookshelfRestorePayload(
    val placeholder: Boolean = true,
)

@RestorableScreenEntry
class ILocalNovelBookshelfScreen : RestorableNavigatable {
    override val id = buildId()
    override val restoreDecoder = Decoder
    override fun toRestoreSnapshot(): RestorableScreenSnapshot =
        emptyRestoreSnapshot(restoreDecoder)

    @Composable
    override fun Content() {
        LocalNovelBookshelfScreen()
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ILocalNovelBookshelfScreen>(ILocalNovelBookshelfScreen::class) {
        override fun decode(payload: String): RestorableNavigatable = ILocalNovelBookshelfScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalNovelBookshelfScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val repository = LocalLocalNovelRepository.current
    val fileOps = LocalPlatformFileOperations.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var novels by remember { mutableStateOf<List<LocalNovelInfo>>(emptyList()) }
    var progressMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var showDeleteConfirm by remember { mutableStateOf<LocalNovelInfo?>(null) }
    var manageMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                val list = repository.getAllNovels()
                val map = buildProgressMap(list, repository)
                list to map
            }
            novels = result.first
            progressMap = result.second
        }
    }

    val pickFile = rememberLocalNovelFilePicker { handle ->
        scope.launch {
            importNovel(handle, repository, fileOps, snackbarHostState, navigator)
            reload()
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(i18n("本地小說"), color = colors.textDark) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.creamBackground),
                navigationIcon = {
                    TextButton(onClick = {
                        if (manageMode) {
                            manageMode = false
                            selectedIds = emptySet()
                        } else {
                            navigator.pop()
                        }
                    }) {
                        Text(if (manageMode) i18n("取消") else i18n("返回"), color = colors.brownPrimary)
                    }
                },
                actions = {
                    if (novels.isNotEmpty()) {
                        TextButton(onClick = {
                            manageMode = !manageMode
                            selectedIds = emptySet()
                        }) {
                            Text(
                                if (manageMode) i18n("完成") else i18n("管理"),
                                color = colors.brownPrimary,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { me.thenano.yamibo.yamibo_app.components.theme.YamiboSnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        if (novels.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        i18n("還沒有本地小說"),
                        color = colors.textOnBackground,
                        fontSize = 16.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    YamiboActionChip(
                        text = i18n("導入小說"),
                        onClick = { pickFile() },
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Batch action bar (manage mode)
                if (manageMode && selectedIds.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(colors.creamSurface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            i18n("已選擇 {} 項", selectedIds.size),
                            color = colors.textDark,
                            fontSize = 14.sp,
                        )
                        TextButton(onClick = { showBatchDeleteConfirm = true }) {
                            Text(i18n("刪除"), color = colors.redAccent)
                        }
                    }
                }

                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    if (!manageMode) {
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                YamiboActionChip(
                                    text = i18n("導入小說"),
                                    onClick = { pickFile() },
                                )
                            }
                        }
                    }
                    items(novels, key = { it.id }) { novel ->
                        LocalNovelCard(
                            novel = novel,
                            progress = progressMap[novel.id],
                            manageMode = manageMode,
                            isSelected = novel.id in selectedIds,
                            onClick = {
                                if (manageMode) {
                                    selectedIds = if (novel.id in selectedIds)
                                        selectedIds - novel.id else selectedIds + novel.id
                                } else {
                                    navigator.navigate(ILocalNovelReaderScreen(novel.id))
                                }
                            },
                            onDelete = { showDeleteConfirm = novel },
                        )
                    }
                }
            }
        }
    }

    // Single delete confirmation
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(i18n("刪除本地小說")) },
            text = { Text(i18n("確定要刪除「{}」嗎？此操作無法復原。", showDeleteConfirm!!.title)) },
            confirmButton = {
                TextButton(onClick = {
                    val novel = showDeleteConfirm!!
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            repository.deleteNovel(novel.id)
                            novel.epubExtractDir?.let { fileOps.deleteDirectory(it) }
                        }
                        snackbarHostState.showSnackbar(i18n("已刪除"))
                        reload()
                    }
                    showDeleteConfirm = null
                }) {
                    Text(i18n("刪除"), color = colors.redAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(i18n("取消"))
                }
            },
        )
    }

    // Batch delete confirmation
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text(i18n("批量刪除")) },
            text = { Text(i18n("確定要刪除所選的 {} 本小說嗎？此操作無法復原。", selectedIds.size)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.Default) {
                            for (id in selectedIds) {
                                val novel = repository.getNovelById(id)
                                repository.deleteNovel(id)
                                novel?.epubExtractDir?.let { fileOps.deleteDirectory(it) }
                            }
                        }
                        snackbarHostState.showSnackbar(i18n("已刪除 {} 項", selectedIds.size))
                        selectedIds = emptySet()
                        manageMode = false
                        reload()
                    }
                    showBatchDeleteConfirm = false
                }) {
                    Text(i18n("刪除"), color = colors.redAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text(i18n("取消"))
                }
            },
        )
    }
}

@Composable
private fun LocalNovelCard(
    novel: LocalNovelInfo,
    progress: String?,
    manageMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.brownPrimary.copy(alpha = 0.1f) else colors.creamSurface
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox in manage mode
            if (manageMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = colors.brownPrimary),
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(
                    novel.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (novel.author.isNotBlank()) {
                    Text(
                        novel.author,
                        fontSize = 13.sp,
                        color = colors.textOnBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when (novel.fileType) {
                            LocalNovelFileType.TXT -> "TXT"
                            LocalNovelFileType.EPUB -> "EPUB"
                        },
                        fontSize = 11.sp,
                        color = colors.textOnBackground,
                    )
                    Text(
                        "${novel.totalChapters}章",
                        fontSize = 11.sp,
                        color = colors.textOnBackground,
                    )
                    if (!progress.isNullOrEmpty()) {
                        Text(
                            progress,
                            fontSize = 11.sp,
                            color = colors.brownPrimary,
                        )
                    }
                }
            }

            if (!manageMode) {
                IconButton(onClick = onDelete) {
                    Icon(
                        YamiboIcons.Trashcan,
                        contentDescription = i18n("刪除"),
                        tint = colors.textOnBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private suspend fun buildProgressMap(
    novels: List<LocalNovelInfo>,
    repository: me.thenano.yamibo.yamibo_app.repository.LocalNovelRepository,
): Map<Long, String> {
    val map = mutableMapOf<Long, String>()
    for (novel in novels) {
        val progress = repository.getProgress(novel.id)
        if (progress != null && progress.chapterId > 0) {
            val chapter = repository.getChapterById(progress.chapterId)
            if (chapter != null) {
                map[novel.id] = "${chapter.chapterIndex + 1}/${novel.totalChapters}"
            }
        }
    }
    return map
}

private suspend fun importNovel(
    handle: LocalNovelFileHandle,
    repository: me.thenano.yamibo.yamibo_app.repository.LocalNovelRepository,
    fileOps: me.thenano.yamibo.yamibo_app.repository.localnovel.PlatformFileOperations,
    snackbarHostState: SnackbarHostState,
    navigator: me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator,
) {
    val name = handle.name
    val isEpub = name.endsWith(".epub", ignoreCase = true)
    val isTxt = name.endsWith(".txt", ignoreCase = true)

    if (!isEpub && !isTxt) {
        snackbarHostState.showSnackbar(i18n("不支援的檔案格式，請選擇 TXT 或 EPUB"))
        return
    }

    try {
        withContext(Dispatchers.Default) {
            if (isEpub) {
                importEpub(handle, name, repository, fileOps)
            } else {
                importTxt(handle, name, repository, fileOps)
            }
        }
        snackbarHostState.showSnackbar(i18n("導入成功"))
    } catch (e: Exception) {
        snackbarHostState.showSnackbar(i18n("導入失敗：{}", e.message ?: "未知錯誤"))
    }
}

private suspend fun importTxt(
    handle: LocalNovelFileHandle,
    fileName: String,
    repository: me.thenano.yamibo.yamibo_app.repository.LocalNovelRepository,
    fileOps: me.thenano.yamibo.yamibo_app.repository.localnovel.PlatformFileOperations,
) {
    val parser = me.thenano.yamibo.yamibo_app.repository.localnovel.TxtFileParser(fileOps)
    val result = parser.parse(handle.uri)

    val title = fileName.removeSuffix(".txt").removeSuffix(".TXT")
    val now = currentTimeMillis()

    val novel = LocalNovelInfo(
        fileType = LocalNovelFileType.TXT,
        title = title, author = "", fileUri = handle.uri,
        totalChars = result.totalChars, encoding = result.encoding,
        totalChapters = result.chapters.size, createdAt = now, lastReadAt = now,
    )
    val novelId = repository.insertNovel(novel)
    repository.insertChapters(result.chapters.map { it.copy(novelId = novelId) })
}

private suspend fun importEpub(
    handle: LocalNovelFileHandle,
    fileName: String,
    repository: me.thenano.yamibo.yamibo_app.repository.LocalNovelRepository,
    fileOps: me.thenano.yamibo.yamibo_app.repository.localnovel.PlatformFileOperations,
) {
    val extractDir = "${fileOps.getInternalFilesDir()}/epub_${currentTimeMillis()}"
    val parser = me.thenano.yamibo.yamibo_app.repository.localnovel.EpubFileParser(fileOps)
    val result = parser.parse(handle.uri, extractDir)

    val title = result.metadata.title.ifBlank {
        fileName.removeSuffix(".epub").removeSuffix(".EPUB")
    }
    val now = currentTimeMillis()

    val novel = LocalNovelInfo(
        fileType = LocalNovelFileType.EPUB,
        title = title, author = result.metadata.author, fileUri = handle.uri,
        totalChapters = result.chapters.size, epubExtractDir = extractDir,
        createdAt = now, lastReadAt = now,
    )
    val novelId = repository.insertNovel(novel)
    repository.insertChapters(result.chapters.map { it.copy(novelId = novelId) })

    result.metadata.coverInternalPath?.let { coverPath ->
        if (fileOps.localFileExists(coverPath)) {
            val coverDest = "${fileOps.getInternalFilesDir()}/covers/${novelId}"
            fileOps.copyFileToInternal(coverPath, coverDest)
            repository.updateCoverPath(novelId, coverDest)
        }
    }
}
