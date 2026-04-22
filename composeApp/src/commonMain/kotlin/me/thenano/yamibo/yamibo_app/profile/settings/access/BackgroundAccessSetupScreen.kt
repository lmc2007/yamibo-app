package me.thenano.yamibo.yamibo_app.profile.settings.access

import YamiboIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.LocalBackgroundAccessRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackgroundAccessSetupScreen() {
    val colors = YamiboTheme.colors
    val navigator = LocalNavigator.current
    val repository = LocalBackgroundAccessRepository.current
    val state by repository.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val notificationPermissionRequester = rememberBackgroundAccessNotificationPermissionRequester {
        coroutineScope.launch {
            repository.refresh()
        }
    }

    LaunchedEffect(Unit) {
        repository.refresh()
    }
    BackgroundAccessResumeRefreshEffect {
        coroutineScope.launch {
            repository.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "通知與背景同步",
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = state.summary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textDark,
            )
            if (state.platformNote != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.platformNote.orEmpty(),
                    fontSize = 13.sp,
                    color = colors.textDark.copy(alpha = 0.7f),
                    lineHeight = 20.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { coroutineScope.launch { repository.refresh() } },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brownPrimary,
                    contentColor = Color.White,
                ),
            ) {
                Text("重新檢查")
            }

            Spacer(Modifier.height(24.dp))

            state.items.forEachIndexed { index, item ->
                BackgroundAccessItemCard(
                    item = item,
                    onAction = { action ->
                        when (action) {
                            BackgroundAccessRepository.SetupAction.RequestNotificationPermission -> {
                                notificationPermissionRequester?.invoke()
                            }
                            else -> repository.runAction(action)
                        }
                    },
                )
                if (index != state.items.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun BackgroundAccessItemCard(
    item: BackgroundAccessRepository.SetupItem,
    onAction: (BackgroundAccessRepository.SetupAction) -> Unit,
) {
    val colors = YamiboTheme.colors
    val (statusText, statusColor) = when (item.status) {
        BackgroundAccessRepository.SetupStatus.Granted -> "已就緒" to colors.brownPrimary
        BackgroundAccessRepository.SetupStatus.Required -> "必須處理" to Color(0xFFB4573B)
        BackgroundAccessRepository.SetupStatus.Recommended -> "建議處理" to Color(0xFF8A6A2C)
        BackgroundAccessRepository.SetupStatus.Info -> "說明" to colors.textDark.copy(alpha = 0.55f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.creamSurface, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textDark,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                )
            }
        }

        Text(
            text = item.subtitle,
            fontSize = 13.sp,
            color = colors.textDark.copy(alpha = 0.72f),
            lineHeight = 20.sp,
        )

        if (item.action != null && item.actionLabel != null) {
            Button(
                onClick = { onAction(item.action) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.brownDeep,
                    contentColor = Color.White,
                ),
            ) {
                Text(item.actionLabel)
            }
        }
    }
}
