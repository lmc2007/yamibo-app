package me.thenano.yamibo.yamibo_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.thenano.yamibo.yamibo_app.component.BottomNavItem
import me.thenano.yamibo.yamibo_app.component.HomePageBottomBar
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.profile.ProfilePage
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

enum class MainTab(val title: String, val icon: ImageVector) {
    Home("首页", YamiboIcons.Home),
    Message("消息", YamiboIcons.Message),
    Profile("我的", YamiboIcons.Profile)
}

class IMainScreen : Navigatable {
    override val id = "MainScreen"

    @Composable
    override fun Content() {
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    val colors = YamiboTheme.colors
    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
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
            when (currentTab) {
                MainTab.Home -> HomeScreenContent()
                MainTab.Message -> PlaceholderScreen("Message")
                MainTab.Profile -> ProfilePage()
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
