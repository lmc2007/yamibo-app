package me.thenano.yamibo.yamibo_app.message

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedMessage
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.page.PrivateMessage
import io.github.littlesurvival.dto.page.PrivateMessagePage
import io.github.littlesurvival.dto.page.PrivateMessageType
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalUserSpaceRepository
import me.thenano.yamibo.yamibo_app.components.UserAvatar
import me.thenano.yamibo.yamibo_app.components.YamiboEmptyContent
import me.thenano.yamibo.yamibo_app.components.YamiboErrorContent
import me.thenano.yamibo.yamibo_app.components.YamiboLoadingContent
import me.thenano.yamibo.yamibo_app.components.YamiboMessageInputBar
import me.thenano.yamibo.yamibo_app.components.YamiboPageNavigation
import me.thenano.yamibo.yamibo_app.components.YamiboTopBar
import me.thenano.yamibo.yamibo_app.components.YamiboTopBarIconAction
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.reader.components.post.impl.HtmlRenderer

private sealed interface PrivateMessageState {
    data object Loading : PrivateMessageState
    data class Success(val page: PrivateMessagePage) : PrivateMessageState
    data class Error(val message: String) : PrivateMessageState
}

@Composable
fun PrivateMessageScreen(
    toUser: UserId,
    titleHint: String? = null,
) {
    val colors = YamiboTheme.colors
    val repository = LocalUserSpaceRepository.current
    val authRepository = LocalAuthRepository.current
    val currentUser = authRepository.currentUser()
    val navigator = me.thenano.yamibo.yamibo_app.navigation.LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var currentPage by remember(toUser) { mutableIntStateOf(1) }
    var state by remember(toUser) {
        mutableStateOf<PrivateMessageState>(
            repository.getCachedPrivateMessagePage(toUser)?.let { PrivateMessageState.Success(it) }
                ?: PrivateMessageState.Loading
        )
    }
    var input by remember(toUser) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    suspend fun loadPage(page: Int? = null, preferCache: Boolean = true) {
        if (preferCache) {
            repository.getCachedPrivateMessagePage(toUser, page)?.let {
                currentPage = it.pageNav?.currentPage ?: page ?: currentPage
                state = PrivateMessageState.Success(it)
                return
            }
        }
        state = PrivateMessageState.Loading
        state = when (val result = repository.fetchPrivateMessagePage(toUser, page)) {
            is YamiboResult.Success -> {
                currentPage = result.value.pageNav?.currentPage ?: page ?: 1
                PrivateMessageState.Success(result.value)
            }
            else -> PrivateMessageState.Error(result.localizedMessage())
        }
    }

    LaunchedEffect(toUser) {
        if (state is PrivateMessageState.Loading) loadPage()
    }

    LaunchedEffect((state as? PrivateMessageState.Success)?.page?.messages?.size) {
        val size = (state as? PrivateMessageState.Success)?.page?.messages?.size ?: return@LaunchedEffect
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = { YamiboSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PrivateMessageTopBar(
                title = (state as? PrivateMessageState.Success)?.page?.title
                    ?: titleHint?.let { appString(Res.string.private_message_title_with_name, it) }
                    ?: appString(Res.string.ui_chat),
                onBack = { navigator.pop() },
                onRefresh = {
                    scope.launch { loadPage(page = null, preferCache = false) }
                },
            )
        },
        bottomBar = {
            val current = state as? PrivateMessageState.Success
            PrivateMessageInputBar(
                value = input,
                enabled = current != null && !sending,
                sending = sending,
                onValueChange = { input = it },
                onSend = {
                    val page = current?.page ?: return@PrivateMessageInputBar
                    val formHash = authRepository.currentUser()?.formHash
                    val message = input.trim()
                    when {
                        formHash == null -> scope.launch {
                            snackbarHostState.showSnackbar(appString(Res.string.ui_please_log_in_first_sending_message), duration = SnackbarDuration.Short)
                        }
                        message.isBlank() -> scope.launch {
                            snackbarHostState.showSnackbar(appString(Res.string.ui_please_enter_content_2), duration = SnackbarDuration.Short)
                        }
                        else -> scope.launch {
                            sending = true
                            when (val result = repository.sendPrivateMessage(page.pmId, page.toUser, message, formHash)) {
                                is YamiboResult.Success -> {
                                input = ""
                                repository.clearPrivateMessagePages(toUser)
                                repository.clearMessagePages()
                                loadPage(page = null, preferCache = false)
                            }
                                else -> snackbarHostState.showSnackbar(result.localizedMessage(), duration = SnackbarDuration.Short)
                            }
                            sending = false
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(colors.creamBackground),
        ) {
            when (val current = state) {
                PrivateMessageState.Loading -> PrivateMessageLoading()
                is PrivateMessageState.Error -> PrivateMessageError(current.message) {
                    scope.launch { loadPage(currentPage, preferCache = false) }
                }
                is PrivateMessageState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                    ) {
                        if (current.page.messages.isEmpty()) {
                            item { EmptyPrivateMessages() }
                        } else {
                            items(current.page.messages, key = { "${it.messageType}_${it.timeInfo.text}_${it.contentHtml.hashCode()}" }) { message ->
                                PrivateMessageBubble(message, currentUser)
                            }
                        }
                        current.page.pageNav?.let { nav ->
                            item {
                                PrivateMessagePageNavigation(nav, currentPage) { target ->
                                    scope.launch { loadPage(target) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivateMessageTopBar(title: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    YamiboTopBar(
        title = title,
        titleAlign = TextAlign.Center,
        titleFontSize = 18,
        onBack = onBack,
    ) {
        YamiboTopBarIconAction(YamiboIcons.Reload, appString(Res.string.ui_refresh_2), onRefresh, iconSize = 22)
    }
}

@Composable
private fun PrivateMessageBubble(message: PrivateMessage, currentUser: ProfilePage?) {
    val colors = YamiboTheme.colors
    val isSelf = message.messageType == PrivateMessageType.Self
    val displayName = if (isSelf) currentUser?.username ?: message.user.name else message.user.name
    val avatarUrl = if (isSelf) currentUser?.avatarUrl ?: message.user.avatarUrl else message.user.avatarUrl
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isSelf) {
            UserAvatar(avatarUrl, size = 38)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = displayName,
                color = colors.brownDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (isSelf) TextAlign.End else TextAlign.Start,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isSelf) 12.dp else 2.dp,
                    topEnd = if (isSelf) 2.dp else 12.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp,
                ),
                color = colors.creamSurface,
            ) {
                HtmlRenderer(
                    html = message.contentHtml,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = message.timeInfo.text,
                color = colors.brownLight,
                fontSize = 12.sp,
                textAlign = if (isSelf) TextAlign.End else TextAlign.Start,
            )
        }
        if (isSelf) {
            Spacer(Modifier.width(8.dp))
            UserAvatar(avatarUrl, size = 38)
        }
    }
}

@Composable
private fun PrivateMessageInputBar(
    value: String,
    enabled: Boolean,
    sending: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    YamiboMessageInputBar(
        value = value,
        placeholder = appString(Res.string.ui_please_enter_content),
        enabled = enabled,
        sending = sending,
        sendText = appString(Res.string.ui_send),
        sendingText = appString(Res.string.ui_sending),
        onValueChange = onValueChange,
        onSend = onSend,
    )
}

@Composable
private fun PrivateMessagePageNavigation(pageNav: PageNav, currentPage: Int, onPageChange: (Int) -> Unit) {
    YamiboPageNavigation(pageNav = pageNav, currentPage = currentPage, onPageChange = onPageChange)
}

@Composable
private fun PrivateMessageLoading() {
    YamiboLoadingContent()
}

@Composable
private fun PrivateMessageError(message: String, onRetry: () -> Unit) {
    YamiboErrorContent(message = message, onRetry = onRetry)
}

@Composable
private fun EmptyPrivateMessages() {
    YamiboEmptyContent(message = appString(Res.string.ui_no_message_found), modifier = Modifier.padding(vertical = 80.dp))
}



