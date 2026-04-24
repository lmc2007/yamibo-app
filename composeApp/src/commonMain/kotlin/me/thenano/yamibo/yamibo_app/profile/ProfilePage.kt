package me.thenano.yamibo.yamibo_app.profile

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.core.YamiboResult
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.LocalAuthRepository
import me.thenano.yamibo.yamibo_app.LocalSignRepository
import me.thenano.yamibo.yamibo_app.event.AppEventBus
import me.thenano.yamibo.yamibo_app.event.events.LoginSuccessEvent
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.ISettingsScreen
import me.thenano.yamibo.yamibo_app.profile.sign.ISignInfoScreen
import me.thenano.yamibo.yamibo_app.profile.sign.ISignWebView
import me.thenano.yamibo.yamibo_app.repository.settings.SignInMode
import me.thenano.yamibo.yamibo_app.theme.YamiboSnackbarHost
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme.colors

@Composable
fun ProfilePage() {
    val authRepository = LocalAuthRepository.current
    val signRepository = LocalSignRepository.current
    val appSettingsRepository = LocalAppSettingsRepository.current
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val colors = colors
    val snackbarHostState = remember { SnackbarHostState() }

    var userInfo by remember { mutableStateOf(authRepository.currentUser()) }
    var isLoading by remember { mutableStateOf(false) }
    var signButtonTitle by remember { mutableStateOf("每日簽到") }
    var signRefreshKey by remember { mutableIntStateOf(0) }
    var isSigning by remember { mutableStateOf(false) }

    fun refreshSignStatus() {
        coroutineScope.launch {
            if (isSigning) {
                signButtonTitle = "正在簽到..."
                return@launch
            }
            signButtonTitle = when {
                userInfo == null -> "每日簽到"
                signRepository.getCachedPageInfo()?.hasSignedToday == true -> "今日已簽到"
                signRepository.getCachedPageInfo()?.hasSignedToday == false -> "每日簽到"
                signRepository.isSignedToday() -> "今日已簽到"
                else -> {
                    when (val result = signRepository.fetchPageInfo()) {
                        is YamiboResult.Success -> {
                            if (result.value.hasSignedToday) "今日已簽到" else "每日簽到"
                        }
                        is YamiboResult.Failure -> {
                            when {
                                signRepository.getCachedPageInfo()?.hasSignedToday == true -> "今日已簽到"
                                else -> "每日簽到"
                            }
                        }
                        else -> "每日簽到"
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        AppEventBus.events.collect { event ->
            if (event == LoginSuccessEvent) {
                userInfo = authRepository.currentUser()
                signRefreshKey += 1
            }
        }
    }

    LaunchedEffect(userInfo, signRefreshKey) {
        refreshSignStatus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.creamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            UserProfileCard(
                userInfo = userInfo,
                isLoading = isLoading,
                onRefresh = {
                    coroutineScope.launch {
                        isLoading = true
                        authRepository.fetchStatus()
                        userInfo = authRepository.currentUser()
                        signRefreshKey += 1
                        isLoading = false
                    }
                },
                onLogout = {
                    coroutineScope.launch {
                        isLoading = true
                        authRepository.logOut()
                        userInfo = authRepository.currentUser()
                        signRefreshKey += 1
                        isLoading = false
                    }
                },
                modifier = Modifier.padding(top = 12.dp)
            )
            Spacer(Modifier.height(8.dp))

            SignEntryCard(
                title = signButtonTitle,
                onSignClick = {
                    if (userInfo == null) {
                        return@SignEntryCard
                    }
                    when (appSettingsRepository.signInMode.getValue()) {
                        SignInMode.FULL_MANUAL -> {
                            navigator.navigate(
                                ISignWebView(
                                    semiAutomatic = false,
                                    onResultObserved = {
                                        coroutineScope.launch {
                                            isSigning = true
                                            signButtonTitle = "正在簽到..."
                                            authRepository.syncCookieFromWebView()
                                            when (signRepository.fetchPageInfo()) {
                                                is YamiboResult.Success -> {
                                                    signRefreshKey += 1
                                                    refreshSignStatus()
                                                }

                                                else -> Unit
                                            }
                                            isSigning = false
                                            refreshSignStatus()
                                        }
                                    }
                                )
                            )
                        }

                        SignInMode.SEMI_AUTOMATIC -> {
                            isSigning = true
                            signButtonTitle = "正在簽到..."
                            navigator.navigate(
                                ISignWebView(
                                    semiAutomatic = true,
                                    allowRepair = appSettingsRepository.signInAllowRepair.getValue(),
                                    onSemiAutoCompleted = { result ->
                                        coroutineScope.launch {
                                            var snackbarMessage: String?
                                            /** This when maps semi-automatic sign results into the ProfilePage sign button snackbar/title update flow. */
                                            when (result) {
                                                is YamiboResult.Success -> {
                                                    signRefreshKey += 1
                                                    snackbarMessage = result.value.message
                                                }

                                                is YamiboResult.Failure -> {
                                                    snackbarMessage = result.message()
                                                }

                                                is YamiboResult.NotLoggedIn -> {
                                                    snackbarMessage = result.message()
                                                }

                                                is YamiboResult.NoPermission -> {
                                                    snackbarMessage = "目前無法自動簽到，請改用手動模式"
                                                }

                                                is YamiboResult.Maintenance -> {
                                                    snackbarMessage = result.message()
                                                }
                                            }
                                            isSigning = false
                                            refreshSignStatus()
                                            snackbarMessage.let { message ->
                                                coroutineScope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                            }
                                        }
                                    },
                                    onMaintenanceObserved = {
                                        coroutineScope.launch {
                                            isSigning = false
                                            refreshSignStatus()
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar("百合會維護中...現在不是簽到的好時機呢")
                                        }
                                    },
                                )
                            )
                        }
                    }
                },
                onInfoClick = {
                    navigator.navigate(
                        ISignInfoScreen(
                            onInfoLoaded = {
                                signRefreshKey += 1
                            }
                        )
                    )
                },
            )

            EntryCard(
                title = "設定",
                icon = YamiboIcons.Setting,
                onClick = { navigator.navigate(ISettingsScreen()) }
            )

            Spacer(Modifier.height(24.dp))
        }

        YamiboSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun SignEntryCard(
    title: String,
    onSignClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(0.85f)
                    .clickable(onClick = onSignClick)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = YamiboIcons.EditOrSign,
                    contentDescription = "每日簽到",
                    tint = colors.brownPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textDark
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.15f)
                    .fillMaxHeight()
                    .clickable(onClick = onInfoClick)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                VerticalDivider(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .height(24.dp)
                        .padding(start = 10.dp),
                    color = colors.brownLight.copy(alpha = 0.35f),
                    thickness = 1.dp,
                )
                Text(
                    text = "!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.brownPrimary,
                )
            }
        }
    }
}

@Composable
private fun EntryCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = colors.creamSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.brownPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark
            )
        }
    }
}
