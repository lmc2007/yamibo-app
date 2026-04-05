package me.thenano.yamibo.yamibo_app.profile.settings

import YamiboIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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

/** 設定主畫面 — 分類入口列表 */
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
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Text("◀", color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.brownDeep,
                    scrolledContainerColor = colors.brownDeep
                )
            )
        },
        containerColor = colors.creamBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsItem(
                icon = YamiboIcons.Explore,
                title = "外觀",
                subtitle = "主題、配色方案",
                onClick = {
                    navigator.navigate(ISettingsCategoryScreen("appearance"))
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colors.brownLight.copy(alpha = 0.15f)
            )

            SettingsItem(
                icon = YamiboIcons.Book,
                title = "小說閱讀器",
                subtitle = "字體大小、行距",
                onClick = {
                    navigator.navigate(ISettingsCategoryScreen("novel_reader"))
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colors.brownLight.copy(alpha = 0.15f)
            )

            SettingsItem(
                icon = YamiboIcons.Views,
                title = "漫畫閱讀器",
                subtitle = "翻頁模式、觸控區域",
                onClick = {
                    navigator.navigate(ISettingsCategoryScreen("manga_reader"))
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = colors.brownLight.copy(alpha = 0.15f)
            )
        }
    }
}
