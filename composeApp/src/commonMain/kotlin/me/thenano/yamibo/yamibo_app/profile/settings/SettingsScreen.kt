package me.thenano.yamibo.yamibo_app.profile.settings

import YamiboIcons
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsItem
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "設定",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.brownDeep,
                    scrolledContainerColor = colors.brownDeep,
                ),
            )
        },
        containerColor = colors.creamBackground,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsItem(
                icon = YamiboIcons.Views,
                title = "外觀",
                subtitle = "主題色、配色與整體顯示風格",
                onClick = { navigator.navigate(ISettingsCategoryScreen("appearance")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Book,
                title = "小說閱讀器",
                subtitle = "字體大小、行距與版面寬度",
                onClick = { navigator.navigate(ISettingsCategoryScreen("novel_reader")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Book,
                title = "漫畫閱讀器",
                subtitle = "閱讀模式與觸控區域設定",
                onClick = { navigator.navigate(ISettingsCategoryScreen("manga_reader")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Explore,
                title = "收藏管理",
                subtitle = "管理類別、整理排序與收藏互動設定",
                onClick = { navigator.navigate(ISettingsCategoryScreen("favorite")) },
            )
            SettingsDivider()

            SettingsItem(
                icon = YamiboIcons.Storage,
                title = "儲存空間",
                subtitle = "緩存空間與啟動時清理設定",
                onClick = { navigator.navigate(ISettingsCategoryScreen("storage")) },
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    val colors = YamiboTheme.colors
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = colors.brownLight.copy(alpha = 0.15f),
    )
}
