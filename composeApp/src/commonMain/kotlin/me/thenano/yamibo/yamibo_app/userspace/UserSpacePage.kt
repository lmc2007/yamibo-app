package me.thenano.yamibo.yamibo_app.userspace

import YamiboIcons
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import io.github.littlesurvival.YamiboForum
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.BlogSummary
import io.github.littlesurvival.dto.model.PageNav
import io.github.littlesurvival.dto.model.ThreadSummary
import io.github.littlesurvival.dto.model.User
import io.github.littlesurvival.dto.page.NoticeItem
import io.github.littlesurvival.dto.page.PrivateMessageItem
import io.github.littlesurvival.dto.page.ProfilePage
import io.github.littlesurvival.dto.page.ReplyItem
import io.github.littlesurvival.dto.page.UserSpaceBlogPage
import io.github.littlesurvival.dto.page.UserSpaceFriendItem
import io.github.littlesurvival.dto.page.UserSpaceFriendPage
import io.github.littlesurvival.dto.page.UserSpaceNoticePage
import io.github.littlesurvival.dto.page.UserSpacePrivateMessagePage
import io.github.littlesurvival.dto.page.UserSpaceThreadPage
import io.github.littlesurvival.dto.page.UserSpaceThreadReplyPage
import io.github.littlesurvival.dto.value.UserId
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalUserSpaceRepository
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation
import me.thenano.yamibo.yamibo_app.forum.components.StatBadge
import me.thenano.yamibo.yamibo_app.forum.components.ThreadCard
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.thread.detail.novel.INovelThreadDetailScreen
import me.thenano.yamibo.yamibo_app.thread.reader.IThreadReaderScreen
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import me.thenano.yamibo.yamibo_app.webview.action.IActionWebView

enum class UserSpaceTab(val selfTitle: String, val otherTitle: String = selfTitle) {
    Profile("我的資料", "Ta的資料"),
    Threads("我的主題", "Ta的主題"),
    Replies("我的回覆", "Ta的回覆"),
    MyBlogs("我的日志", "Ta的日志"),
    FriendBlogs("好友的日志"),
    ViewAllBlogs("隨便看看"),
    Friends("我的好友"),
    Online("在線成員"),
    Visitors("我的訪客"),
    Traces("我的足跡"),
    Messages("我的消息"),
    Notices("我的提醒");

    fun title(isSelf: Boolean): String = if (isSelf) selfTitle else otherTitle
}

enum class UserSpaceGroup {
    Space,
    Threads,
    Blogs,
    Friends,
    Messages,
}

private sealed interface UserSpaceState {
    data object Loading : UserSpaceState
    data class Success(val content: UserSpaceContent) : UserSpaceState
    data class Error(val message: String) : UserSpaceState
}

private sealed interface UserSpaceContent {
    data class Profile(val page: ProfilePage) : UserSpaceContent
    data class Threads(val page: UserSpaceThreadPage) : UserSpaceContent
    data class Replies(val page: UserSpaceThreadReplyPage) : UserSpaceContent
    data class Blogs(val page: UserSpaceBlogPage) : UserSpaceContent
    data class Friends(val page: UserSpaceFriendPage) : UserSpaceContent
    data class Messages(val page: UserSpacePrivateMessagePage) : UserSpaceContent
    data class Notices(val page: UserSpaceNoticePage) : UserSpaceContent
}

private enum class ViewAllBlogFilter(
    val title: String,
    val apiType: YamiboRoute.UserSpace.Blog.ViewAllType,
) {
    Latest("最新發表的日志", YamiboRoute.UserSpace.Blog.ViewAllType.Latest),
    Hot("推薦閱讀的日志", YamiboRoute.UserSpace.Blog.ViewAllType.Hot),
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UserSpacePage(
    userId: UserId? = null,
    titleHint: String? = null,
    group: UserSpaceGroup = UserSpaceGroup.Space,
    initialTab: UserSpaceTab = UserSpaceTab.Profile,
    mainTabTopBar: Boolean = false,
) {
    val colors = YamiboTheme.colors
    val repository = LocalUserSpaceRepository.current
    val authRepository = LocalAuthRepository.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUser = authRepository.currentUser()
    val isSelf = userId == null || currentUser?.uid?.value == userId.value
    val tabs = remember(isSelf, group) { tabsFor(group, isSelf) }

    var selectedTab by remember(userId, group, initialTab) {
        mutableStateOf(if (initialTab in tabs) initialTab else tabs.first())
    }
    var currentPage by remember { mutableIntStateOf(1) }
    var state by remember { mutableStateOf<UserSpaceState>(UserSpaceState.Loading) }
    var profile by remember { mutableStateOf(repository.getCachedProfile(userId)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var viewAllBlogFilter by remember(userId, group) { mutableStateOf(ViewAllBlogFilter.Latest) }

    suspend fun loadTab(tab: UserSpaceTab, page: Int, preferCache: Boolean = true) {
        if (preferCache) {
            cachedContent(repository, userId, tab, page, viewAllBlogFilter.apiType)?.let {
                currentPage = page
                state = UserSpaceState.Success(it)
                return
            }
        }

        val result = fetchContent(repository, userId, tab, page, viewAllBlogFilter.apiType)
        state = when (result) {
            is YamiboResult.Success -> {
                currentPage = result.value.pageNumber() ?: page
                UserSpaceState.Success(result.value)
            }
            else -> UserSpaceState.Error(result.message())
        }
    }

    LaunchedEffect(userId) {
        profile = repository.getCachedProfile(userId)
        if (profile == null) {
            when (val result = repository.fetchProfile(userId)) {
                is YamiboResult.Success -> profile = result.value
                else -> Unit
            }
        }
    }

    LaunchedEffect(userId, group, selectedTab, viewAllBlogFilter) {
        currentPage = 1
        state = UserSpaceState.Loading
        loadTab(selectedTab, 1)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.creamBackground,
        snackbarHost = { YamiboSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (mainTabTopBar) {
                UserSpaceMainTopBar(
                    title = group.mainTitle(),
                    profile = currentUser,
                    onSpaceClick = {
                        val user = currentUser
                        navigator.navigate(IUserSpaceScreen(user?.uid, user?.username))
                    },
                )
            } else {
                UserSpaceTopBar(
                    title = topBarTitle(profile, titleHint, group, selectedTab, isSelf),
                    showEdit = isSelf && group == UserSpaceGroup.Messages && selectedTab == UserSpaceTab.Messages,
                    applyStatusPadding = true,
                    onBack = { navigator.pop() },
                    onEdit = {
                        scope.launch {
                            snackbarHostState.showSnackbar("TODO: 發起新消息", duration = SnackbarDuration.Short)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(colors.creamBackground)
        ) {
            if (tabs.size > 1) {
                UserSpaceTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    isSelf = isSelf,
                    onSelect = { selectedTab = it }
                )
            }
            if (selectedTab == UserSpaceTab.ViewAllBlogs) {
                UserSpaceViewAllBlogFilterRow(
                    selected = viewAllBlogFilter,
                    onSelect = { viewAllBlogFilter = it },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "userspace_state",
                ) { current ->
                    when (current) {
                        UserSpaceState.Loading -> UserSpaceLoadingContent()
                        is UserSpaceState.Error -> UserSpaceErrorContent(
                            message = current.message,
                            onRetry = {
                                state = UserSpaceState.Loading
                                scope.launch { loadTab(selectedTab, currentPage, preferCache = false) }
                            }
                        )
                        is UserSpaceState.Success -> PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                scope.launch {
                                    loadTab(selectedTab, currentPage, preferCache = false)
                                    if (selectedTab == UserSpaceTab.Profile) {
                                        when (val result = repository.fetchProfile(userId)) {
                                            is YamiboResult.Success -> profile = result.value
                                            else -> Unit
                                        }
                                    }
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            UserSpaceContent(
                                content = current.content,
                                profile = profile,
                                isSelf = isSelf,
                                selectedTab = selectedTab,
                                currentPage = currentPage,
                                onNavigateGroup = { targetGroup, targetTab ->
                                    navigator.navigate(
                                        IUserSpaceScreen(
                                            userId = userId,
                                            titleHint = profile?.username ?: titleHint,
                                            group = targetGroup,
                                            initialTab = targetTab,
                                        )
                                    )
                                },
                                onPageChange = { page ->
                                    state = UserSpaceState.Loading
                                    scope.launch { loadTab(selectedTab, page) }
                                },
                                onThreadClick = { thread -> navigateThread(thread, navigator) },
                                onUserClick = { user -> navigator.navigate(IUserSpaceScreen(user.uid, user.name)) },
                                onBlogClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("TODO: BlogPage Reader API 尚未接入", duration = SnackbarDuration.Short)
                                    }
                                },
                                onReplyQuoteClick = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("TODO: 回覆定位跳轉尚未接入", duration = SnackbarDuration.Short)
                                    }
                                },
                                onMessageAction = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("TODO: 消息互動尚未接入", duration = SnackbarDuration.Short)
                                    }
                                },
                                onOpenWebView = { title, url ->
                                    navigator.navigate(
                                        IActionWebView(
                                            title = title,
                                            initialUrl = fullYamiboUrl(url),
                                            successCondition = { false },
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tabsFor(group: UserSpaceGroup, isSelf: Boolean): List<UserSpaceTab> = when (group) {
    UserSpaceGroup.Space -> listOf(UserSpaceTab.Profile)
    UserSpaceGroup.Threads -> listOf(UserSpaceTab.Threads, UserSpaceTab.Replies)
    UserSpaceGroup.Blogs -> if (isSelf) {
        listOf(UserSpaceTab.FriendBlogs, UserSpaceTab.MyBlogs, UserSpaceTab.ViewAllBlogs)
    } else {
        listOf(UserSpaceTab.MyBlogs)
    }
    UserSpaceGroup.Friends -> listOf(
        UserSpaceTab.Friends,
        UserSpaceTab.Online,
        UserSpaceTab.Visitors,
        UserSpaceTab.Traces,
    )
    UserSpaceGroup.Messages -> listOf(UserSpaceTab.Messages, UserSpaceTab.Notices)
}

private fun UserSpaceGroup.mainTitle(): String = when (this) {
    UserSpaceGroup.Space -> "我的資料"
    UserSpaceGroup.Threads -> "我的主題"
    UserSpaceGroup.Blogs -> "我的日志"
    UserSpaceGroup.Friends -> "我的好友"
    UserSpaceGroup.Messages -> "我的消息"
}

@Composable
private fun UserSpaceViewAllBlogFilterRow(
    selected: ViewAllBlogFilter,
    onSelect: (ViewAllBlogFilter) -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.creamSurface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ViewAllBlogFilter.entries.forEach { filter ->
            Surface(
                onClick = { onSelect(filter) },
                shape = RoundedCornerShape(5.dp),
                color = if (filter == selected) colors.brownDeep else Color.Transparent,
            ) {
                Text(
                    text = filter.title,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = if (filter == selected) Color.White else colors.brownPrimary.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = if (filter == selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.45f))
}

@Composable
private fun UserSpaceTopBar(
    title: String,
    showEdit: Boolean,
    applyStatusPadding: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = YamiboTheme.colors
    val modifier = if (applyStatusPadding) {
        Modifier.fillMaxWidth().statusBarsPadding()
    } else {
        Modifier.fillMaxWidth()
    }
    Surface(modifier = modifier, color = colors.brownDeep, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showEdit) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = YamiboIcons.EditOrSign,
                        contentDescription = "編輯",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSpaceMainTopBar(
    title: String,
    profile: ProfilePage?,
    onSpaceClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Surface(modifier = Modifier.fillMaxWidth().statusBarsPadding(), color = colors.creamBackground, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.brownDeep,
                modifier = Modifier.weight(1f),
            )
            Surface(onClick = onSpaceClick, shape = RoundedCornerShape(18.dp), color = Color.Transparent) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Avatar(profile?.avatarUrl, size = 28)
                    Text(
                        text = "我的空間",
                        color = colors.brownDeep,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserSpaceTabRow(
    tabs: List<UserSpaceTab>,
    selectedTab: UserSpaceTab,
    isSelf: Boolean,
    onSelect: (UserSpaceTab) -> Unit,
) {
    val colors = YamiboTheme.colors
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    ScrollableYamiboTabRow(
        selectedIndex = selectedIndex,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        text = tab.title(isSelf),
                        color = colors.brownDeep,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }
            )
        }
    }
}

@Composable
private fun ScrollableYamiboTabRow(
    selectedIndex: Int,
    content: @Composable () -> Unit,
) {
    val colors = YamiboTheme.colors
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = colors.creamSurface,
        contentColor = colors.brownDeep,
        edgePadding = 0.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = colors.brownDeep,
                    height = 2.dp,
                )
            }
        },
        divider = {
            HorizontalDivider(color = colors.brownLight.copy(alpha = 0.45f))
        },
    ) {
        content()
    }
}

@Composable
private fun UserSpaceContent(
    content: UserSpaceContent,
    profile: ProfilePage?,
    isSelf: Boolean,
    selectedTab: UserSpaceTab,
    currentPage: Int,
    onNavigateGroup: (UserSpaceGroup, UserSpaceTab) -> Unit,
    onPageChange: (Int) -> Unit,
    onThreadClick: (ThreadSummary) -> Unit,
    onUserClick: (User) -> Unit,
    onBlogClick: (BlogSummary) -> Unit,
    onReplyQuoteClick: () -> Unit,
    onMessageAction: () -> Unit,
    onOpenWebView: (String, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        when (content) {
            is UserSpaceContent.Profile -> {
                item {
                    UserSpaceProfileHeader(
                        profile = content.page,
                        isSelf = isSelf,
                        onNavigateGroup = onNavigateGroup,
                    )
                }
            }
            is UserSpaceContent.Threads -> {
                if (content.page.threads.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.threads, key = { it.tid.value }) { thread ->
                    ThreadCard(
                        thread = thread,
                        onClick = { onThreadClick(thread) },
                        onAuthorClick = { user -> onUserClick(user) },
                    )
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Replies -> {
                if (content.page.replies.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.replies, key = { it.tId.value }) { reply ->
                    ReplyGroupCard(reply, onThreadClick = { onThreadClick(it) }, onQuoteClick = onReplyQuoteClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Blogs -> {
                if (content.page.blogs.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.blogs, key = { it.bId.value }) { blog ->
                    BlogCard(blog, onClick = { onBlogClick(blog) }, onUserClick = onUserClick)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Friends -> {
                if (content.page.users.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.users, key = { it.user.uid.value }) { item ->
                    FriendCard(
                        item = item,
                        onUserClick = { onUserClick(item.user) },
                        onMessageClick = item.pmUrl?.let { url -> { onOpenWebView("發消息", url) } },
                        onDeleteClick = onMessageAction,
                    )
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Messages -> {
                if (content.page.messages.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.messages, key = { "${it.user.uid.value}_${it.timeInfo.text}" }) { message ->
                    PrivateMessageCard(message, onUserClick = { onUserClick(message.user) }, onAction = onMessageAction)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
            is UserSpaceContent.Notices -> {
                if (content.page.notices.isEmpty()) {
                    item { UserSpaceEmptyListMessage(emptyMessage(selectedTab, isSelf)) }
                }
                items(content.page.notices, key = { it.noticeId.value }) { notice ->
                    NoticeCard(notice, onUserClick = { notice.actor?.let(onUserClick) }, onAction = onMessageAction)
                }
                content.page.pageNav?.let { nav -> item { UserSpacePageNavigation(nav, currentPage, onPageChange) } }
            }
        }
    }
}

@Composable
private fun UserSpaceEmptyListMessage(message: String) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = colors.brownPrimary.copy(alpha = 0.65f),
            fontSize = 14.sp,
        )
    }
}

private fun emptyMessage(tab: UserSpaceTab, isSelf: Boolean): String = when (tab) {
    UserSpaceTab.Profile -> ""
    UserSpaceTab.Threads -> if (isSelf) "沒有找到主題" else "沒有找到Ta的主題"
    UserSpaceTab.Replies -> if (isSelf) "沒有找到回覆" else "沒有找到Ta的回覆"
    UserSpaceTab.MyBlogs -> if (isSelf) "沒有找到日志" else "沒有找到Ta的日志"
    UserSpaceTab.FriendBlogs -> "沒有找到好友的日志"
    UserSpaceTab.ViewAllBlogs -> "沒有找到日志"
    UserSpaceTab.Friends -> "沒有找到好友"
    UserSpaceTab.Online -> "沒有找到在線成員"
    UserSpaceTab.Visitors -> "沒有找到訪客"
    UserSpaceTab.Traces -> "沒有找到足跡"
    UserSpaceTab.Messages -> "沒有找到消息"
    UserSpaceTab.Notices -> "沒有找到提醒"
}

@Composable
private fun UserSpacePageNavigation(pageNav: PageNav, currentPage: Int, onPageChange: (Int) -> Unit) {
    val current = pageNav.currentPage
    val total = pageNav.totalPages
    if (current != null && total != null) {
        PageNavigation(pageNav = pageNav, onPageChange = onPageChange)
        return
    }

    val colors = YamiboTheme.colors
    val currentFallback = current ?: currentPage
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = { if (currentFallback > 1) onPageChange(currentFallback - 1) },
            enabled = pageNav.prevUrl != null,
            shape = RoundedCornerShape(12.dp),
            color = if (pageNav.prevUrl != null) colors.creamSurface else colors.brownLight.copy(alpha = 0.3f),
            shadowElevation = if (pageNav.prevUrl != null) 2.dp else 0.dp,
        ) {
            Text(
                text = "上一頁",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = if (pageNav.prevUrl != null) colors.brownDeep else colors.brownLight,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.width(14.dp))
        Surface(
            onClick = { onPageChange(currentFallback + 1) },
            enabled = pageNav.nextUrl != null,
            shape = RoundedCornerShape(12.dp),
            color = if (pageNav.nextUrl != null) colors.creamSurface else colors.brownLight.copy(alpha = 0.3f),
            shadowElevation = if (pageNav.nextUrl != null) 2.dp else 0.dp,
        ) {
            Text(
                text = "下一頁",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = if (pageNav.nextUrl != null) colors.brownDeep else colors.brownLight,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun UserSpaceProfileHeader(
    profile: ProfilePage,
    isSelf: Boolean,
    onNavigateGroup: (UserSpaceGroup, UserSpaceTab) -> Unit,
) {
    val colors = YamiboTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .background(colors.brownPrimary.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            val backgroundUrl = profile.avatarBackgroundUrl
            if (!backgroundUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = rememberImageRequest(backgroundUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(profile.avatarUrl, size = 64)
                Spacer(Modifier.height(10.dp))
                Text(profile.username, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }

        StatPanel(profile)
        ActionGrid(isSelf, onNavigateGroup)
        if (isSelf) {
            ProfileInfoTable(profile)
        } else {
            TodoSignatureCard()
        }
    }
}

@Composable
private fun CompactProfileHeader(profile: ProfilePage, isSelf: Boolean) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile.avatarUrl, size = 42)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(profile.username, color = colors.brownDeep, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (isSelf) "我的空間" else "用戶空間",
                color = colors.brownPrimary.copy(alpha = 0.65f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun StatPanel(profile: ProfilePage) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-24).dp),
        shape = RoundedCornerShape(5.dp),
        color = colors.creamSurface,
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            ProfileStat("總積分", profile.totalPoints.toString(), Modifier.weight(1f))
            ProfileStat("積分", "${profile.points} 點", Modifier.weight(1f))
            ProfileStat("對象", profile.partner.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    val colors = YamiboTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = colors.textDark, fontSize = 18.sp)
        Text(label, color = colors.brownPrimary.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}

private data class SpaceAction(
    val group: UserSpaceGroup?,
    val initialTab: UserSpaceTab,
    val label: String,
)

@Composable
private fun ActionGrid(isSelf: Boolean, onNavigateGroup: (UserSpaceGroup, UserSpaceTab) -> Unit) {
    val actions = if (isSelf) {
        listOf(
            SpaceAction(UserSpaceGroup.Threads, UserSpaceTab.Threads, "我的主題"),
            SpaceAction(UserSpaceGroup.Blogs, UserSpaceTab.FriendBlogs, "我的日志"),
            SpaceAction(UserSpaceGroup.Friends, UserSpaceTab.Friends, "我的好友"),
            SpaceAction(UserSpaceGroup.Messages, UserSpaceTab.Messages, "消息提醒"),
        )
    } else {
        listOf(
            SpaceAction(UserSpaceGroup.Threads, UserSpaceTab.Threads, "Ta的主題"),
            SpaceAction(UserSpaceGroup.Blogs, UserSpaceTab.MyBlogs, "Ta的日志"),
            SpaceAction(UserSpaceGroup.Threads, UserSpaceTab.Replies, "Ta的回覆"),
            SpaceAction(null, UserSpaceTab.Profile, "加為好友"),
        )
    }
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).offset(y = (-12).dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { action ->
                        Surface(
                            onClick = {
                                if (action.group == null) {
                                    // TODO: add friend API / web flow.
                                } else {
                                    onNavigateGroup(action.group, action.initialTab)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                            color = colors.creamBackground,
                        ) {
                            Text(
                                text = action.label,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                color = colors.brownDeep,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoTable(profile: ProfilePage) {
    val rows = listOf(
        "UID" to profile.uid.value.toString(),
        "用戶組" to profile.userGroup,
        "性別" to (profile.gender ?: "-"),
        "生日" to (profile.birthday ?: "-"),
        "在線時間" to "${profile.onlineHours} 小時",
        "注冊時間" to (profile.registerTime?.text ?: "-"),
        "最後訪問" to (profile.lastVisit?.text ?: "-"),
    )
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("個人資料", color = colors.brownDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f), color = colors.textDark, fontSize = 14.sp)
                    Text(value, color = colors.brownPrimary.copy(alpha = 0.75f), fontSize = 14.sp)
                }
                HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun TodoSignatureCard() {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("個人簽名", color = colors.brownDeep, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("TODO", color = colors.brownPrimary.copy(alpha = 0.65f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReplyGroupCard(
    item: ReplyItem,
    onThreadClick: (ThreadSummary) -> Unit,
    onQuoteClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.creamSurface,
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.35f)),
        onClick = {
            onThreadClick(
                ThreadSummary(
                    tid = item.tId,
                    title = item.title,
                    hasPoll = false,
                    url = item.url,
                )
            )
        },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(item.title, color = colors.brownDeep, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            item.posts.forEach { post ->
                Surface(
                    onClick = onQuoteClick,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = colors.creamBackground,
                ) {
                    Text(
                        text = post.quote,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        color = colors.textDark,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlogCard(blog: BlogSummary, onClick: () -> Unit, onUserClick: (User) -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        color = colors.creamSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, colors.brownLight.copy(alpha = 0.35f)),
        onClick = onClick,
    ) {
        Column(Modifier.padding(14.dp)) {
            UserLine(blog.author, blog.timeInfo.text, onUserClick)
            Spacer(Modifier.height(10.dp))
            Text(blog.title, color = colors.brownDeep, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                blog.description,
                color = colors.brownPrimary.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FriendCard(
    item: UserSpaceFriendItem,
    onUserClick: () -> Unit,
    onMessageClick: (() -> Unit)?,
    onDeleteClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onUserClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(item.user.avatarUrl, size = 42)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.user.name, color = colors.brownDeep, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            item.description?.let {
                Text(it, color = colors.brownPrimary.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
        if (onMessageClick != null) {
            SmallActionButton("發消息", onMessageClick)
            Spacer(Modifier.width(6.dp))
        }
        if (item.deleteUrl != null) {
            SmallActionButton("刪除", onDeleteClick)
        }
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
}

@Composable
private fun PrivateMessageCard(item: PrivateMessageItem, onUserClick: () -> Unit, onAction: () -> Unit) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable(onClick = onUserClick)) {
            Avatar(item.user.avatarUrl, size = 42)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, color = colors.brownDeep, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.unreadCount?.let {
                    Spacer(Modifier.width(6.dp))
                    SmallBadge(it.toString())
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(item.message, color = colors.brownPrimary.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(item.timeInfo.text, color = colors.brownLight, fontSize = 12.sp)
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
}

@Composable
private fun NoticeCard(item: NoticeItem, onUserClick: () -> Unit, onAction: () -> Unit) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.clickable(onClick = onUserClick)) {
            Avatar(item.avatarUrl, size = 42)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.timeInfo.text, color = colors.brownLight, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            Text(item.message, color = colors.brownDeep, fontSize = 14.sp, lineHeight = 19.sp)
            item.quote?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = colors.creamBackground,
                ) {
                    Text(it, modifier = Modifier.padding(10.dp), color = colors.textDark, fontSize = 13.sp)
                }
            }
        }
        SmallActionButton("屏蔽", onAction)
    }
    HorizontalDivider(color = colors.brownLight.copy(alpha = 0.35f))
}

@Composable
private fun UserLine(user: User, time: String?, onUserClick: (User) -> Unit) {
    val colors = YamiboTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable { onUserClick(user) }) {
            Avatar(user.avatarUrl, size = 34)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.clickable { onUserClick(user) }) {
            Text(user.name, color = colors.brownDeep, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            time?.let { Text(it, color = colors.brownLight, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun Avatar(url: String?, size: Int) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(colors.brownPrimary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = YamiboIcons.PersonFill,
            contentDescription = null,
            modifier = Modifier.size((size * 0.7f).dp),
            tint = colors.textDark.copy(alpha = 0.45f),
        )
        if (!url.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun SmallActionButton(text: String, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(onClick = onClick, shape = RoundedCornerShape(5.dp), color = colors.orangeAccent) {
        Text(text, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun SmallBadge(text: String) {
    val colors = YamiboTheme.colors
    Surface(shape = RoundedCornerShape(10.dp), color = colors.orangeAccent) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun UserSpaceLoadingContent() {
    val colors = YamiboTheme.colors
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = colors.brownDeep)
    }
}

@Composable
private fun UserSpaceErrorContent(message: String, onRetry: () -> Unit) {
    val colors = YamiboTheme.colors
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("載入失敗", color = colors.brownDeep, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(10.dp))
                Text(message, color = colors.brownPrimary.copy(alpha = 0.75f), fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                Surface(onClick = onRetry, shape = RoundedCornerShape(50), color = colors.brownDeep) {
                    Text("重試", modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp), color = Color.White)
                }
            }
        }
    }
}

private suspend fun fetchContent(
    repository: me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository,
    userId: UserId?,
    tab: UserSpaceTab,
    page: Int,
    viewAllType: YamiboRoute.UserSpace.Blog.ViewAllType,
): YamiboResult<UserSpaceContent> {
    return when (tab) {
        UserSpaceTab.Profile -> repository.fetchProfile(userId).mapSuccess { UserSpaceContent.Profile(it) }
        UserSpaceTab.Threads -> repository.fetchThreads(userId, page).mapSuccess { UserSpaceContent.Threads(it) }
        UserSpaceTab.Replies -> repository.fetchReplies(userId, page).mapSuccess { UserSpaceContent.Replies(it) }
        UserSpaceTab.MyBlogs -> repository.fetchMyBlogs(userId, page).mapSuccess { UserSpaceContent.Blogs(it) }
        UserSpaceTab.FriendBlogs -> repository.fetchFriendBlogs(page).mapSuccess { UserSpaceContent.Blogs(it) }
        UserSpaceTab.ViewAllBlogs -> repository.fetchViewAllBlogs(viewAllType, page).mapSuccess { UserSpaceContent.Blogs(it) }
        UserSpaceTab.Friends -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.MyFriend, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Online -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.OnlineMember, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Visitors -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.MyVisitor, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Traces -> repository.fetchFriends(YamiboRoute.UserSpace.FriendPageType.MyTrace, page).mapSuccess { UserSpaceContent.Friends(it) }
        UserSpaceTab.Messages -> repository.fetchPrivateMessages(page).mapSuccess { UserSpaceContent.Messages(it) }
        UserSpaceTab.Notices -> repository.fetchNotices(page).mapSuccess { UserSpaceContent.Notices(it) }
    }
}

private fun cachedContent(
    repository: me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository,
    userId: UserId?,
    tab: UserSpaceTab,
    page: Int,
    viewAllType: YamiboRoute.UserSpace.Blog.ViewAllType,
): UserSpaceContent? {
    return when (tab) {
        UserSpaceTab.Profile -> repository.getCachedProfile(userId)?.let { UserSpaceContent.Profile(it) }
        UserSpaceTab.Threads -> repository.getCachedThreads(userId, page)?.let { UserSpaceContent.Threads(it) }
        UserSpaceTab.Replies -> repository.getCachedReplies(userId, page)?.let { UserSpaceContent.Replies(it) }
        UserSpaceTab.MyBlogs -> repository.getCachedMyBlogs(userId, page)?.let { UserSpaceContent.Blogs(it) }
        UserSpaceTab.FriendBlogs -> repository.getCachedFriendBlogs(page)?.let { UserSpaceContent.Blogs(it) }
        UserSpaceTab.ViewAllBlogs -> repository.getCachedViewAllBlogs(viewAllType, page)?.let { UserSpaceContent.Blogs(it) }
        UserSpaceTab.Friends -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.MyFriend, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Online -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.OnlineMember, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Visitors -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.MyVisitor, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Traces -> repository.getCachedFriends(YamiboRoute.UserSpace.FriendPageType.MyTrace, page)?.let { UserSpaceContent.Friends(it) }
        UserSpaceTab.Messages -> repository.getCachedPrivateMessages(page)?.let { UserSpaceContent.Messages(it) }
        UserSpaceTab.Notices -> repository.getCachedNotices(page)?.let { UserSpaceContent.Notices(it) }
    }
}

private fun UserSpaceContent.pageNumber(): Int? = when (this) {
    is UserSpaceContent.Profile -> null
    is UserSpaceContent.Threads -> page.pageNav?.currentPage
    is UserSpaceContent.Replies -> page.pageNav?.currentPage
    is UserSpaceContent.Blogs -> page.pageNav?.currentPage
    is UserSpaceContent.Friends -> page.pageNav?.currentPage
    is UserSpaceContent.Messages -> page.pageNav?.currentPage
    is UserSpaceContent.Notices -> page.pageNav?.currentPage
}

private fun <T, R> YamiboResult<T>.mapSuccess(transform: (T) -> R): YamiboResult<R> = when (this) {
    is YamiboResult.Success -> YamiboResult.Success(transform(value))
    is YamiboResult.Failure -> this
    is YamiboResult.NotLoggedIn -> this
    is YamiboResult.NoPermission -> this
    is YamiboResult.Maintenance -> this
}

private fun topBarTitle(
    profile: ProfilePage?,
    titleHint: String?,
    group: UserSpaceGroup,
    tab: UserSpaceTab,
    isSelf: Boolean,
): String {
    val name = profile?.username ?: titleHint ?: "用戶"
    return when (group) {
        UserSpaceGroup.Space -> if (isSelf) "我的資料" else "${name}的資料"
        UserSpaceGroup.Threads -> if (isSelf) "我的主題" else "${name} - Ta的主題"
        UserSpaceGroup.Blogs -> if (isSelf) "我的日志" else "${name} - Ta的日志"
        UserSpaceGroup.Friends -> tab.title(isSelf)
        UserSpaceGroup.Messages -> tab.title(isSelf)
    }
}

private fun navigateThread(thread: ThreadSummary, navigator: ComposableNavigator) {
    val fid = thread.fid
    if (fid != null && YamiboForum.isNovelForum(fid)) {
        navigator.navigate(INovelThreadDetailScreen(thread.tid, thread.title, thread.author?.uid))
    } else {
        navigator.navigate(IThreadReaderScreen(tid = thread.tid, title = thread.title))
    }
}

private fun fullYamiboUrl(url: String): String =
    if (url.startsWith("http")) url else "${YamiboRoute.Domain.build()}${url.removePrefix("/")}"

