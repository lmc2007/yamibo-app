package me.thenano.yamibo.yamibo_app

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory

import me.thenano.yamibo.yamibo_app.profile.sign.ISignWebView
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.util.SignReminderTrigger
import androidx.compose.material3.SnackbarHostState
import me.thenano.yamibo.yamibo_app.components.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import io.github.littlesurvival.core.YamiboResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.home.HomePageScreen
import me.thenano.yamibo.yamibo_app.i18n.AppLocaleProvider
import me.thenano.yamibo.yamibo_app.i18n.i18n
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.NavAction
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateCheckResult
import me.thenano.yamibo.yamibo_app.repository.appupdate.AppUpdateRelease
import me.thenano.yamibo.yamibo_app.repository.appupdate.changelogContent
import me.thenano.yamibo.yamibo_app.repository.appupdate.fullVersionName
import me.thenano.yamibo.yamibo_app.repository.chineseconversion.ChineseConversionMode
import me.thenano.yamibo.yamibo_app.repository.settings.ReaderChineseConversionOption
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme
import me.thenano.yamibo.yamibo_app.components.font.getFontFamily
import me.thenano.yamibo.yamibo_app.util.state
import me.thenano.yamibo.yamibo_app.util.time.currentLocalDateKey
import me.thenano.yamibo.yamibo_app.util.time.currentTimeMillis
import org.jetbrains.compose.resources.painterResource
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.logo_about

@Composable
fun HomeScreenContent(
    onNewMessageStatusChange: (Boolean) -> Unit = {},
) {
    HomePageScreen(onNewMessageStatusChange = onNewMessageStatusChange)
}

@Composable
fun App() {
    val imageLoaderFactory = remember {
        { context: PlatformContext ->
            ImageLoader.Builder(context)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(context, 0.35)
                        .build()
                }
                .components {
                    add(KtorNetworkFetcherFactory())
                }
                .build()
            }
    }
    setSingletonImageLoaderFactory(imageLoaderFactory)

    val navigator = LocalNavigator.current
    val appSettingsRepository = LocalAppSettingsRepository.current
    val authRepository = LocalAuthRepository.current
    val signRepository = LocalSignRepository.current
    val appUpdateRepository = LocalAppUpdateRepository.current
    val fontRepository = LocalFontRepository.current
    val appLanguage = appSettingsRepository.language.state()
    val appFontId = appSettingsRepository.appFontId.state()
    val appFontFamily = remember(appFontId) { fontRepository.getFontFamily(appFontId) }
    val signLaunchReminderEnabled = appSettingsRepository.signInLaunchReminderEnabled.state()
    val holder = rememberSaveableStateHolder()
    val snackbarHostState = remember { SnackbarHostState() }
    navigator.stateHolder = holder
    ChineseConversionModeSync()

    val stack = navigator.stack
    val poppingIdx by navigator.poppingIndex
    val duration = 250
    var completedPushTopId by remember { mutableStateOf(stack.lastOrNull()?.id) }
    var showSignReminder by remember { mutableStateOf(false) }
    var launchUpdateRelease by remember { mutableStateOf<AppUpdateRelease?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val threshold = appSettingsRepository.appUpdateLaunchCheckThreshold.getValue()
        val intervalMillis = threshold.hours?.times(60L * 60L * 1000L) ?: return@LaunchedEffect
        val now = currentTimeMillis()
        val lastCheckAt = appSettingsRepository.appUpdateLastCheckAt.getValue().toLongOrNull() ?: 0L
        if (now - lastCheckAt >= intervalMillis) {
            val result = appUpdateRepository.checkForUpdate(force = false)
            if (result is AppUpdateCheckResult.UpdateAvailable) {
                launchUpdateRelease = result.release
            }
        }
    }

    AppLocaleProvider(appLanguage) {
        val rootTextStyle = LocalTextStyle.current
        CompositionLocalProvider(
            LocalTextStyle provides rootTextStyle.copy(fontFamily = appFontFamily),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = YamiboTheme.colors.creamBackground
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val topIndex = stack.lastIndex
                val topId = stack.lastOrNull()?.id
                val renderPreviousForPush =
                    navigator.lastAction == NavAction.Push &&
                        topIndex > 0 &&
                        completedPushTopId != topId
                stack.forEachIndexed { index, navigatable ->
                    val isPopping = index == poppingIdx
                    val isTop = index == stack.lastIndex
                    val isNewPush =
                        navigator.lastAction == NavAction.Push &&
                            isTop &&
                            !isPopping &&
                            topIndex > 0 &&
                            completedPushTopId != navigatable.id
                    val shouldDraw =
                        isTop ||
                            isPopping ||
                            (poppingIdx >= 0 && index == poppingIdx - 1) ||
                            (renderPreviousForPush && index == topIndex - 1)

                    key(navigatable.id) {
                        // New push screens start invisible (false→true), others start visible
                        val visibleState = remember {
                            MutableTransitionState(!isNewPush)
                        }

                        // Drive animation: pop = true→false, otherwise stay/become true
                        if (isPopping) {
                            visibleState.targetState = false
                        } else {
                            visibleState.targetState = true
                        }

                        holder.SaveableStateProvider(navigatable.id) {
                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(duration)
                                ) + fadeIn(animationSpec = tween(duration)),
                                exit = slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(duration)
                                ) + fadeOut(animationSpec = tween(duration)),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .drawOnlyWhen(shouldDraw)
                                    .blockPointerPassthrough(isTop || isPopping)
                                    .zIndex(index.toFloat())
                            ) {
                                navigatable.Content()
                            }
                        }

                        // When exit animation finished, actually remove from stack
                        if (isPopping && visibleState.isIdle && !visibleState.currentState) {
                            LaunchedEffect(Unit) {
                                navigator.completePop()
                            }
                        }
                        if (isNewPush && visibleState.isIdle && visibleState.currentState && completedPushTopId != navigatable.id) {
                            LaunchedEffect(navigatable.id) {
                                completedPushTopId = navigatable.id
                            }
                        }
                    }
                }
            }
            YamiboSnackbarHost(snackbarHostState)
            LaunchSignReminderDialog(
                visible = showSignReminder,
                onDismiss = {
                    appSettingsRepository.signInLaunchReminderDismissedDate.setValue(currentLocalDateKey())
                    showSignReminder = false
                },
                onGoSign = {
                    showSignReminder = false
                    navigateToSignWebViewOrProfile(
                        navigator = navigator,
                        appSettingsRepository = appSettingsRepository,
                        authRepository = authRepository,
                        signRepository = signRepository,
                        coroutineScope = coroutineScope,
                        snackbarHostState = snackbarHostState,
                    )
                },
            )
            LaunchUpdateAvailableDialog(
                release = launchUpdateRelease,
                onDismiss = { launchUpdateRelease = null },
                onDownload = { release ->
                    launchUpdateRelease = null
                    coroutineScope.launch {
                        appUpdateRepository.downloadAndInstall(release)
                    }
                },
                onOpenReleasePage = { release ->
                    launchUpdateRelease = null
                    appUpdateRepository.openReleasePage(release)
                },
            )
            }
        }
    }

    LaunchedEffect(signLaunchReminderEnabled, appLanguage) {
        if (!signLaunchReminderEnabled) {
            showSignReminder = false
            return@LaunchedEffect
        }
        val today = currentLocalDateKey()
        if (appSettingsRepository.signInLaunchReminderDismissedDate.getValue() == today) return@LaunchedEffect
        if (authRepository.currentUser() == null) return@LaunchedEffect
        if (!signRepository.isSignedToday()) {
            showSignReminder = true
        }
    }

    LaunchedEffect(SignReminderTrigger.showSignWebViewTrigger.value) {
        if (SignReminderTrigger.showSignWebViewTrigger.value) {
            SignReminderTrigger.showSignWebViewTrigger.value = false
            navigateToSignWebViewOrProfile(
                navigator = navigator,
                appSettingsRepository = appSettingsRepository,
                authRepository = authRepository,
                signRepository = signRepository,
                coroutineScope = coroutineScope,
                snackbarHostState = snackbarHostState,
            )
        }
    }
}

private fun Modifier.blockPointerPassthrough(enabled: Boolean): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent()
                }
            }
        }
    }

private fun Modifier.drawOnlyWhen(shouldDraw: Boolean): Modifier =
    drawWithContent {
        if (shouldDraw) drawContent()
    }

@Composable
private fun LaunchUpdateAvailableDialog(
    release: AppUpdateRelease?,
    onDismiss: () -> Unit,
    onDownload: (AppUpdateRelease) -> Unit,
    onOpenReleasePage: (AppUpdateRelease) -> Unit,
) {
    if (release == null) return
    Dialog(onDismissRequest = onDismiss) {
        LaunchUpdateAvailableContent(
            release = release,
            onDismiss = onDismiss,
            onDownload = onDownload,
            onOpenReleasePage = onOpenReleasePage,
        )
    }
}

@Composable
private fun LaunchUpdateAvailableContent(
    release: AppUpdateRelease,
    onDismiss: () -> Unit,
    onDownload: (AppUpdateRelease) -> Unit,
    onOpenReleasePage: (AppUpdateRelease) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.creamSurface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = i18n("發現新版本"),
                color = colors.textStrong,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = release.fullVersionName(),
                color = colors.textDark.copy(alpha = 0.72f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Image(
                painter = painterResource(Res.drawable.logo_about),
                contentDescription = null,
                modifier = Modifier
                    .width(270.dp)
                    .height(76.dp),
                contentScale = ContentScale.Fit,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = release.changelogContent().ifBlank {
                        i18n("新版已可下載。你可以立即下載更新，或前往發布頁手動更新。")
                    },
                    color = colors.textDark.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Button(
                onClick = {
                    if (release.asset == null) {
                        onOpenReleasePage(release)
                    } else {
                        onDownload(release)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brownDeep,
                    contentColor = colors.textOnDeep,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = i18n("立即更新"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onOpenReleasePage(release) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.creamBackground,
                        contentColor = colors.textStrong,
                    ),
                    border = BorderStroke(1.dp, colors.brownLight.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = i18n("手動更新"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.creamBackground,
                        contentColor = colors.textDark,
                    ),
                    border = BorderStroke(1.dp, colors.brownLight.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = i18n("稍後"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LaunchSignReminderDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onGoSign: () -> Unit,
) {
    if (!visible) return
    val colors = YamiboTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.creamSurface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.creamSurface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = i18n("你今天還沒簽到"),
                    color = colors.textStrong,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = i18n("今天尚未完成每日簽到。"),
                    color = colors.textDark.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textStrong,
                        ),
                    ) {
                        Text(i18n("取消"))
                    }
                    Button(
                        onClick = onGoSign,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brownDeep,
                            contentColor = colors.textOnDeep,
                        ),
                    ) {
                        Text(i18n("前往簽到"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChineseConversionModeSync() {
    val conversionRepository = LocalChineseConversionRepository.current
    val novelSettingsRepository = LocalNovelReaderSettingsRepository.current
    val option = novelSettingsRepository.chineseConversion.state()

    LaunchedEffect(option) {
        conversionRepository.setConversionMode(
            when (option) {
                ReaderChineseConversionOption.DEFAULT -> null
                ReaderChineseConversionOption.SIMPLIFIED -> ChineseConversionMode.Simplified
                ReaderChineseConversionOption.TRADITIONAL -> ChineseConversionMode.Traditional
            }
        )
    }
}

private fun navigateToSignWebViewOrProfile(
    navigator: ComposableNavigator,
    appSettingsRepository: AppSettingsRepository,
    authRepository: AuthRepository,
    signRepository: SignRepository,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    if (authRepository.currentUser() == null) return
    val isDirect = appSettingsRepository.signInDirectWebView.getValue()
    if (isDirect) {
        val mode = appSettingsRepository.signInMode.getValue()
        val allowRepair = appSettingsRepository.signInAllowRepair.getValue()
        when (mode) {
            SignInMode.FULL_MANUAL -> {
                navigator.navigate(
                    ISignWebView(
                        semiAutomatic = false,
                        onResultObserved = {
                            coroutineScope.launch {
                                authRepository.syncCookieFromWebView()
                                signRepository.markTodaySigned()
                                signRepository.fetchPageInfo()
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(i18n("簽到成功"))
                            }
                        },
                        onLoadFailed = { reason ->
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(i18n("簽到頁載入失敗：{}", reason))
                            }
                        }
                    )
                )
            }
            SignInMode.SEMI_AUTOMATIC -> {
                navigator.navigate(
                    ISignWebView(
                        semiAutomatic = true,
                        onCfCleared = {
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(i18n("開始自動簽到..."))
                                when (val result = signRepository.runAutoSign(allowRepair)) {
                                    is YamiboResult.Success -> {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(result.value.message)
                                    }
                                    is YamiboResult.Failure -> {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(i18n(result.message()))
                                    }
                                    is YamiboResult.NotLoggedIn -> {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(i18n(result.message()))
                                    }
                                    is YamiboResult.NoPermission -> {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(i18n("目前無法自動簽到，請改用手動模式"))
                                    }
                                    is YamiboResult.Maintenance -> {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(i18n(result.message()))
                                    }
                                }
                            }
                        },
                        onMaintenanceObserved = {
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(i18n("百合會維護中...現在不是簽到的好時機呢"))
                            }
                        },
                        onLoadFailed = { reason ->
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar(i18n("簽到頁載入失敗：{}", reason))
                            }
                        }
                    )
                )
            }
        }
    } else {
        navigator.popToRoot()
        navigator.replace(IMainScreen(MainTab.Profile))
    }
}
