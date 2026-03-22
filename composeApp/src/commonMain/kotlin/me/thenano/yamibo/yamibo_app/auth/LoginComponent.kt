package me.thenano.yamibo.yamibo_app.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.page.ProfilePage
import coil3.compose.SubcomposeAsyncImage
import me.thenano.yamibo.yamibo_app.util.rememberImageRequest
import io.github.littlesurvival.YamiboRoute
import kotlinx.coroutines.launch
import me.thenano.yamibo.yamibo_app.webview.WebViewTopBar
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.Navigatable
import me.thenano.yamibo.yamibo_app.store.auth.UserStore
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

class ILoginScreen : Navigatable {
    override val id: String = buildId(Any().hashCode())

    @Composable
    override fun Content() {
        LoginScreen()
    }
}

@Composable
expect fun LoginWebView(onLoadingChanged: (Boolean) -> Unit = {})

@Composable
@Preview
fun LoginScreen() {
    val navigator = LocalNavigator.current
    var loading by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        WebViewTopBar(
            title = "登入頁面",
            url = YamiboRoute.Login.build(),
            onCloseClick = { navigator.pop() },
            useBackIcon = true
        )
        Box(modifier = Modifier.weight(1f)) {
            LoginWebView(onLoadingChanged = { loading = it })
            LoadingOverlay(
                visible = loading,
            )
        }
    }
}

@Composable
@Preview
fun UserProfileCard(
    userInfo: ProfilePage? = UserStore.Preview,
    isLoading: Boolean = false,
    onRefresh: suspend () -> Unit = {},
    onLogout: suspend () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = YamiboTheme.colors
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = colors.creamSurface,
                contentColor = colors.textDark
            ),
        border = BorderStroke(1.dp, colors.brownPrimary.copy(alpha = 0.1f))
    ) {
        Box {
            AnimatedContent(
                targetState = userInfo,
                transitionSpec = {
                    (fadeIn(tween(400)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                        .togetherWith(fadeOut(tween(200)) + scaleOut(targetScale = 1.05f))
                },
                label = "user_profile_transition"
            ) { state ->
                if (state == null) {
                    LoginContent()
                } else {
                    UserInfoContent(state, { onRefresh() }, { onLogout() })
                }
            }
            /**
             * IMPORTANT: Use fully-qualified androidx.compose.animation.AnimatedVisibility.
             *
             * If import is auto-optimized or replaced with implicit receiver version, Kotlin may
             * resolve to a LayoutScope extension overload and fail with: "cannot be called in this
             * context with an implicit receiver".
             *
             * @suppress Do NOT clean up this path.
             */
            @Suppress("RemoveRedundantQualifierName")
            androidx.compose.animation.AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(colors.creamSurface.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.orangeAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

/** Not Logged In */
@Composable
private fun LoginContent() {
    val navigator = LocalNavigator.current
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        AnimatedYamiboButton(
            text = "登入",
            onClick = { navigator.navigate(ILoginScreen()) },
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

/** Logged In */
@Composable
private fun UserInfoContent(
    user: ProfilePage,
    onRefresh: suspend () -> Unit,
    onLogout: suspend () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        /* top actions */
        /* top actions */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedYamiboChip(
                label = "刷新",
                onClick = { onRefresh() },
                containerColor = YamiboTheme.colors.brownPrimary.copy(alpha = 0.1f),
                contentColor = YamiboTheme.colors.brownDeep
            )
            Spacer(Modifier.width(12.dp))
            AnimatedYamiboChip(
                label = "登出",
                onClick = { onLogout() },
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            /* avatar placeholder */
            AvatarPlaceholder(avatarUrl = user.avatarUrl ?: "")

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "UID: ${user.uid.value}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = YamiboTheme.colors.brownPrimary
                )

                Spacer(Modifier.height(8.dp))

                Row {
                    InfoChip(user.userGroup)
                    Spacer(Modifier.width(8.dp))
                    InfoChip("總積分 ${user.totalPoints}")
                }
            }
        }
    }
}

/** Avatar */
@Composable
private fun AvatarPlaceholder(
    avatarUrl: String,
) {
    SubcomposeAsyncImage(
        model = rememberImageRequest(url = avatarUrl),
        contentDescription = null,
        modifier = Modifier.size(72.dp).clip(CircleShape),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        error = {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    )
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = YamiboTheme.colors.orangeAccent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, YamiboTheme.colors.orangeAccent.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = YamiboTheme.colors.brownDeep,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AnimatedYamiboButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by
    animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    val backgroundColor = YamiboTheme.colors.brownDeep
    val contentColor = Color.White

    Surface(
        onClick = onClick,
        modifier =
            modifier.scale(scale).graphicsLayer {
                shadowElevation = if (isHovered) 8.dp.toPx() else 4.dp.toPx()
                shape = RoundedCornerShape(50)
                clip = true
            },
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun AnimatedYamiboChip(
    label: String,
    onClick: suspend () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val scale by
    animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (isHovered) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier =
            Modifier.scale(scale).clickable(
                interactionSource = interactionSource,
                indication = null
            ) { scope.launch { onClick() } }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
