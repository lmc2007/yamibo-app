package me.thenano.yamibo.yamibo_app

import YamiboIcons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.favorite.FavoritePage
import me.thenano.yamibo.yamibo_app.history.ReadHistoryPage
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.profile.ProfilePage
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

enum class MainTab(val title: String, val icon: ImageVector) {
    Home("首页", YamiboIcons.Home),
    History("紀錄", YamiboIcons.History),
    Message("消息", YamiboIcons.Message),
    Favorite("收藏", YamiboIcons.Explore),
    Profile("我的", YamiboIcons.Profile)
}

class IMainScreen(private val initialTab: MainTab = MainTab.Home) : Navigatable {
    override val id = buildId(initialTab.name)

    @Composable
    override fun Content() {
        MainScreen(initialTab)
    }
}

data class BottomNavItem(val title: String, val icon: ImageVector)

@Composable
fun MainScreen(initialTab: MainTab = MainTab.Home) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }
    var reTapHistoryToken by remember { mutableIntStateOf(0) }

    DisposableEffect(currentTab) {
        val handler = {
            if (navigator.currentScreen is IMainScreen && currentTab != MainTab.Home) {
                currentTab = MainTab.Home
                true
            } else {
                false
            }
        }
        navigator.backHandlers.add(handler)
        onDispose { navigator.backHandlers.remove(handler) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        containerColor = colors.creamBackground,
        bottomBar = {
            MainScreenBottomBar(
                tabs = MainTab.entries.map { BottomNavItem(it.title, it.icon) },
                currentTab = BottomNavItem(currentTab.title, currentTab.icon),
                onTabSelected = { selected ->
                    val newTab = MainTab.entries.first { it.title == selected.title }
                    if (newTab == MainTab.History && currentTab == MainTab.History) {
                        reTapHistoryToken++
                    }
                    currentTab = newTab
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier =
                Modifier.padding(paddingValues)
                    .fillMaxSize()
                    .background(colors.creamBackground)
        ) {
            MainTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val alpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(durationMillis = 220),
                    label = "MainScreenTabAlpha_${tab.name}",
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (selected) 1f else 0f)
                        .graphicsLayer {
                            this.alpha = alpha
                        }
                ) {
                    when (tab) {
                        MainTab.Home -> HomeScreenContent()
                        MainTab.History -> ReadHistoryPage(reTapHistoryToken)
                        MainTab.Message -> PlaceholderScreen("Message")
                        MainTab.Favorite -> FavoritePage()
                        MainTab.Profile -> ProfilePage()
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreenBottomBar(
    tabs: List<BottomNavItem>,
    currentTab: BottomNavItem,
    onTabSelected: (BottomNavItem) -> Unit
) {
    val colors = YamiboTheme.colors
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(56.dp)
                .background(colors.navBarBg)
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab == currentTab
            val color by
            animateColorAsState(
                targetValue = if (selected) colors.navBarIconSelected else colors.navBarIconUnselected,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )

            Column(
                modifier =
                    Modifier.weight(1f).clickable(
                        interactionSource =
                            remember { MutableInteractionSource() },
                        indication = null
                    ) { onTabSelected(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(text = tab.title, color = color, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    val colors = YamiboTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.creamBackground),
        contentAlignment = Alignment.Center
    ) { Text("Content for $name") }
}
