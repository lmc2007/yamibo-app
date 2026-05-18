package me.thenano.yamibo.yamibo_app.forum.search

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.SearchId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import io.github.littlesurvival.parse.util.ParseUtils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalForumRepository
import me.thenano.yamibo.yamibo_app.LocalThreadRepository
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation
import me.thenano.yamibo.yamibo_app.forum.components.ThreadCard
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.RestorableNavigatable
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenEntry
import me.thenano.yamibo.yamibo_app.navigation.RestorableScreenSnapshot
import me.thenano.yamibo.yamibo_app.navigation.TypedRestorableNavigatableDecoder
import me.thenano.yamibo.yamibo_app.navigation.decodeRestorePayload
import me.thenano.yamibo.yamibo_app.navigation.restoreSnapshot
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.userspace.IUserSpaceScreen

@Serializable
private data class SearchScreenRestorePayload(val fid: Int?)

@RestorableScreenEntry
class ISearchScreen(private val fid: ForumId? = null) : RestorableNavigatable {
    override val id: String = buildId(fid?.value ?: "all")
    override val restoreDecoder = Decoder

    override fun toRestoreSnapshot(): RestorableScreenSnapshot = restoreSnapshot(
        decoder = restoreDecoder,
        payload = SearchScreenRestorePayload(fid = fid?.value),
    )

    @Composable
    override fun Content() {
        SearchScreen(fid = fid)
    }

    companion object Decoder : TypedRestorableNavigatableDecoder<ISearchScreen>(ISearchScreen::class) {
        override fun decode(payload: String): RestorableNavigatable {
            val data = decodeRestorePayload<SearchScreenRestorePayload>(payload)
            return ISearchScreen(fid = data.fid?.let(::ForumId))
        }
    }
}

private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val page: SearchPage) : SearchState
    data class Error(val message: String) : SearchState
}

private data class SearchDirectThreadTarget(
    val tid: ThreadId,
    val title: String,
    val isNovel: Boolean,
    val authorId: UserId?,
)

private data class SearchPageSnapshot(
    val state: SearchState,
    val currentPage: Int,
    val currentSearchId: SearchId?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(fid: ForumId?) {
    val colors = YamiboTheme.colors
    val forumRepository = LocalForumRepository.current
    val authRepository = LocalAuthRepository.current
    val threadRepository = LocalThreadRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var currentPage by remember { mutableIntStateOf(1) }
    var currentSearchId by remember { mutableStateOf<SearchId?>(null) }
    val pageHistory = remember { mutableStateListOf<SearchPageSnapshot>() }

    fun navigateThread(thread: ThreadSummary) {
        val isNovelThread = fid?.let { YamiboForum.isNovelForum(it) }
            ?: YamiboForum.isNovelForum(thread.tag ?: "")
        if (isNovelThread) {
            navigator.navigate(INovelThreadDetailScreen(thread.tid, thread.title, thread.author?.uid))
        } else {
            navigator.navigate(IThreadReaderScreen(tid = thread.tid, title = thread.title))
        }
    }

    fun navigateDirectThread(target: SearchDirectThreadTarget) {
        if (target.isNovel) {
            navigator.navigate(INovelThreadDetailScreen(target.tid, target.title, target.authorId))
        } else {
            navigator.navigate(IThreadReaderScreen(tid = target.tid, title = target.title))
        }
    }

    fun restorePreviousSearchPage(): Boolean {
        val snapshot = pageHistory.removeLastOrNull() ?: return false
        state = snapshot.state
        currentPage = snapshot.currentPage
        currentSearchId = snapshot.currentSearchId
        return true
    }

    fun doSearch(page: Int = 1, recordHistory: Boolean = false) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        if (recordHistory) {
            pageHistory.add(SearchPageSnapshot(state, currentPage, currentSearchId))
        } else if (page == 1) {
            pageHistory.clear()
        }

        state = SearchState.Loading
        currentPage = page
        scope.launch {
            val directThreadId = ParseUtils.extractTid(trimmed)
            val looksLikeUrl = trimmed.startsWith("http://") || trimmed.startsWith("https://")
            if (page == 1 && looksLikeUrl) {
                if (directThreadId == null) {
                    state = SearchState.Error(appString(Res.string.ui_this_link_not_recognized))
                    return@launch
                }
                state = when (val result = threadRepository.fetchThread(directThreadId)) {
                    is YamiboResult.Success -> {
                        val threadPage = result.value
                        navigateDirectThread(
                            SearchDirectThreadTarget(
                                tid = directThreadId,
                                title = threadPage.thread.title,
                                isNovel = YamiboForum.isNovelForum(threadPage.thread.forum.fid),
                                authorId = threadPage.posts.firstOrNull()?.author?.uid,
                            )
                        )
                        SearchState.Idle
                    }

                    else -> SearchState.Error(result.localizedMessage())
                }
                return@launch
            }

            val formHash = authRepository.currentUser()?.formHash
            if (formHash == null) {
                state = SearchState.Error(appString(Res.string.ui_please_log_in_first_searching))
                return@launch
            }

            val result = if (page == 1 || currentSearchId == null) {
                forumRepository.fetchSearch(trimmed, fid, formHash)
            } else {
                forumRepository.fetchSearchById(trimmed, currentSearchId!!, page)
            }
            state = when (result) {
                is YamiboResult.Success -> {
                    if (result.value.threads.isEmpty()) {
                        SearchState.Error(appString(Res.string.ui_no_search_results_found))
                    } else {
                        if (page == 1) currentSearchId = result.value.searchId
                        SearchState.Success(result.value)
                    }
                }

                else -> SearchState.Error(result.localizedMessage())
            }
        }
    }

    DisposableEffect(navigator) {
        val handler = { restorePreviousSearchPage() }
        navigator.backHandlers.add(handler)
        onDispose { navigator.backHandlers.remove(handler) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground)
            .systemBarsPadding(),
    ) {
        Surface(color = colors.brownDeep, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { if (!restorePreviousSearchPage()) navigator.pop() }) {
                    Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = appString(Res.string.ui_search_title_link),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 15.sp,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch(1) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = colors.orangeAccent,
                        focusedBorderColor = colors.orangeAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                )
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = { doSearch(1) },
                    shape = RoundedCornerShape(12.dp),
                    color = colors.orangeAccent,
                ) {
                    Text(
                        text = appString(Res.string.read_history_search),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "search_screen_content",
        ) { currentState ->
            when (currentState) {
                is SearchState.Idle -> SearchIdleContent()
                is SearchState.Loading -> SearchLoadingContent()
                is SearchState.Error -> SearchErrorContent(currentState.message)
                is SearchState.Success -> SearchResultContent(
                    searchPage = currentState.page,
                    onThreadClick = ::navigateThread,
                    onPageChange = { doSearch(it, recordHistory = true) },
                )
            }
        }
    }
}

@Composable
private fun SearchIdleContent() {
    val colors = YamiboTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = YamiboIcons.Search,
                contentDescription = null,
                tint = colors.brownPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(appString(Res.string.ui_enter_keyword_post_link), color = colors.brownPrimary.copy(alpha = 0.6f), fontSize = 15.sp)
        }
    }
}

@Composable
private fun SearchLoadingContent() {
    val colors = YamiboTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colors.brownDeep,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.size(12.dp))
            Text(appString(Res.string.ui_searching), color = colors.brownPrimary.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun SearchErrorContent(message: String) {
    val colors = YamiboTheme.colors
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = colors.brownDeep.copy(alpha = 0.6f))
                Spacer(Modifier.size(8.dp))
                Text(message, color = colors.brownDeep, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun SearchResultContent(
    searchPage: SearchPage,
    onThreadClick: (ThreadSummary) -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Text(
                text = appString(Res.string.search_results_count, searchPage.totalCount),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = colors.brownPrimary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(searchPage.threads, key = { it.tid.value }) { thread ->
            ThreadCard(
                thread = thread,
                onClick = { onThreadClick(thread) },
                onAuthorClick = { user -> navigator.navigate(IUserSpaceScreen(user.uid, user.name)) },
            )
        }
        searchPage.pageNav?.let { nav ->
            item { PageNavigation(pageNav = nav, onPageChange = onPageChange) }
        }
    }
}



