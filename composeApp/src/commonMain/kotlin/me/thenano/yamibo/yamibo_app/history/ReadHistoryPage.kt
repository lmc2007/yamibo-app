package me.thenano.yamibo.yamibo_app.history

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.value.ThreadId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.IMainScreen
import me.thenano.yamibo.yamibo_app.LocalReadHistoryRepository
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository.ThreadReadingHistory
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import kotlin.math.ceil

private sealed interface HistoryState {
    data object Loading : HistoryState
    data class Success(
        val items: List<ThreadReadingHistory>,
        val totalCount: Long,
        val currentPage: Int
    ) : HistoryState

    data object Empty : HistoryState
    data class Error(val message: String) : HistoryState
}

/** Top bar mode */
private enum class PageMode {
    Normal, Search, Select
}

private const val PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadHistoryPage() {
    val colors = YamiboTheme.colors
    val readHistoryRepo = LocalReadHistoryRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<HistoryState>(HistoryState.Loading) }
    var currentPage by remember { mutableIntStateOf(1) }
    var mode by remember { mutableStateOf(PageMode.Normal) }

    /** Search state */
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    /** Select state */
    var selectedIds by remember { mutableStateOf(setOf<ThreadId>()) }

    suspend fun loadPage(page: Int) {
        state = HistoryState.Loading
        try {
            val count = readHistoryRepo.getHistoryCount()
            if (count == 0L) {
                state = HistoryState.Empty
                return
            }
            val items = readHistoryRepo.getHistoryPage(page, PAGE_SIZE)
            currentPage = page
            state = HistoryState.Success(
                items = items,
                totalCount = count,
                currentPage = page
            )
        } catch (e: Exception) {
            state = HistoryState.Error(e.message ?: "未知錯誤")
        }
    }

    suspend fun doSearch(query: String, page: Int = 1) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        state = HistoryState.Loading
        try {
            val count = readHistoryRepo.searchHistoryCount(trimmed)
            if (count == 0L) {
                state = HistoryState.Empty
                return
            }
            val items = readHistoryRepo.searchHistory(trimmed, page, PAGE_SIZE)
            currentPage = page
            state = HistoryState.Success(
                items = items,
                totalCount = count,
                currentPage = page
            )
        } catch (e: Exception) {
            state = HistoryState.Error(e.message ?: "搜索失敗")
        }
    }

    val stackSize = navigator.stack.size
    LaunchedEffect(stackSize) {
        if (navigator.currentScreen is IMainScreen) {
            if (mode == PageMode.Normal) {
                loadPage(currentPage)
            } else if (mode == PageMode.Search) {
                doSearch(searchQuery, currentPage)
            }
        }
    }

    /** Focus search field when entering search mode */
    LaunchedEffect(mode) {
        if (mode == PageMode.Search) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground)
    ) {
        /** Top bar — varies by mode */
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.creamBackground,
            shadowElevation = 2.dp
        ) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "top_bar_mode"
            ) { currentMode ->
                when (currentMode) {
                    PageMode.Normal -> NormalTopBar(
                        onSearch = { mode = PageMode.Search },
                        onMultiSelect = {
                            selectedIds = emptySet()
                            mode = PageMode.Select
                        }
                    )

                    PageMode.Search -> SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = {
                            scope.launch { doSearch(searchQuery) }
                        },
                        onBack = {
                            searchQuery = ""
                            mode = PageMode.Normal
                            scope.launch { loadPage(1) }
                        },
                        focusRequester = focusRequester
                    )

                    PageMode.Select -> SelectTopBar(
                        onSelectAll = {
                            val current = state
                            if (current is HistoryState.Success) {
                                selectedIds = current.items.map { it.threadId }.toSet()
                            }
                        },
                        onClearAll = {
                            scope.launch {
                                readHistoryRepo.deleteAll()
                                selectedIds = emptySet()
                                mode = PageMode.Normal
                                state = HistoryState.Empty
                                snackbarHostState.showSnackbar("已清除所有紀錄")
                            }
                        },
                        onCancel = {
                            selectedIds = emptySet()
                            mode = PageMode.Normal
                        },
                        onDeleteSelected = {
                            if (selectedIds.isNotEmpty()) {
                                scope.launch {
                                    val deletedAmount = selectedIds.size
                                    readHistoryRepo.deleteHistoryBatch(selectedIds.toList())
                                    selectedIds = emptySet()
                                    mode = PageMode.Normal
                                    loadPage(1)
                                    snackbarHostState.showSnackbar("已刪除 $deletedAmount 條紀錄")
                                }
                            }
                        },
                        selectedCount = selectedIds.size
                    )
                }
            }
        }

        /** Content area */
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val current = state) {
                is HistoryState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = colors.brownPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                is HistoryState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = YamiboIcons.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colors.brownPrimary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (mode == PageMode.Search) "找不到相關紀錄" else "還沒有閱讀紀錄",
                            fontSize = 16.sp,
                            color = colors.textDark.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (mode == PageMode.Search) "試試別的關鍵字" else "開始閱讀後，紀錄會自動保存在這裡",
                            fontSize = 13.sp,
                            color = colors.textDark.copy(alpha = 0.35f)
                        )
                    }
                }

                is HistoryState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "載入失敗: ${current.message}",
                            fontSize = 14.sp,
                            color = colors.textDark.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { scope.launch { loadPage(currentPage) } },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.brownPrimary)
                        ) {
                            Text("重試", color = Color.White)
                        }
                    }
                }

                is HistoryState.Success -> {
                    val totalPages = ceil(current.totalCount.toDouble() / PAGE_SIZE).toInt()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        /** Group items by date */
                        val grouped = groupByDate(current.items)

                        grouped.forEach { (dateLabel, entries) ->
                            item(key = "header_$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textDark.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 12.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }

                            items(
                                items = entries,
                                key = { it.threadId.value }
                            ) { history ->
                                ReadHistoryCard(
                                    history = history,
                                    timeLabel = formatTime(history.lastVisitTime),
                                    isSelectMode = mode == PageMode.Select,
                                    isSelected = history.threadId in selectedIds,
                                    onClick = {
                                        when (mode) {
                                            PageMode.Select -> {
                                                selectedIds = if (history.threadId in selectedIds) {
                                                    selectedIds - history.threadId
                                                } else {
                                                    selectedIds + history.threadId
                                                }
                                            }

                                            else -> {
                                                navigator.navigate(
                                                    IThreadReaderScreen(
                                                        tid = history.threadId,
                                                        title = history.threadName,
                                                        authorId = history.authorId,
                                                        initialPage = history.page,
                                                        isAuthorOnly = history.authorId != null
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onCoverClick = {
                                        if (mode == PageMode.Select) {
                                            selectedIds = if (history.threadId in selectedIds) {
                                                selectedIds - history.threadId
                                            } else {
                                                selectedIds + history.threadId
                                            }
                                        } else {
                                            // Navigation logic for novel forum covers
                                            if (isNovelForum(history.forumId)) {
                                                navigator.navigate(
                                                    INovelThreadDetailScreen(
                                                        tid = history.threadId,
                                                        title = history.threadName,
                                                        authorId = history.authorId
                                                    )
                                                )
                                            } else {
                                                navigator.navigate(
                                                    IThreadReaderScreen(
                                                        tid = history.threadId,
                                                        title = history.threadName,
                                                        authorId = history.authorId,
                                                        initialPage = history.page,
                                                        isAuthorOnly = history.authorId != null
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    onDelete = {

                                        scope.launch {
                                            readHistoryRepo.deleteHistory(history.threadId)
                                            loadPage(currentPage)
                                            snackbarHostState.showSnackbar("已刪除紀錄")
                                        }
                                    },
                                    onFavorite = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("收藏功能開發中")
                                        }
                                    }
                                )
                            }
                        }

                        /** Pagination */
                        if (totalPages > 1) {
                            item(key = "pagination") {
                                PageNavigation(
                                    pageNav = PageNav(
                                        currentPage = current.currentPage,
                                        totalPages = totalPages,
                                        prevUrl = if (current.currentPage > 1) "prev" else null,
                                        nextUrl = if (current.currentPage < totalPages) "next" else null
                                    ),
                                    onPageChange = { page ->
                                        scope.launch {
                                            if (mode == PageMode.Search && searchQuery.isNotBlank()) {
                                                doSearch(searchQuery, page)
                                            } else {
                                                loadPage(page)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            /** Snackbar host */
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colors.brownDeep,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        }
    }
}

/** Normal mode top bar: title + search icon + trashcan icon */
@Composable
private fun NormalTopBar(
    onSearch: () -> Unit,
    onMultiSelect: () -> Unit
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "閱讀紀錄",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.brownDeep,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onSearch,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = YamiboIcons.Search,
                contentDescription = "搜尋",
                modifier = Modifier.size(30.dp).offset(y = 5.dp),
                tint = colors.brownDeep
            )
        }

        Spacer(Modifier.width(4.dp))

        IconButton(
            onClick = onMultiSelect,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = YamiboIcons.Trashcan,
                contentDescription = "多選刪除",
                modifier = Modifier.size(30.dp),
                tint = colors.brownDeep
            )
        }
    }
}

/** Search mode top bar: back arrow + search field */
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Text("◀", color = colors.brownDeep, fontSize = 20.sp)
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = "搜尋...",
                    color = colors.textDark.copy(alpha = 0.4f),
                    fontSize = 15.sp
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textDark,
                unfocusedTextColor = colors.textDark,
                cursorColor = colors.brownDeep,
                focusedBorderColor = colors.brownDeep,
                unfocusedBorderColor = colors.brownPrimary.copy(alpha = 0.3f),
                focusedContainerColor = colors.brownPrimary.copy(alpha = 0.05f),
                unfocusedContainerColor = colors.brownPrimary.copy(alpha = 0.03f)
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )

        Spacer(Modifier.width(6.dp))

        Surface(
            onClick = onSearch,
            shape = RoundedCornerShape(12.dp),
            color = colors.brownDeep
        ) {
            Text(
                text = "搜尋",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(8.dp))
    }
}

/** Select mode top bar: selected count + 全選 + 刪除選取 + 清空紀錄 + 取消 */
@Composable
private fun SelectTopBar(
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    onCancel: () -> Unit,
    onDeleteSelected: () -> Unit,
    selectedCount: Int
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "已選 $selectedCount",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.brownDeep,
            modifier = Modifier.weight(1f)
        )

        /** 全選 */
        Surface(
            onClick = onSelectAll,
            shape = RoundedCornerShape(10.dp),
            color = colors.brownPrimary.copy(alpha = 0.12f)
        ) {
            Text(
                text = "全選",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.brownDeep
            )
        }

        /** 刪除選取 */
        if (selectedCount > 0) {
            Surface(
                onClick = onDeleteSelected,
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFE53935).copy(alpha = 0.15f)
            ) {
                Text(
                    text = "刪除",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE53935)
                )
            }
        }

        /** 清空紀錄 */
        Surface(
            onClick = onClearAll,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFE53935).copy(alpha = 0.1f)
        ) {
            Text(
                text = "清空紀錄",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE53935)
            )
        }

        /** 取消 */
        Surface(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            color = colors.brownPrimary.copy(alpha = 0.12f)
        ) {
            Text(
                text = "取消",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.brownDeep
            )
        }
    }
}

/** Group history items by date label */
private fun groupByDate(items: List<ThreadReadingHistory>): List<Pair<String, List<ThreadReadingHistory>>> {
    val now = currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    val grouped = mutableMapOf<String, MutableList<ThreadReadingHistory>>()

    for (item in items) {
        val diffMs = now - item.lastVisitTime
        val label = when {
            diffMs < oneDayMs -> "今天"
            diffMs < 2 * oneDayMs -> "昨天"
            diffMs < 3 * oneDayMs -> "前天"
            diffMs < 7 * oneDayMs -> "${(diffMs / oneDayMs).toInt()} 天前"
            else -> formatDate(item.lastVisitTime)
        }
        grouped.getOrPut(label) { mutableListOf() }.add(item)
    }

    return grouped.toList()
}

/** Format timestamp to date string */
private fun formatDate(timestamp: Long): String {
    val totalDays = timestamp / (24 * 60 * 60 * 1000L)
    var year = 1970
    var remainingDays = totalDays + (8 * 60 * 60 * 1000L / (24 * 60 * 60 * 1000L)) // UTC+8

    while (true) {
        val daysInYear = if (isLeapYear(year)) 366L else 365L
        if (remainingDays < daysInYear) break
        remainingDays -= daysInYear
        year++
    }

    val monthDays = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 1
    for (days in monthDays) {
        if (remainingDays < days) break
        remainingDays -= days
        month++
    }
    val day = remainingDays.toInt() + 1

    return "$year/$month/$day"
}

/** Format timestamp to time string (HH:mm) */
private fun formatTime(timestamp: Long): String {
    val adjustedMs = timestamp + 8 * 60 * 60 * 1000L // UTC+8
    val totalMinutes = (adjustedMs / (60 * 1000L)) % (24 * 60)
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()

    val period = if (hours < 12) "上午" else "下午"
    val displayHour = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours

    return "$period${displayHour}:${minutes.toString().padStart(2, '0')}"
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun isNovelForum(fid: io.github.littlesurvival.dto.value.ForumId?): Boolean {
    if (fid == null) return false
    return fid.value == YamiboForum.LITERATURE.id.value ||
        fid.value == YamiboForum.TRANSLATED_LIGHT_NOVEL.id.value
}
