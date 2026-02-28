package me.thenano.yamibo.yamibo_app.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.model.ForumSummary
import io.github.littlesurvival.dto.page.ForumCategory
import io.github.littlesurvival.dto.page.HomePage
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalForumRepository
import me.thenano.yamibo.yamibo_app.forum.IForumScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.logo_homepage

/** Sealed state for the home page */
private sealed interface HomeState {
    data object Loading : HomeState
    data class Success(val page: HomePage) : HomeState
    data class Error(val message: String) : HomeState
}

/** Main Entry */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen() {
    val colors = YamiboTheme.colors
    val forumRepository = LocalForumRepository.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var state by remember { mutableStateOf<HomeState>(HomeState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }

    suspend fun mapFetchResultState() {
        val result = forumRepository.fetchHomePage()
        state =
                when (result) {
                    is YamiboResult.Success -> HomeState.Success(result.value)
                    is YamiboResult.Failure -> HomeState.Error(result.reason)
                    is YamiboResult.Maintenance -> HomeState.Error(result.message())
                    is YamiboResult.NotLoggedIn -> HomeState.Error(result.message())
                }
    }

    /** initial load — use cache if available, only fetch on first cold start */
    LaunchedEffect(Unit) {
        val cached = forumRepository.getCachedHomePage()
        if (cached != null) {
            state = HomeState.Success(cached)
            return@LaunchedEffect
        }
        mapFetchResultState()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.creamBackground)) {
        when (val currentState = state) {
            is HomeState.Loading -> LoadingSkeleton()
            is HomeState.Error ->
                    ErrorContent(
                            message = currentState.message,
                            onRetry = {
                                state = HomeState.Loading
                                scope.launch { mapFetchResultState() }
                            }
                    )
            is HomeState.Success ->
                    PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                scope.launch {
                                    val result = forumRepository.fetchHomePage()
                                    when (result) {
                                        is YamiboResult.Success ->
                                                state = HomeState.Success(result.value)
                                        is YamiboResult.Failure -> {
                                            snackbarHostState.showSnackbar(
                                                    message = "刷新失敗：${result.reason}",
                                                    duration = SnackbarDuration.Short
                                            )
                                        }
                                        is YamiboResult.Maintenance -> {
                                            snackbarHostState.showSnackbar(
                                                    message = "伺服器維護中",
                                                    duration = SnackbarDuration.Short
                                            )
                                        }
                                        is YamiboResult.NotLoggedIn -> {
                                            snackbarHostState.showSnackbar(
                                                    message = "未登入",
                                                    duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                    ) { HomeContent(homePage = currentState.page) }
        }

        /** Snackbar overlay */
        SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
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

/** Success Content */
@Composable
private fun HomeContent(homePage: HomePage) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        /** header banner */
        item { HomeHeader() }

        /** categories */
        homePage.categories.forEachIndexed { index, category ->
            item(key = "cat_${category.title}") {
                CategorySection(category = category, initialExpanded = index < 3)
            }
        }
    }
}

/** Header Banner */
@Composable
private fun HomeHeader() {
    val colors = YamiboTheme.colors
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    Brush.verticalGradient(
                                            colors =
                                                    listOf(
                                                            colors.brownDeep,
                                                            colors.brownPrimary,
                                                            colors.creamBackground,
                                                    )
                                    )
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.CenterStart
    ) {
        Image(
                painter = painterResource(Res.drawable.logo_homepage),
                contentDescription = "logo_homepage",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.height(40.dp)
        )
    }
}

/** Category Section (collapsible) */
@Composable
private fun CategorySection(category: ForumCategory, initialExpanded: Boolean = true) {
    val colors = YamiboTheme.colors
    var expanded by rememberSaveable { mutableStateOf(initialExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        /** category header bar */
        Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                onClick = { expanded = !expanded }
        ) {
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .background(
                                            Brush.horizontalGradient(
                                                    colors =
                                                            listOf(
                                                                    colors.brownDeep,
                                                                    colors.brownPrimary.copy(
                                                                            alpha = 0.85f
                                                                    )
                                                            )
                                            ),
                                            shape = RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                            text = category.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                    )
                    val rotation by
                            animateFloatAsState(
                                    targetValue = if (expanded) 180f else 0f,
                                    animationSpec = tween(300),
                                    label = "chevron_rotation"
                            )
                    Text(
                            text = "▼",
                            modifier = Modifier.graphicsLayer { rotationZ = rotation },
                            color = colors.orangeAccent,
                            fontSize = 14.sp
                    )
                }
            }
        }

        /** forums list */
        AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(300)) + fadeIn(tween(300)),
                exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 4.dp)
            ) { category.forums.forEach { forum -> ForumCard(forum = forum) } }
        }
    }
}

/** Forum Card */
@Composable
private fun ForumCard(forum: ForumSummary) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by
            animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1f,
                    animationSpec = tween(150)
            )

    Card(
            modifier =
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).scale(scale).clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                            ) { navigator.navigate(IForumScreen(forum.fid)) },
            shape = RoundedCornerShape(16.dp),
            elevation =
                    CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            /** left accent bar */
            Box(
                    modifier =
                            Modifier.width(4.dp)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                            Brush.verticalGradient(
                                                    listOf(colors.orangeAccent, colors.brownPrimary)
                                            )
                                    )
            )
            Spacer(Modifier.width(14.dp))

            /** text content */
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = forum.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                if (!forum.description.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                            text = forum.description!!,
                            fontSize = 12.sp,
                            color = colors.brownPrimary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
            }

            /** today count badge */
            val today = forum.todayCount ?: 0
            if (today > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.orangeAccent.copy(alpha = 0.18f)
                ) {
                    Text(
                            text = "今日 $today",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.brownDeep
                    )
                }
            }
        }
    }
}

/** Loading Skeleton */
@Composable
private fun LoadingSkeleton() {
    val colors = YamiboTheme.colors
    val shimmerColor = colors.brownLight
    val shimmerAnim = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by
            shimmerAnim.animateFloat(
                    initialValue = -300f,
                    targetValue = 600f,
                    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                    label = "shimmer_x"
            )

    LazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.creamBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        /** header placeholder */
        item {
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .shimmer(shimmerX, shimmerColor)
            )
        }

        /** category skeletons */
        items(4) {
            Column {
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .shimmer(shimmerX, shimmerColor)
                )
                Spacer(Modifier.height(8.dp))
                repeat(3) {
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .height(60.dp)
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .shimmer(shimmerX, shimmerColor)
                    )
                }
            }
        }
    }
}

/** Shimmer modifier */
private fun Modifier.shimmer(translateX: Float, baseColor: Color): Modifier =
        this.drawBehind {
            val brush =
                    Brush.linearGradient(
                            colors =
                                    listOf(
                                            baseColor.copy(alpha = 0.25f),
                                            baseColor.copy(alpha = 0.50f),
                                            baseColor.copy(alpha = 0.25f),
                                    ),
                            start = Offset(translateX, 0f),
                            end = Offset(translateX + 300f, size.height)
                    )
            drawRect(brush)
        }

/** Error Content */
@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    val colors = YamiboTheme.colors
    Box(
            modifier = Modifier.fillMaxSize().background(colors.creamBackground).padding(32.dp),
            contentAlignment = Alignment.Center
    ) {
        Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
        ) {
            Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                        text = "載入失敗",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.brownDeep
                )
                Spacer(Modifier.height(12.dp))
                Text(
                        text = message,
                        fontSize = 13.sp,
                        color = colors.brownPrimary.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                        onClick = onRetry,
                        shape = RoundedCornerShape(50),
                        color = colors.brownDeep,
                        contentColor = Color.White
                ) {
                    Text(
                            text = "重試",
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                    )
                }
            }
        }
    }
}
