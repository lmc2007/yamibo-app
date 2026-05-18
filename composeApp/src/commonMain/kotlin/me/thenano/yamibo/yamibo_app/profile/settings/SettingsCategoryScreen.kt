package me.thenano.yamibo.yamibo_app.profile.settings

import me.thenano.yamibo.yamibo_app.i18n.appString
import me.thenano.yamibo.yamibo_app.i18n.localizedAppMessage
import me.thenano.yamibo.yamibo_app.i18n.localizedLabel
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import me.thenano.yamibo.yamibo_app.core.cache.CacheStorageBreakdown
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalDiskCacheFactory
import me.thenano.yamibo.yamibo_app.LocalFavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.LocalFavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.favorite.IFavoriteCategoryManageScreen
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncStatusCard
import me.thenano.yamibo.yamibo_app.favorite.sync.IFavoriteSyncProgressScreen
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.access.IBackgroundAccessSetupScreen
import me.thenano.yamibo.yamibo_app.profile.settings.bound.MangaReadingModeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.MangaTouchZoneSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelContentWidthSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelChineseConversionSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelFontSizeSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelLineSpacingSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelReaderPreviewSetting
import me.thenano.yamibo.yamibo_app.profile.settings.bound.NovelSystemBarsBackgroundSetting
import me.thenano.yamibo.yamibo_app.profile.settings.components.SettingsChipRow
import me.thenano.yamibo.yamibo_app.profile.settings.components.ThemeSelectorContent
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository.FavoriteSyncState
import me.thenano.yamibo.yamibo_app.repository.settings.AppLanguage
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteGridMode
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteSortMode
import me.thenano.yamibo.yamibo_app.repository.settings.FavoriteUpdateInterval
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryScreen(category: String) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current

    val title = when (category) {
        "appearance" -> stringResource(Res.string.settings_appearance_title)
        "language" -> stringResource(Res.string.settings_language_title)
        "novel_reader" -> stringResource(Res.string.settings_novel_reader_title)
        "manga_reader" -> stringResource(Res.string.settings_manga_reader_title)
        "favorite" -> stringResource(Res.string.settings_favorite_title)
        "storage" -> stringResource(Res.string.settings_storage_title)
        "sign" -> stringResource(Res.string.settings_sign_title)
        else -> stringResource(Res.string.settings_title)
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
        snackbarHost = { YamiboSnackbarHost(snackbarHostState) },
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
                "language" -> LanguageContent()
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
private fun LanguageContent() {
    val appSettingsRepo = LocalAppSettingsRepository.current
    val language = appSettingsRepo.language.state()

    SectionLabel(stringResource(Res.string.language_section_title))
    Text(
        text = stringResource(Res.string.language_current_title),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = YamiboTheme.colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = listOf(
            AppLanguage.TRADITIONAL_CHINESE to stringResource(Res.string.language_traditional_chinese),
            AppLanguage.SIMPLIFIED_CHINESE to stringResource(Res.string.language_simplified_chinese),
            AppLanguage.ENGLISH to stringResource(Res.string.language_english),
        ),
        selectedValue = language,
        onSelect = { appSettingsRepo.language.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun NovelReaderContent() {
    SectionLabel(appString(Res.string.ui_preview))
    NovelReaderPreviewSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_font_size))
    NovelFontSizeSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_line_spacing))
    NovelLineSpacingSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_content_width))
    NovelContentWidthSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_system_column))
    NovelSystemBarsBackgroundSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_conversion_between_simplified_traditional_chinese))
    NovelChineseConversionSetting()
}

@Composable
private fun MangaReaderContent() {
    SectionLabel(appString(Res.string.ui_reading_mode))
    MangaReadingModeSetting()
    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_touch_area_2))
    MangaTouchZoneSetting()
}

@Composable
private fun FavoriteSettingsContent(snackbarHostState: SnackbarHostState) {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val appSettingsRepository = LocalAppSettingsRepository.current
    val favoriteSyncRunner = LocalFavoriteSyncRunner.current
    val favoriteUpdateRunner = LocalFavoriteUpdateRunner.current
    val skipConfirm = appSettingsRepository.skipFavoriteRemovalConfirm.state()
    val addSyncPromptEnabled = appSettingsRepository.favoriteAddSyncPromptEnabled.state()
    val addSyncDefault = appSettingsRepository.favoriteAddSyncDefault.state()
    val removeSyncPromptEnabled = appSettingsRepository.favoriteRemoveSyncPromptEnabled.state()
    val removeSyncDefault = appSettingsRepository.favoriteRemoveSyncDefault.state()
    val gridMode = appSettingsRepository.favoriteGridMode.state()
    val sortMode = appSettingsRepository.favoriteSortMode.state()
    val sortDescending = appSettingsRepository.favoriteSortDescending.state()
    val updateInterval = appSettingsRepository.favoriteUpdateInterval.state()
    val syncState by favoriteSyncRunner.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    SectionLabel(appString(Res.string.settings_favorite_title))
    SettingsActionRow(
        title = appString(Res.string.ui_manage_favorite_categories),
        subtitle = appString(Res.string.ui_add_edit_delete_adjust_favorite_categories),
        onClick = { navigator.navigate(IFavoriteCategoryManageScreen()) },
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_favorite_display))
    Text(
        text = appString(Res.string.ui_arrangement),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = FavoriteGridMode.entries.map { it to it.localizedLabel() },
        selectedValue = gridMode,
        onSelect = { appSettingsRepository.favoriteGridMode.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(18.dp))
    Text(
        text = appString(Res.string.ui_sort_by),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = FavoriteSortMode.entries.map { mode ->
            val label = mode.localizedLabel()
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

    SectionLabel(appString(Res.string.ui_favorite_delete))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { appSettingsRepository.skipFavoriteRemovalConfirm.setValue(!skipConfirm) }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appString(Res.string.ui_skip_deletion_confirmation),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = appString(Res.string.ui_after_turning_on_confirmation_window_no_longer_pop_up_deleting),
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

    SectionLabel(appString(Res.string.ui_favorite_sync_preferences))
    SettingsActionRow(
        title = appString(Res.string.ui_notification_background_synchronization_settings),
        subtitle = appString(Res.string.ui_check_system_settings_required_for_notification_permissions_battery),
        onClick = { navigator.navigate(IBackgroundAccessSetupScreen()) },
    )

    Spacer(Modifier.height(18.dp))
    Text(
        text = appString(Res.string.ui_favorite_update_check_cycle),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = FavoriteUpdateInterval.entries.map { it to it.localizedLabel() },
        selectedValue = updateInterval,
        onSelect = { interval ->
            appSettingsRepository.favoriteUpdateInterval.setValue(interval)
            coroutineScope.launch {
                favoriteUpdateRunner.schedulePeriodicUpdate(interval)
                if (interval == FavoriteUpdateInterval.SMART) {
                    snackbarHostState.showSnackbar(appString(Res.string.ui_the_smart_update_policy_has_not_accessed_yet_periodic_background))
                }
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(18.dp))

    SettingsToggleRow(
        title = appString(Res.string.ui_ask_for_synchronization_adding_favorite),
        subtitle = appString(Res.string.ui_after_turning_on_creating_new_favorite_asked_whether_synchronize),
        checked = addSyncPromptEnabled,
        onCheckedChange = { appSettingsRepository.favoriteAddSyncPromptEnabled.setValue(it) },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = appString(Res.string.ui_added_new_favorite_preset_actions),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = listOf(true to appString(Res.string.ui_sync_yamibo), false to appString(Res.string.ui_only_save_local)),
        selectedValue = addSyncDefault,
        onSelect = { appSettingsRepository.favoriteAddSyncDefault.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(18.dp))
    SettingsToggleRow(
        title = appString(Res.string.ui_ask_for_simultaneous_deletion_completely_removing_favorites),
        subtitle = appString(Res.string.ui_after_turning_on_favorite_disappears_completely_asked_whether),
        checked = removeSyncPromptEnabled,
        onCheckedChange = { appSettingsRepository.favoriteRemoveSyncPromptEnabled.setValue(it) },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = appString(Res.string.ui_completely_remove_favorite_preset_actions),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = listOf(true to appString(Res.string.ui_synchronous_removal), false to appString(Res.string.ui_only_delete_local)),
        selectedValue = removeSyncDefault,
        onSelect = { appSettingsRepository.favoriteRemoveSyncDefault.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_favorite_synchronization))
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
            text = appString(Res.string.ui_the_status_progress_latest_favorite_synchronization_displayed_here),
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) }
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
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
    var cacheSizeText by remember { mutableStateOf(appString(Res.string.ui_calculating)) }
    var cacheBreakdown by remember { mutableStateOf(CacheStorageBreakdown(rootPath = "", usages = emptyList())) }

    suspend fun refreshCacheUsage() {
        cacheBreakdown = diskCacheFactory.getCacheStorageBreakdown()
        cacheSizeText = formatStorageSize(cacheBreakdown.usages.sumOf { it.bytes })
    }

    LaunchedEffect(Unit) {
        refreshCacheUsage()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { appSettingsRepo.clearCacheOnAppLaunch.setValue(!clearOnLaunch) }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appString(Res.string.ui_clear_cache_app_starts),
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

    if (cacheBreakdown.usages.isNotEmpty()) {
        StorageUsageOverview(cacheBreakdown)
        Spacer(Modifier.height(18.dp))
    }

    SettingsActionRow(
        title = appString(Res.string.ui_clear_all_caches_now),
        subtitle = appString(Res.string.settings_storage_subtitle_with_size, cacheSizeText),
        onClick = {
            coroutineScope.launch {
                diskCacheFactory.clearAllCache()
                cacheSizeText = "0 kB"
                cacheBreakdown = cacheBreakdown.copy(usages = emptyList())
                snackbarHostState.showSnackbar(appString(Res.string.ui_all_cache_cleared))
            }
        },
    )
}

@Composable
private fun StorageUsageOverview(breakdown: CacheStorageBreakdown) {
    val colors = YamiboTheme.colors
    val usageColors = mapOf(
        "images" to Color(0xFF5C8DD6),
        "pages" to Color(0xFFB66D32),
        "userspace" to Color(0xFF7D63B8),
        "other" to Color(0xFF8A7D70),
    )
    val totalBytes = breakdown.usages.sumOf { it.bytes }.coerceAtLeast(1L)

    Text(
        text = appString(Res.string.ui_storage_usage),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.brownDeep,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = breakdown.rootPath,
        fontSize = 12.sp,
        color = colors.textDark.copy(alpha = 0.62f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.brownLight.copy(alpha = 0.32f)),
    ) {
        breakdown.usages.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((item.bytes.toFloat() / totalBytes.toFloat()).coerceAtLeast(0.01f))
                    .background(usageColors[item.key] ?: colors.brownPrimary),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    breakdown.usages.forEach { item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(usageColors[item.key] ?: colors.brownPrimary),
            )
            Text(
                text = localizedAppMessage(item.label),
                color = colors.textDark,
                fontSize = 13.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                text = formatStorageSize(item.bytes),
                color = colors.textDark.copy(alpha = 0.62f),
                fontSize = 12.sp,
            )
        }
    }
}

private fun formatStorageSize(size: Long): String {
    return when {
        size >= 1024L * 1024L * 1024L -> "${(size / (1024f * 1024f * 1024f) * 100).roundToInt() / 100f} GB"
        size >= 1024L * 1024L -> "${(size / (1024f * 1024f) * 100).roundToInt() / 100f} MB"
        else -> "${(size / 1024f * 100).roundToInt() / 100f} kB"
    }
}

@Composable
private fun SignSettingsContent() {
    val colors = YamiboTheme.colors
    val appSettingsRepository = LocalAppSettingsRepository.current
    val signMode = appSettingsRepository.signInMode.state()
    val allowRepair = appSettingsRepository.signInAllowRepair.state()

    SectionLabel(appString(Res.string.ui_check_in_mode))
    Text(
        text = appString(Res.string.ui_check_in_behavior),
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = colors.textDark,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    Spacer(Modifier.height(6.dp))
    SettingsChipRow(
        options = SignInMode.entries.map { it to it.localizedLabel() },
        selectedValue = signMode,
        onSelect = { appSettingsRepository.signInMode.setValue(it) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )

    Spacer(Modifier.height(24.dp))

    SectionLabel(appString(Res.string.ui_re_signing))
    SettingsToggleRow(
        title = appString(Res.string.ui_automatic_re_signing),
        subtitle = when (signMode) {
            SignInMode.FULL_MANUAL -> appString(Res.string.ui_in_manual_mode_only_preference_saved_in_semi_automatic_mode)
            SignInMode.SEMI_AUTOMATIC -> appString(Res.string.ui_after_completing_cloudflare_verification_program_fill_in_all_time)
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

