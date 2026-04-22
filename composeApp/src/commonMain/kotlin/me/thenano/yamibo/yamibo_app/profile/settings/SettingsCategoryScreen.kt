package me.thenano.yamibo.yamibo_app.profile.settings

import YamiboIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalDiskCacheFactory
import me.thenano.yamibo.yamibo_app.LocalFavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.favorite.IFavoriteCategoryManageScreen
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncStatusCard
import me.thenano.yamibo.yamibo_app.favorite.sync.IFavoriteSyncProgressScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.access.IBackgroundAccessSetupScreen
import me.thenano.yamibo.yamibo_app.profile.settings.bound.MangaReadingModeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.MangaTouchZoneSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelContentWidthSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelFontSizeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelLineSpacingSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelReaderPreviewSetting
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteSortMode
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryScreen(category: String) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    val title = when (category) {
        "appearance" -> "外觀"
        "novel_reader" -> "小說閱讀器"
        "manga_reader" -> "漫畫閱讀器"
        "favorite" -> "收藏"
        "storage" -> "儲存空間"
        "sign" -> "簽到"
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.creamBackground,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            when (category) {
                "appearance" -> AppearanceContent()
                "novel_reader" -> NovelReaderContent()
                "manga_reader" -> MangaReaderContent()
                "favorite" -> FavoriteSettingsContent(snackbarHostState)
                "storage" -> StorageContent(snackbarHostState)
                "sign" -> SignSettingsContent()
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
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun AppearanceContent() {
    val appSettingsRepo = LocalAppSettingsRepository.current
    val themeMode = appSettingsRepo.themeMode.state()
    val themeScheme = appSettingsRepo.themeScheme.state()

    ThemeSelectorContent(
        currentMode = themeMode,
        currentScheme = themeScheme,
        onModeChange = { appSettingsRepo.themeMode.setValue(it) },
        onSchemeChange = { appSettingsRepo.themeScheme.setValue(it) },
    )
}

@Composable
private fun NovelReaderContent() {
    SectionLabel("預覽")
    NovelReaderPreviewSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel("字體大小")
    NovelFontSizeSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel("行距")
    NovelLineSpacingSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel("內容寬度")
    NovelContentWidthSetting()
}

@Composable
private fun MangaReaderContent() {
    SectionLabel("閱讀模式")
    MangaReadingModeSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel("觸控分區")
    MangaTouchZoneSetting()
}

@Composable
private fun FavoriteSettingsContent(snackbarHostState: SnackbarHostState) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val appSettingsRepository = LocalAppSettingsRepository.current
    val favoriteSyncRunner = LocalFavoriteSyncRunner.current
    val skipConfirm = appSettingsRepository.skipFavoriteRemovalConfirm.state()
    val addSyncPromptEnabled = appSettingsRepository.favoriteAddSyncPromptEnabled.state()
    val addSyncDefault = appSettingsRepository.favoriteAddSyncDefault.state()
    val removeSyncPromptEnabled = appSettingsRepository.favoriteRemoveSyncPromptEnabled.state()
    val removeSyncDefault = appSettingsRepository.favoriteRemoveSyncDefault.state()
    val gridMode = appSettingsRepository.favoriteGridMode.state()
    val sortMode = appSettingsRepository.favoriteSortMode.state()
    val sortDescending = appSettingsRepository.favoriteSortDescending.state()
    val syncState by favoriteSyncRunner.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    SectionLabel("收藏管理")
    SettingsActionRow(
        title = "管理收藏類別",
        subtitle = "新增、編輯、刪除或調整你的收藏類別",
        onClick = { navigator.navigate(IFavoriteCategoryManageScreen()) },
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel("收藏顯示")
    Text(
        text = "排列方式",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = AppSettingsRepository.favoriteGridModeOptions,
        selectedValue = gridMode,
        onSelect = { appSettingsRepository.favoriteGridMode.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(18.dp))
    Text(
        text = "排序方式",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = AppSettingsRepository.favoriteSortModeOptions.map { (mode, label) ->
            mode to if (mode == sortMode) "$label${if (sortDescending) " ↓" else " ↑"}" else label
        },
        selectedValue = sortMode,
        onSelect = {
            if (it == sortMode) {
                appSettingsRepository.favoriteSortDescending.setValue(!sortDescending)
            } else {
                appSettingsRepository.favoriteSortMode.setValue(it)
                appSettingsRepository.favoriteSortDescending.setValue(
                    it != FavoriteSortMode.NAME && it != FavoriteSortMode.FORUM_NAME
                )
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel("收藏刪除")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { appSettingsRepository.skipFavoriteRemovalConfirm.setValue(!skipConfirm) }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "略過刪除確認",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "開啟後，刪除收藏時不再跳出確認視窗。",
                fontSize = 13.sp,
                color = colors.textDark.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = skipConfirm,
            onCheckedChange = { appSettingsRepository.skipFavoriteRemovalConfirm.setValue(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.brownDeep,
                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = colors.textDark.copy(alpha = 0.5f),
                uncheckedTrackColor = colors.brownLight.copy(alpha = 0.3f),
            ),
        )
    }

    Spacer(Modifier.height(24.dp))

    SectionLabel("收藏同步偏好")
    SettingsActionRow(
        title = "通知與背景同步設定",
        subtitle = "檢查通知權限、電池最佳化與背景同步所需的系統設定。",
        onClick = { navigator.navigate(IBackgroundAccessSetupScreen()) },
    )

    Spacer(Modifier.height(18.dp))

    SettingsToggleRow(
        title = "新增收藏時詢問同步",
        subtitle = "開啟後，新建收藏時會詢問是否同步到百合會。",
        checked = addSyncPromptEnabled,
        onCheckedChange = { appSettingsRepository.favoriteAddSyncPromptEnabled.setValue(it) },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "新增收藏預設動作",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = listOf(true to "同步到百合會", false to "只存本地"),
        selectedValue = addSyncDefault,
        onSelect = { appSettingsRepository.favoriteAddSyncDefault.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(18.dp))
    SettingsToggleRow(
        title = "完全移除收藏時詢問同步刪除",
        subtitle = "開啟後，收藏將完全消失時會詢問是否同步從百合會移除。",
        checked = removeSyncPromptEnabled,
        onCheckedChange = { appSettingsRepository.favoriteRemoveSyncPromptEnabled.setValue(it) },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = "完全移除收藏預設動作",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = listOf(true to "同步移除", false to "只刪本地"),
        selectedValue = removeSyncDefault,
        onSelect = { appSettingsRepository.favoriteRemoveSyncDefault.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel("收藏同步")
    if (syncState != FavoriteSyncState.Idle) {
        FavoriteSyncStatusCard(
            state = syncState,
            modifier = Modifier.fillMaxWidth(),
            onOpenProgress = {
                settingsCurrentSyncRunId(syncState)?.let { navigator.navigate(IFavoriteSyncProgressScreen(it)) }
            },
            onResume = {
                coroutineScope.launch {
                    when (val result = favoriteSyncRunner.resumeInterruptedImport()) {
                        null -> Unit
                        is me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner.LaunchResult.Started -> {
                            navigator.navigate(IFavoriteSyncProgressScreen(result.runId))
                        }
                        is me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner.LaunchResult.Rejected -> {
                            snackbarHostState.showSnackbar(result.reason)
                        }
                    }
                }
            },
            onInterrupt = {
                val runId = settingsCurrentSyncRunId(syncState)
                if (runId != null) {
                    coroutineScope.launch {
                        favoriteSyncRunner.interruptImport(runId)
                    }
                }
            },
        )
    } else {
        Text(
            text = "這裡會顯示最近一次收藏同步的狀態與進度。同步開始後，你也可以從這裡重新打開進度畫面。",
            fontSize = 13.sp,
            color = colors.textDark.copy(alpha = 0.68f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.textDark.copy(alpha = 0.6f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.brownDeep,
                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = colors.textDark.copy(alpha = 0.5f),
                uncheckedTrackColor = colors.brownLight.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.textDark.copy(alpha = 0.6f),
            )
        }
        Text(
            text = ">",
            color = colors.textDark.copy(alpha = 0.35f),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StorageContent(snackbarHostState: SnackbarHostState) {
    val colors = YamiboTheme.colors
    val appSettingsRepo = LocalAppSettingsRepository.current
    val diskCacheFactory = LocalDiskCacheFactory.current
    val coroutineScope = rememberCoroutineScope()

    val clearOnLaunch = appSettingsRepo.clearCacheOnAppLaunch.state()
    var cacheSizeText by remember { mutableStateOf("正在計算中...") }

    LaunchedEffect(Unit) {
        val size = diskCacheFactory.getTotalCacheSizeBytes() ?: 0L
        cacheSizeText = if (size > 1024 * 1024) {
            "${(size / (1024f * 1024f) * 100).roundToInt() / 100f} MB"
        } else {
            "${(size / 1024f * 100).roundToInt() / 100f} kB"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { appSettingsRepo.clearCacheOnAppLaunch.setValue(!clearOnLaunch) }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "App 啟動時清除快取",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark,
            )
        }
        Switch(
            checked = clearOnLaunch,
            onCheckedChange = { appSettingsRepo.clearCacheOnAppLaunch.setValue(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.brownDeep,
                checkedTrackColor = colors.brownPrimary.copy(alpha = 0.5f),
                uncheckedThumbColor = colors.textDark.copy(alpha = 0.5f),
                uncheckedTrackColor = colors.brownLight.copy(alpha = 0.3f),
            ),
        )
    }

    Spacer(Modifier.height(24.dp))

    SettingsActionRow(
        title = "立即清除所有快取",
        subtitle = "清除圖片、頁面與其他暫存資料，釋放目前已使用的儲存空間。\n目前快取大小：$cacheSizeText",
        onClick = {
            coroutineScope.launch {
                diskCacheFactory.clearAllCache()
                cacheSizeText = "0 kB"
                snackbarHostState.showSnackbar("已清除所有快取")
            }
        },
    )
}

@Composable
private fun SignSettingsContent() {
    val colors = YamiboTheme.colors
    val appSettingsRepository = LocalAppSettingsRepository.current
    val signMode = appSettingsRepository.signInMode.state()
    val allowRepair = appSettingsRepository.signInAllowRepair.state()

    SectionLabel("簽到模式")
    Text(
        text = "簽到行為",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = AppSettingsRepository.signInModeOptions,
        selectedValue = signMode,
        onSelect = { appSettingsRepository.signInMode.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel("補簽")
    SettingsToggleRow(
        title = "自動進行補簽",
        subtitle = when (signMode) {
            SignInMode.FULL_MANUAL -> "手動模式下只保存這個偏好；半自動模式才會真的自動補簽。"
            SignInMode.SEMI_AUTOMATIC -> "完成 Cloudflare 驗證後，程式會在簽到成功後一路補到不能再補。"
        },
        checked = allowRepair,
        onCheckedChange = { appSettingsRepository.signInAllowRepair.setValue(it) },
    )
}

private fun settingsCurrentSyncRunId(state: FavoriteSyncState): String? {
    return when (state) {
        FavoriteSyncState.Idle -> null
        is FavoriteSyncState.Running -> state.snapshot.runId
        is FavoriteSyncState.Interrupted -> state.snapshot.runId
        is FavoriteSyncState.Failed -> state.snapshot.runId
        is FavoriteSyncState.Completed -> state.snapshot.runId
    }
}
