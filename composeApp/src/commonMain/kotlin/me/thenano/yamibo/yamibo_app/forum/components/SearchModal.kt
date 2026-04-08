package me.thenano.yamibo.yamibo_app.forum.components

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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.SearchId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalForumRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/** Search result state */
private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val page: SearchPage) : SearchState
    data class Error(val message: String) : SearchState
}

/** Full-screen search modal */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchModal(fid: ForumId?, onDismiss: () -> Unit, onThreadClick: (ThreadSummary) -> Unit) {
    val colors = YamiboTheme.colors
    val forumRepository = LocalForumRepository.current
    val authRepository = LocalAuthRepository.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var currentPage by remember { mutableIntStateOf(1) }
    var currentSearchId by remember { mutableStateOf<SearchId?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun doSearch(page: Int = 1) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val formHash = authRepository.currentUser()?.formHash ?: return
        state = SearchState.Loading
        currentPage = page
        scope.launch {
            val result =
                if (page == 1 || currentSearchId == null) {
                    forumRepository.fetchSearch(trimmed, fid, formHash)
                } else {
                    forumRepository.fetchSearchById(trimmed, currentSearchId!!, page)
                }
            state =
                when (result) {
                    is YamiboResult.Success -> {
                        if (result.value.threads.isEmpty()) {
                            SearchState.Error("沒有找到相關主題")
                        } else {
                            if (page == 1) {
                                currentSearchId = result.value.searchId
                            }
                            SearchState.Success(result.value)
                        }
                    }

                    else -> SearchState.Error(result.message())
                }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(colors.creamBackground)
                    .systemBarsPadding()
        ) {
            /** Search top bar */
            Surface(color = colors.brownDeep, shadowElevation = 4.dp) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Text("◀", color = Color.White, fontSize = 20.sp)
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = "搜尋主題...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 15.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch(1) }),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = colors.orangeAccent,
                                focusedBorderColor = colors.orangeAccent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                unfocusedContainerColor =
                                    Color.White.copy(alpha = 0.05f)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Surface(
                        onClick = { doSearch(1) },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.orangeAccent
                    ) {
                        Text(
                            text = "搜尋",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            /** Search body */
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "search_content"
            ) { currentState ->
                when (currentState) {
                    is SearchState.Idle -> SearchIdleContent()
                    is SearchState.Loading -> SearchLoadingContent()
                    is SearchState.Error -> SearchErrorContent(currentState.message)
                    is SearchState.Success ->
                        SearchResultContent(
                            searchPage = currentState.page,
                            onThreadClick = onThreadClick,
                            onPageChange = { doSearch(it) }
                        )
                }
            }
        }
    }
}

/** Idle: prompt text */
@Composable
private fun SearchIdleContent() {
    val colors = YamiboTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = YamiboIcons.Search,
                contentDescription = null,
                tint = colors.brownPrimary.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "輸入關鍵字搜尋主題",
                color = colors.brownPrimary.copy(alpha = 0.6f),
                fontSize = 15.sp
            )
        }
    }
}

/** Loading: spinner */
@Composable
private fun SearchLoadingContent() {
    val colors = YamiboTheme.colors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colors.brownDeep,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(text = "搜尋中...", color = colors.brownPrimary.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

/** Error */
@Composable
private fun SearchErrorContent(message: String) {
    val colors = YamiboTheme.colors
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.brownDeep.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Text(text = message, color = colors.brownDeep, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

/** Results: thread list */
@Composable
private fun SearchResultContent(
    searchPage: SearchPage,
    onThreadClick: (ThreadSummary) -> Unit,
    onPageChange: (Int) -> Unit
) {
    val colors = YamiboTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        /** result count header */
        item {
            Text(
                text = "找到 ${searchPage.totalCount} 個相關內容",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = colors.brownPrimary.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        /** thread cards */
        items(searchPage.threads, key = { it.tid.value }) { thread ->
            ThreadCard(thread = thread, onClick = { onThreadClick(thread) })
        }

        /** pagination */
        searchPage.pageNav?.let { nav ->
            item { PageNavigation(pageNav = nav, onPageChange = onPageChange) }
        }
    }
}
