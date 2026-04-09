package me.thenano.yamibo.yamibo_app.profile.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalDiskCacheFactory
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.bound.MangaReadingModeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.MangaTouchZoneSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelContentWidthSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelFontSizeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelLineSpacingSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelReaderPreviewSetting
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import kotlin.math.roundToInt

private const val PREVIEW_TEXT = "我是YamiboApp的作者TheNano，這是一個第三方個人獨立開發的開源App"

/** 根據 category 顯示對應的設定內容 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryScreen(category: String) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    val title = when (category) {
        "appearance" -> "外觀"
        "novel_reader" -> "小說閱讀器"
        "manga_reader" -> "漫畫閱讀器"
        "storage" -> "數據與存儲"
        else -> "設定"
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.creamBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when (category) {
                "appearance" -> AppearanceContent()
                "novel_reader" -> NovelReaderContent()
                "manga_reader" -> MangaReaderContent()
                "storage" -> StorageContent(snackbarHostState)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = YamiboTheme.colors
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.textDark.copy(alpha = 0.5f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ─── Appearance ───

@Composable
private fun AppearanceContent() {
    val appSettingsRepo = LocalAppSettingsRepository.current
    val themeMode = appSettingsRepo.themeMode.state()
    val themeScheme = appSettingsRepo.themeScheme.state()

    ThemeSelectorContent(
        currentMode = themeMode,
        currentScheme = themeScheme,
        onModeChange = { appSettingsRepo.themeMode.setValue(it) },
        onSchemeChange = { appSettingsRepo.themeScheme.setValue(it) }
    )
}

// ─── Novel Reader ───

@Composable
private fun NovelReaderContent() {
    // Preview Area
    SectionLabel("預覽")
    NovelReaderPreviewSetting()

    Spacer(Modifier.height(24.dp))

    // Font Size
    SectionLabel("字體大小")
    NovelFontSizeSetting()

    Spacer(Modifier.height(24.dp))

    // Line Spacing
    SectionLabel("行距")
    NovelLineSpacingSetting()

    Spacer(Modifier.height(24.dp))

    // Content Width Fraction
    SectionLabel("頁寬")
    NovelContentWidthSetting()
}

// Manga Reader

@Composable
private fun MangaReaderContent() {
    SectionLabel("閱讀模式")
    MangaReadingModeSetting()

    Spacer(Modifier.height(24.dp))

    SectionLabel("輕觸區域")
    MangaTouchZoneSetting()
}

// Storage

@Composable
private fun StorageContent(snackbarHostState: SnackbarHostState) {
    val colors = YamiboTheme.colors
    val appSettingsRepo = LocalAppSettingsRepository.current
    val diskCacheFactory = LocalDiskCacheFactory.current
    val coroutineScope = rememberCoroutineScope()
    
    val clearOnLaunch = appSettingsRepo.clearCacheOnAppLaunch.state()
    var cacheSizeText by remember { mutableStateOf("計算中...") }
    
    LaunchedEffect(Unit) {
        val size = diskCacheFactory.getTotalCacheSizeBytes() ?: 0L
        cacheSizeText = if (size > 1024 * 1024) {
            "${(size / (1024f * 1024f) * 100).roundToInt() / 100f} MB"
        } else {
            "${(size / 1024f * 100).roundToInt() / 100f} kB"
        }
    }

    // Clear Cache Action
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                coroutineScope.launch {
                    diskCacheFactory.clearAllCache()
                    val newSize = diskCacheFactory.getTotalCacheSizeBytes() ?: 0L
                    cacheSizeText = "${(newSize / 1024f * 100).roundToInt() / 100f} kB"
                    snackbarHostState.showSnackbar("快取已清除")
                }
            }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "清除快取",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "已使用: $cacheSizeText",
                fontSize = 13.sp,
                color = colors.textDark.copy(alpha = 0.6f)
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    // Clear on App Launch Switch
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                appSettingsRepo.clearCacheOnAppLaunch.setValue(!clearOnLaunch)
            }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "App啟動時清除快取",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textDark,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = clearOnLaunch,
            onCheckedChange = { appSettingsRepo.clearCacheOnAppLaunch.setValue(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.brownDeep,
                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = colors.textDark.copy(alpha = 0.5f),
                uncheckedTrackColor = colors.brownLight.copy(alpha = 0.3f)
            )
        )
    }
}
