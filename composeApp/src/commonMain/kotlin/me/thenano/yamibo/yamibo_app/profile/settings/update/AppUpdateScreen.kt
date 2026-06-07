package me.thenano.yamibo.yamibo_app.profile.settings.update

import me.thenano.yamibo.yamibo_app.i18n.i18n

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import me.thenano.yamibo.yamibo_app.AppVersion
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalAppUpdateRepository
import me.thenano.yamibo.yamibo_app.components.controls.YamiboSingleSelectDialog
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateCheckResult
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateDownloadState
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateRelease
import me.thenano.yamibo.yamibo_app.repository.settings.AppUpdateLaunchCheckThreshold
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.util.state

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppUpdateScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val repository = LocalAppUpdateRepository.current
    val appSettingsRepository = LocalAppSettingsRepository.current
    val coroutineScope = rememberCoroutineScope()
    val downloadState by repository.downloadState.collectAsState()
    val launchCheckThreshold = appSettingsRepository.appUpdateLaunchCheckThreshold.state()

    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AppUpdateCheckResult?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    val release = (result as? AppUpdateCheckResult.UpdateAvailable)?.release
        ?: (result as? AppUpdateCheckResult.Ignored)?.release

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = i18n("App 更新"),
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.creamBackground)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppUpdateStatusCard(
                checking = checking,
                result = result,
                downloadState = downloadState,
                release = release,
            )

            AppUpdateLaunchThresholdCard(
                selected = launchCheckThreshold,
                onClick = { showThresholdDialog = true },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            checking = true
                            result = repository.checkForUpdate(force = true)
                            checking = false
                        }
                    },
                    enabled = !checking && downloadState !is AppUpdateDownloadState.Running,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brownDeep),
                ) {
                    Text(if (checking) i18n("檢查中...") else i18n("檢查更新"))
                }
                release?.let { available ->
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.downloadAndInstall(available)
                            }
                        },
                        enabled = downloadState !is AppUpdateDownloadState.Running && available.asset != null,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brownDeep),
                    ) {
                        Text(i18n("下載更新"))
                    }
                    OutlinedButton(
                        onClick = { repository.openReleasePage(available) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brownDeep),
                    ) {
                        Text(i18n("發布頁"))
                    }
                }
            }
        }
    }

    if (showThresholdDialog) {
        AppUpdateLaunchThresholdDialog(
            selected = launchCheckThreshold,
            onSelect = {
                appSettingsRepository.appUpdateLaunchCheckThreshold.setValue(it)
                showThresholdDialog = false
            },
            onDismiss = { showThresholdDialog = false },
        )
    }
}

@Composable
private fun AppUpdateStatusCard(
    checking: Boolean,
    result: AppUpdateCheckResult?,
    downloadState: AppUpdateDownloadState,
    release: AppUpdateRelease?,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when {
                    checking -> i18n("正在檢查更新")
                    result == null -> i18n("尚未檢查更新")
                    result is AppUpdateCheckResult.UpdateAvailable -> i18n("發現新版本 {}", result.release.displayVersionLabel())
                    result is AppUpdateCheckResult.Ignored -> i18n("已忽略版本 {}", result.release.displayVersionLabel())
                    result is AppUpdateCheckResult.Preparing -> i18n("新版本 {} 正在準備中", result.displayVersionLabel())
                    result is AppUpdateCheckResult.UpToDate -> i18n("目前已是最新版本：{}", AppVersion.displayName)
                    result is AppUpdateCheckResult.Failed -> i18n("檢查更新失敗")
                    else -> i18n("尚未檢查更新")
                },
                color = colors.textDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (checking) {
                CircularProgressIndicator(color = colors.brownPrimary)
            }
            if (result is AppUpdateCheckResult.Failed) {
                Text(result.message, color = colors.textDark.copy(alpha = 0.68f), fontSize = 13.sp)
            }
            if (result is AppUpdateCheckResult.Preparing) {
                Text(
                    text = i18n("更新檔案尚未發布完成，請稍後再試。來源：{}", result.sourceName),
                    color = colors.textDark.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            release?.let {
                Text(
                    text = i18n("來源：{}", it.source.name),
                    color = colors.textDark.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                )
                if (it.releaseNotes.isNotBlank()) {
                    Text(
                        text = it.releaseNotes,
                        color = colors.textDark.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            DownloadProgress(downloadState)
        }
    }
}

@Composable
private fun DownloadProgress(state: AppUpdateDownloadState) {
    val colors = YamiboTheme.colors
    when (state) {
        AppUpdateDownloadState.Idle -> Unit
        is AppUpdateDownloadState.Running -> {
            val progress = state.totalBytes?.takeIf { it > 0L }?.let {
                state.downloadedBytes.toFloat() / it.toFloat()
            }
            LinearProgressIndicator(
                progress = { progress ?: 0f },
                modifier = Modifier.fillMaxWidth(),
                color = colors.brownPrimary,
                trackColor = colors.brownLight.copy(alpha = 0.25f),
            )
            Text(
                text = i18n("正在下載更新..."),
                color = colors.textDark.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }
        is AppUpdateDownloadState.Completed -> Text(
            text = i18n("下載完成，已打開系統安裝流程。"),
            color = colors.textDark.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        is AppUpdateDownloadState.PermissionRequired -> Text(
            text = i18n("需要允許此 app 安裝未知來源應用，完成授權後請重新點擊下載。"),
            color = colors.textDark.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        is AppUpdateDownloadState.Failed -> Text(
            text = i18n("下載失敗：{}", state.message),
            color = colors.textDark.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AppUpdateLaunchThresholdCard(
    selected: AppUpdateLaunchCheckThreshold,
    onClick: () -> Unit,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = i18n("啟動檢查更新間隔"),
                    color = colors.textDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = i18n("啟動 app 後在背景檢查更新；未超過間隔不會發出網路請求。"),
                    color = colors.textDark.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            Box(
                modifier = Modifier
                    .background(
                        color = colors.brownLight.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = appUpdateLaunchThresholdLabel(selected),
                    color = colors.brownDeep,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun AppUpdateLaunchThresholdDialog(
    selected: AppUpdateLaunchCheckThreshold,
    onSelect: (AppUpdateLaunchCheckThreshold) -> Unit,
    onDismiss: () -> Unit,
) {
    YamiboSingleSelectDialog(
        title = i18n("啟動檢查更新間隔"),
        options = AppUpdateLaunchCheckThreshold.entries,
        selected = selected,
        onDismiss = onDismiss,
        onSelect = onSelect,
        label = { appUpdateLaunchThresholdLabel(it) },
        dismissOnSelect = true,
    )
}

@Composable
private fun appUpdateLaunchThresholdLabel(option: AppUpdateLaunchCheckThreshold): String = when (option) {
    AppUpdateLaunchCheckThreshold.MANUAL -> i18n("不自動檢查")
    AppUpdateLaunchCheckThreshold.HOURS_6 -> i18n("6 小時")
    AppUpdateLaunchCheckThreshold.HOURS_12 -> i18n("12 小時")
    AppUpdateLaunchCheckThreshold.HOURS_24 -> i18n("24 小時")
    AppUpdateLaunchCheckThreshold.DAYS_3 -> i18n("3 天")
    AppUpdateLaunchCheckThreshold.DAYS_7 -> i18n("7 天")
}

private fun AppUpdateRelease.displayVersionLabel(): String = "$channel-v$versionName"

private fun AppUpdateCheckResult.Preparing.displayVersionLabel(): String = "$channel-v$versionName"
