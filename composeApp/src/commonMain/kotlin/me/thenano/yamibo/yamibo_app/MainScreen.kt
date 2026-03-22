package me.thenano.yamibo.yamibo_app

import YamiboIcons
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.thenano.yamibo.yamibo_app.home.BottomNavItem
import me.thenano.yamibo.yamibo_app.home.HomePageBottomBar
import me.thenano.yamibo.yamibo_app.favorite.FavoritePage
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.auth.ProfilePage
import me.thenano.yamibo.yamibo_app.history.ReadHistoryPage
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

@Composable
fun MainScreen(initialTab: MainTab = MainTab.Home) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    var currentTab by rememberSaveable { mutableStateOf(initialTab) }

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
            HomePageBottomBar(
                tabs = MainTab.entries.map { BottomNavItem(it.title, it.icon) },
                currentTab = BottomNavItem(currentTab.title, currentTab.icon),
                onTabSelected = { selected ->
                    currentTab = MainTab.entries.first { it.title == selected.title }
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
            Crossfade(
                targetState = currentTab,
                label = "MainScreenTabCrossfade"
            ) { tab ->
                when (tab) {
                    MainTab.Home -> HomeScreenContent()
                    MainTab.History -> ReadHistoryPage()
                    MainTab.Message -> PlaceholderScreen("Message")
                    MainTab.Favorite -> FavoritePage()
                    MainTab.Profile -> ProfilePage()
                }
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
